import {
  addPreset,
  deletePreset,
  getDefaultPreset,
  getPreset,
  queryList,
  setDefaultPreset,
  updatePreset
} from '@/api/playPreset'

// 分屏监控预案
const actions = {
  // 查询当前用户的全部分屏预案
  queryList({ commit }) {
    return new Promise((resolve, reject) => {
      queryList().then(response => {
        resolve(response ? response.data : [])
      }).catch(error => {
        reject(error)
      })
    })
  },
  // 查询分屏预案详情
  getPreset({ commit }, presetId) {
    return new Promise((resolve, reject) => {
      getPreset(presetId).then(response => {
        resolve(response ? response.data : null)
      }).catch(error => {
        reject(error)
      })
    })
  },
  // 查询当前用户的默认分屏预案
  getDefaultPreset({ commit }) {
    return new Promise((resolve, reject) => {
      getDefaultPreset().then(response => {
        resolve(response ? response.data : null)
      }).catch(error => {
        reject(error)
      })
    })
  },
  // 新增分屏预案
  addPreset({ commit }, data) {
    return new Promise((resolve, reject) => {
      addPreset(data).then(response => {
        resolve(response ? response.data : null)
      }).catch(error => {
        reject(error)
      })
    })
  },
  // 更新分屏预案
  updatePreset({ commit }, data) {
    return new Promise((resolve, reject) => {
      updatePreset(data).then(response => {
        resolve(response ? response.data : null)
      }).catch(error => {
        reject(error)
      })
    })
  },
  // 删除分屏预案
  deletePreset({ commit }, presetId) {
    return new Promise((resolve, reject) => {
      deletePreset(presetId).then(response => {
        resolve(response ? response.data : null)
      }).catch(error => {
        reject(error)
      })
    })
  },
  // 设置默认分屏预案
  setDefaultPreset({ commit }, presetId) {
    return new Promise((resolve, reject) => {
      setDefaultPreset(presetId).then(response => {
        resolve(response ? response.data : null)
      }).catch(error => {
        reject(error)
      })
    })
  }
}

export default {
  namespaced: true,
  actions
}
