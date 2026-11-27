<template>
  <div class="play-preset">
    <span class="play-preset__label">预案:</span>
    <el-select
      v-model="currentPresetId"
      class="play-preset__select"
      size="mini"
      placeholder="选择分屏预案"
      clearable
      @change="handleChange"
    >
      <el-option
        v-for="item in presetList"
        :key="item.id"
        :label="formatLabel(item)"
        :value="item.id"
      />
    </el-select>
    <i class="el-icon-document-add play-preset__btn" title="保存当前分屏为预案" @click="handleSave" />
    <i class="el-icon-star-on play-preset__btn" title="设为默认预案" @click="handleSetDefault" />
    <i class="el-icon-delete play-preset__btn" title="删除预案" @click="handleDelete" />
  </div>
</template>

<script>
export default {
  name: 'PlayPresetBar',
  props: {
    // 当前分屏数， 保存预案时一并提交
    splitNum: {
      type: Number,
      required: true
    },
    // 当前每格正在播放的通道ID， 索引对应分屏序号
    channelIds: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      presetList: [], // 当前用户的预案列表
      currentPresetId: null, // 当前选中的预案ID
      saving: false // 保存中状态， 防止重复提交
    }
  },
  created() {
    this.loadList()
  },
  methods: {
    // 加载预案列表
    loadList() {
      this.$store.dispatch('playPreset/queryList').then(data => {
        this.presetList = data || []
        // 列表加载完成后默认选中默认预案， 便于直接看到当前固定方案
        const defaultPreset = this.presetList.find(item => item.isDefault)
        if (!this.currentPresetId && defaultPreset) {
          this.currentPresetId = defaultPreset.id
        }
      }).catch(() => {
        this.presetList = []
      })
    },
    // 下拉展示：名称 + 分屏数 + 通道数
    formatLabel(preset) {
      return preset.name + '（' + preset.splitNum + '分屏/' + preset.channelCount + '路）'
    },
    // 切换预案时通知父组件按预案恢复画面
    handleChange(presetId) {
      if (!presetId) {
        return
      }
      this.$store.dispatch('playPreset/getPreset', presetId).then(preset => {
        this.$emit('apply', preset)
      }).catch(() => {
        this.currentPresetId = null
      })
    },
    // 收集当前分屏布局并保存为预案， 同名预案执行覆盖更新
    handleSave() {
      const itemList = []
      this.channelIds.forEach((channelId, index) => {
        if (channelId) {
          itemList.push({ channelId: Number(channelId), sort: index + 1 })
        }
      })
      if (itemList.length === 0) {
        this.$message.warning('当前没有正在播放的通道， 请先拉流后再保存')
        return
      }
      const selectedPreset = this.presetList.find(item => item.id === this.currentPresetId)
      this.$prompt('请输入预案名称， 同名预案将被覆盖', '保存分屏预案', {
        confirmButtonText: '保存',
        cancelButtonText: '取消',
        inputValue: selectedPreset ? selectedPreset.name : '',
        inputPattern: /^.{1,50}$/,
        inputErrorMessage: '预案名称长度为1-50个字符'
      }).then(({ value }) => {
        if (this.saving) {
          return
        }
        this.saving = true
        const name = value.trim()
        const sameNamePreset = this.presetList.find(item => item.name === name)
        const params = {
          name: name,
          splitNum: this.splitNum,
          itemList: itemList
        }
        const saveAction = sameNamePreset
          ? this.$store.dispatch('playPreset/updatePreset', { ...params, id: sameNamePreset.id })
          : this.$store.dispatch('playPreset/addPreset', params)
        saveAction.then(() => {
          this.$message.success('预案保存成功')
          this.loadList()
        }).catch(err => {
          this.$message.error(typeof err === 'string' ? err : '预案保存失败')
        }).finally(() => {
          this.saving = false
        })
      }).catch(() => {
        // 用户取消输入， 无需处理
      })
    },
    // 设置当前选中的预案为默认
    handleSetDefault() {
      if (!this.currentPresetId) {
        this.$message.warning('请先选择预案')
        return
      }
      this.$store.dispatch('playPreset/setDefaultPreset', this.currentPresetId).then(() => {
        this.$message.success('已设为默认预案， 任意终端登录后将自动恢复')
        this.loadList()
      }).catch(err => {
        this.$message.error(typeof err === 'string' ? err : '设置默认预案失败')
      })
    },
    // 删除当前选中的预案
    handleDelete() {
      if (!this.currentPresetId) {
        this.$message.warning('请先选择预案')
        return
      }
      this.$confirm('确认删除该预案？删除后无法恢复。', '删除确认', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('playPreset/deletePreset', this.currentPresetId).then(() => {
          this.$message.success('预案已删除')
          this.currentPresetId = null
          this.loadList()
        }).catch(err => {
          this.$message.error(typeof err === 'string' ? err : '删除预案失败')
        })
      }).catch(() => {
        // 用户取消删除， 无需处理
      })
    }
  }
}
</script>

<style scoped>
.play-preset {
  display: flex;
  align-items: center;
  font-size: 14px;
}

.play-preset__label {
  margin-right: 6px;
}

.play-preset__select {
  width: 190px;
}

.play-preset__btn {
  margin-left: 8px;
  cursor: pointer;
}

.play-preset__btn:hover {
  color: #409eff;
}
</style>
