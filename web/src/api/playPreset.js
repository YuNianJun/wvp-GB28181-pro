import request from '@/utils/request'

// 分屏监控预案API

// 查询当前用户的全部分屏预案
export function queryList() {
  return request({
    method: 'get',
    url: '/api/play/preset/list'
  })
}

// 查询分屏预案详情
export function getPreset(presetId) {
  return request({
    method: 'get',
    url: '/api/play/preset/get',
    params: {
      presetId: presetId
    }
  })
}

// 查询当前用户的默认分屏预案
export function getDefaultPreset() {
  return request({
    method: 'get',
    url: '/api/play/preset/default'
  })
}

// 新增分屏预案
export function addPreset(data) {
  return request({
    method: 'post',
    url: '/api/play/preset/add',
    data: data
  })
}

// 更新分屏预案
export function updatePreset(data) {
  return request({
    method: 'post',
    url: '/api/play/preset/update',
    data: data
  })
}

// 删除分屏预案
export function deletePreset(presetId) {
  return request({
    method: 'delete',
    url: '/api/play/preset/delete',
    params: {
      presetId: presetId
    }
  })
}

// 设置默认分屏预案
export function setDefaultPreset(presetId) {
  return request({
    method: 'get',
    url: '/api/play/preset/set-default',
    params: {
      presetId: presetId
    }
  })
}
