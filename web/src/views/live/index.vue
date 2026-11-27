<template>
  <div id="live" class="live-container">
    <div v-loading="loading" class="live-content" :class="{ 'sidebar-collapsed': !sidebarVisible }" element-loading-text="拼命加载中">
      <div class="device-tree-container-box" :class="{ 'device-tree-hidden': !sidebarVisible }">
        <DeviceTree @clickEvent="clickEvent" :context-menu-event="contextMenuEvent" />
      </div>
      <div class="video-container">
        <div class="control-bar">
          <div class="split-controls">
            <i :class="['btn', 'sidebar-toggle', sidebarVisible ? 'el-icon-s-fold' : 'el-icon-s-unfold']" title="切换侧边栏" @click="toggleSidebar" />
            <span class="divider" />
            分屏:
            <i class="iconfont icon-a-mti-1fenpingshi btn" :class="{active:spiltIndex === 0}" @click="spiltIndex=0" />
            <i class="iconfont icon-a-mti-4fenpingshi btn" :class="{active: spiltIndex === 1}" @click="spiltIndex=1" />
            <i class="iconfont icon-a-mti-6fenpingshi btn" :class="{active: spiltIndex === 2}" @click="spiltIndex=2" />
            <i class="iconfont icon-a-mti-9fenpingshi btn" :class="{active: spiltIndex === 3}" @click="spiltIndex=3" />
          </div>
          <div class="preset-controls">
            <PlayPresetBar
              :split-num="layout[spiltIndex].spilt"
              :channel-ids="channelIds"
              @apply="applyPreset"
            />
          </div>
          <div class="global-player-control">
            播放器:
            <el-select v-model="globalPlayer" size="mini" style="width: 120px">
              <el-option label="Jessibuca" value="jessibuca" />
              <el-option label="WebRTC" value="webRTC" />
              <el-option label="H265web" value="h265web" />
            </el-select>
          </div>
          <div class="fullscreen-control">
            <i class="el-icon-full-screen btn" @click="fullScreen()" />
            <i class="iconfont icon-PTZ btn" title="云台控制" @click="togglePtzPanel" />
          </div>
        </div>
        <div class="player-container">
          <div
            ref="playBox"
            class="play-grid"
            :style="liveStyle"
          >
            <div
              v-for="i in layout[spiltIndex].spilt"
              :key="i"
              class="play-box"
              :class="getPlayerClass(spiltIndex, i)"
              @click="playerIdx = (i-1)"
            >
              <div v-if="!streamInfo[i-1]" class="no-signal">{{ videoTip[i-1]?videoTip[i-1]:"无信号" }}</div>
              <PlayerTabs
                v-else
                :ref="'playerTabs' + i"
                :show-tab="false"
                :show-button="true"
              />
            </div>
          </div>
        </div>
      </div>
      <div class="ptz-panel" v-show="ptzVisible">
        <div class="ptz-panel-header">
          <span>云台控制</span>
          <i class="el-icon-close" @click="ptzVisible = false" />
        </div>
        <div class="ptz-panel-body">
          <template v-if="currentChannelId">
            <div class="ptz-preset-section">
              <div class="section-title">预置位</div>
              <LivePtzPreset :channel-id="currentChannelId" />
            </div>
            <div class="ptz-control-section">
              <div class="section-title">方向控制</div>
              <channelPtzPanel :channel-id="currentChannelId" @drag-zoom-start="handleDragZoom" />
            </div>
          </template>
          <div v-else class="ptz-empty-tip">请先在左侧选择通道</div>
        </div>
      </div>
    </div>
  </div>
</template>
<script>

import PlayerTabs from '../common/playerTabs.vue'
import DeviceTree from '../common/DeviceTree.vue'
import channelPtzPanel from '../channel/common/channelPtzPanel.vue'
import LivePtzPreset from './LivePtzPreset.vue'
import PlayPresetBar from './PlayPresetBar.vue'
import screenFull from 'screenfull'

export default {
  name: 'Live',
  components: {
    PlayerTabs, DeviceTree, channelPtzPanel, LivePtzPreset, PlayPresetBar
  },

  data() {
    return {
      streamInfo: [], // 每格播放的流信息， 索引对应分屏序号
      videoTip: [], // 每格提示文案， 如拉流中、播放失败
      channelIds: [], // 每格正在播放的通道ID， 保存预案时使用
      globalPlayer: 'jessibuca',
      sidebarVisible: true, // 侧边栏
      ptzVisible: false, // 云台面板
      currentChannelId: null, // 当前选中通道
      spiltIndex: 2, // 分屏
      playerIdx: 0, // 激活播放器

      updateLooper: 0, // 数据刷新轮训标志
      count: 15,
      total: 0,

      // channel
      loading: false,
      layout: [
        {
          spilt: 1,
          columns: '1fr',
          rows: '1fr',
          style: function() {}
        },
        {
          spilt: 4,
          columns: '1fr 1fr',
          rows: '1fr 1fr',
          style: function() {}
        },
        {
          spilt: 6,
          columns: '1fr 1fr 1fr',
          rows: '1fr 1fr 1fr',
          style: function(index) {
            console.log(index)
            if (index === 0) {
              return {
                gridColumn: ' 1 / span 2',
                gridRow: ' 1 / span 2'
              }
            }
          }

        },
        {
          spilt: 9,
          columns: '1fr 1fr 1fr',
          rows: '1fr 1fr 1fr',
          style: function() {}
        }
      ]
    }
  },

  computed: {
    liveStyle() {
      return {
        display: 'grid',
        gridTemplateColumns: this.layout[this.spiltIndex].columns,
        gridTemplateRows: this.layout[this.spiltIndex].rows,
        gap: '4px',
        backgroundColor: '#a9a8a8'
      }
    }
  },
  watch: {
    spiltIndex(newValue) {
      console.log('切换画幅;' + newValue)
      // 分屏数量变化时同步每格数据长度， 避免映射错位
      this.resizeGrid(this.layout[newValue].spilt)
      const that = this
      for (let i = 1; i <= this.layout[newValue].spilt; i++) {
        if (!that.$refs['playerTabs' + i]) {
          continue
        }
        this.$nextTick(() => {
          const ref = that.$refs['playerTabs' + i]
          const instance = ref instanceof Array ? ref[0] : ref
          if (instance && instance.resize) {
            instance.resize()
          }
        })
      }
      window.localStorage.setItem('split', newValue)
    },
    globalPlayer(newKey) {
      for (let i = 1; i <= this.layout[this.spiltIndex].spilt; i++) {
        const ref = this.$refs['playerTabs' + i]
        if (ref) {
          const instance = ref instanceof Array ? ref[0] : ref
          instance.switchPlayer(newKey)
        }
      }
      window.localStorage.setItem('globalPlayer', newKey)
    },
    '$route.fullPath': 'checkPlayByParam'
  },
  mounted() {
    // Add window resize event listener to handle responsive behavior
    window.addEventListener('resize', this.handleResize)
    this.handleResize()
  },
  created() {
    // 先恢复本地的分屏数和播放器偏好
    this.restorePreference()
    this.resizeGrid(this.layout[this.spiltIndex].spilt)
    if (this.$route.query.channelId) {
      // URL 指定了通道时优先播放该通道
      this.checkPlayByParam()
    } else {
      // 优先按服务端保存的默认预案恢复固定的分屏画面
      this.loadDefaultPreset()
    }
  },
  destroyed() {
    clearTimeout(this.updateLooper)
    // Remove event listener when component is destroyed
    window.removeEventListener('resize', this.handleResize)
  },
  methods: {
    toggleSidebar() {
      this.sidebarVisible = !this.sidebarVisible
      if (this.sidebarVisible) {
        this.ptzVisible = false
      }
    },
    handleDragZoom(direction) {
      const refName = 'playerTabs' + (this.playerIdx + 1)
      const ref = this.$refs[refName]
      if (!ref) return
      const instance = ref instanceof Array ? ref[0] : ref
      if (!instance || !instance.startDragZoom) return
      console.log('[live] handleDragZoom playerTabs:', refName, 'playerIdx:', this.playerIdx, 'direction:', direction)
      instance.startDragZoom((params) => {
        console.log('[live] dragZoom before channelId:', JSON.stringify(params))
        params.channelId = this.currentChannelId
        console.log('[live] dragZoom after channelId:', JSON.stringify(params))
        const action = direction === 'in' ? 'commonChanel/dragZoomIn' : 'commonChanel/dragZoomOut'
        const successMsg = direction === 'in' ? '拉框放大成功' : '拉框缩小成功'
        const failMsg = direction === 'in' ? '拉框放大失败' : '拉框缩小失败'
        this.$store.dispatch(action, params).then(() => {
          this.$message({ showClose: true, message: successMsg, type: 'success' })
        }).catch(() => {
          this.$message({ showClose: true, message: failMsg, type: 'error' })
        })
      })
    },
    togglePtzPanel() {
      this.ptzVisible = !this.ptzVisible
      if (this.ptzVisible) {
        this.sidebarVisible = false
      }
    },
    handleResize() {
      this.$forceUpdate()

      this.$nextTick(() => {
        for (let i = 0; i < this.layout[this.spiltIndex].spilt; i++) {
          const ref = this.$refs[`playerTabs${i + 1}`]
          if (ref) {
            const instance = ref instanceof Array ? ref[0] : ref
            instance.resize && instance.resize()
          }
        }
      })
    },
    clickEvent: function(channelId) {
      this.currentChannelId = channelId
      this.playAt(channelId, this.playerIdx)
    },
    getPlayerClass: function(splitIndex, i) {
      let classStr = 'play-box-' + splitIndex + '-' + i
      if (this.playerIdx === (i - 1)) {
        classStr += ' redborder'
      }
      return classStr
    },
    contextMenuEvent: function(device, event, data, isCatalog) {

    },
    // 在指定的分屏格子拉流播放
    playAt: async function(channelId, idx) {
      if (idx < 0 || idx >= this.layout[this.spiltIndex].spilt) {
        return
      }
      this.$set(this.streamInfo, idx, null)
      this.$set(this.videoTip, idx, '正在拉流...')
      this.$set(this.channelIds, idx, channelId)
      this.saveLocalPlayData()
      try {
        const data = await this.$store.dispatch('commonChanel/playChannel', channelId)
        const streamInfo = data && data.transcodeStream ? data.transcodeStream : data
        this.setPlayStream(streamInfo, idx)
      } catch (err) {
        this.$set(this.videoTip, idx, '播放失败: ' + err)
      } finally {
        this.loading = false
      }
    },
    setPlayStream(streamInfo, idx) {
      this.$set(this.streamInfo, idx, streamInfo)
      this.$nextTick(() => {
        const refName = 'playerTabs' + (idx + 1)
        const ref = this.$refs[refName]
        if (ref) {
          const instance = ref instanceof Array ? ref[0] : ref
          if (instance && instance.setStreamInfo) {
            instance.setStreamInfo(streamInfo)
          }
        }
      })
    },
    checkPlayByParam() {
      const query = this.$route.query
      if (query.channelId) {
        this.playAt(Number(query.channelId), this.playerIdx)
      }
    },
    // 恢复本地的分屏数和播放器偏好
    restorePreference() {
      const splitStr = window.localStorage.getItem('split')
      if (splitStr !== null) {
        const split = Number(splitStr)
        if (!Number.isNaN(split) && split >= 0 && split < this.layout.length) {
          this.spiltIndex = split
        }
      }
      const globalPlayer = window.localStorage.getItem('globalPlayer')
      if (globalPlayer) {
        this.globalPlayer = globalPlayer
      }
    },
    // 按分屏数同步每格数据长度， 避免数组越界导致画面映射错位
    resizeGrid(size) {
      for (let i = this.streamInfo.length; i < size; i++) {
        this.$set(this.streamInfo, i, null)
        this.$set(this.videoTip, i, '')
        this.$set(this.channelIds, i, null)
      }
      if (this.streamInfo.length > size) {
        this.streamInfo.splice(size)
        this.videoTip.splice(size)
        this.channelIds.splice(size)
      }
      if (this.playerIdx >= size) {
        this.playerIdx = 0
      }
    },
    // 清空所有分屏画面， 只销毁本地播放器， 不调用后端停止接口， 避免影响其他终端
    clearGrid() {
      for (let i = 0; i < this.streamInfo.length; i++) {
        this.$set(this.streamInfo, i, null)
        this.$set(this.videoTip, i, '')
        this.$set(this.channelIds, i, null)
      }
      this.saveLocalPlayData()
    },
    // 加载服务端保存的默认预案并自动铺屏
    loadDefaultPreset: async function() {
      try {
        const preset = await this.$store.dispatch('playPreset/getDefaultPreset')
        if (preset && preset.itemList && preset.itemList.length > 0) {
          await this.applyPreset(preset)
          return
        }
      } catch (err) {
        console.warn('[live] 加载默认分屏预案失败， 使用本地记忆恢复', err)
      }
      await this.restoreLocalPlayData()
    },
    // 应用预案： 切换分屏数后串行拉流， 避免同时向设备发起多路 INVITE
    applyPreset: async function(preset) {
      if (!preset || !preset.itemList || preset.itemList.length === 0) {
        return
      }
      const splitIndex = this.layout.findIndex(layoutItem => layoutItem.spilt === preset.splitNum)
      if (splitIndex >= 0) {
        this.spiltIndex = splitIndex
      }
      this.resizeGrid(this.layout[this.spiltIndex].spilt)
      this.clearGrid()
      this.playerIdx = 0
      await new Promise(resolve => this.$nextTick(resolve))
      for (const item of preset.itemList) {
        const idx = item.sort - 1
        if (idx < 0 || idx >= this.layout[this.spiltIndex].spilt) {
          continue
        }
        await this.playAt(item.channelId, idx)
        // 间隔 300ms 再发起下一路， 降低设备侧并发拉流的压力
        await this.sleep(300)
      }
    },
    // 服务端没有预案时， 兼容旧版本保存的本地分屏记忆
    restoreLocalPlayData: async function() {
      let playData = []
      try {
        playData = JSON.parse(window.localStorage.getItem('playData') || '[]')
      } catch (err) {
        playData = []
      }
      if (!Array.isArray(playData) || playData.length === 0) {
        return
      }
      const size = this.layout[this.spiltIndex].spilt
      for (let i = 0; i < playData.length && i < size; i++) {
        if (playData[i]) {
          await this.playAt(playData[i], i)
          await this.sleep(300)
        }
      }
    },
    // 同步当前分屏布局到本地， 作为服务端不可用时的降级方案
    saveLocalPlayData() {
      window.localStorage.setItem('playData', JSON.stringify(this.channelIds))
    },
    sleep(ms) {
      return new Promise(resolve => setTimeout(resolve, ms))
    },
    fullScreen: function() {
      if (screenFull.isEnabled) {
        screenFull.toggle(this.$refs.playBox)
      }
    }
  }
}
</script>
<style>
.live-container {
  height: calc(100vh - 124px);
  width: 100%;
}

.live-content {
  height: 100%;
  display: flex;
  flex-direction: row;
}

.device-tree-container-box {
  width: 406px;
  min-width: 250px;
  max-width: 400px;
  background-color: #ffffff;
  overflow: auto;
  resize: horizontal;
  transition: width 0.3s ease, min-width 0.3s ease;
}

.device-tree-hidden {
  width: 0 !important;
  min-width: 0 !important;
  overflow: hidden;
  resize: none;
}

@media (max-width: 768px) {
  .live-content {
    flex-direction: column;
  }

  .device-tree-container-box {
    width: 100%;
    max-width: 100%;
    height: 200px;
    min-height: 150px;
    max-height: 300px;
    resize: vertical;
  }
}

.video-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.control-bar {
  height: 5vh;
  min-height: 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 17px;
}

.split-controls {
  text-align: left;
  padding-left: 10px;
}

.fullscreen-control {
  text-align: right;
  padding-right: 10px;
}

.ptz-toggle-control {
  text-align: right;
  padding-right: 10px;
}

.ptz-toggle-control .btn.active {
  color: #409EFF;
}

.ptz-panel {
  width: 406px;
  min-width: 340px;
  background-color: #ffffff;
  border-left: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.ptz-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;
  font-size: 15px;
  font-weight: bold;
}

.ptz-panel-header .el-icon-close {
  cursor: pointer;
  font-size: 18px;
  color: #909399;
}

.ptz-panel-header .el-icon-close:hover {
  color: #409EFF;
}

.ptz-panel-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 12px 16px;
}

.ptz-preset-section {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 8px;
}

.ptz-control-section {
  flex-shrink: 0;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid #ebeef5;
}

.ptz-divider {
  height: 1px;
  background-color: #e4e7ed;
  margin: 12px 0;
}

.ptz-empty-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
  font-size: 14px;
}

.preset-controls {
  display: flex;
  align-items: center;
}

.global-player-control {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

.player-container {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 10px;
  overflow: hidden;
}

.play-grid {
  width: 100%;
  height: 100%;
  max-height: calc(100vh - 180px);
  aspect-ratio: 16/9;
  border: 4px solid rgb(169, 168, 168);
}

.btn {
  margin: 0 10px;
  cursor: pointer;
}

.btn:hover {
  color: #409EFF;
}

.btn.active {
  color: #409EFF;
}

.sidebar-toggle {
  margin: 0 2px;
  font-size: 18px;
  vertical-align: middle;
}

.divider {
  display: inline-block;
  width: 1px;
  height: 16px;
  background-color: #dcdfe6;
  margin: 0 8px;
  vertical-align: middle;
}

.redborder {
  outline: 4px solid rgb(0, 198, 255);
}

.play-box {
  background-color: #000000;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.no-signal {
  color: #ffffff;
  font-size: 15px;
  font-weight: bold;
}

.play-box-2-1 {
  grid-column: 1 / span 2;
  grid-row: 1 / span 2;
}

/* Responsive adjustments for smaller screens */
@media (max-width: 576px) {
  .control-bar {
    flex-direction: column;
    height: auto;
    padding: 5px 0;
  }

  .split-controls, .preset-controls, .fullscreen-control {
    width: 100%;
    justify-content: center;
    text-align: center;
    padding: 5px 0;
  }

  .btn {
    margin: 0 5px;
  }
}


</style>
