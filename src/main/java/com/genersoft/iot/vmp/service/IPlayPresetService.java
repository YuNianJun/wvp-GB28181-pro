package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.service.bean.PlayPreset;

import java.util.List;

/**
 * 分屏监控预案服务
 *
 * @author wvp
 * @date 2026-11-25
 */
public interface IPlayPresetService {

    /**
     * 查询用户的全部预案
     *
     * @param userId 用户ID
     * @return 预案列表
     */
    List<PlayPreset> queryByUser(int userId);

    /**
     * 查询预案详情， 包含分屏明细
     *
     * @param presetId 预案ID
     * @param userId   用户ID
     * @return 预案详情
     */
    PlayPreset get(int presetId, int userId);

    /**
     * 查询用户的默认预案
     *
     * @param userId 用户ID
     * @return 默认预案， 不存在时返回 null
     */
    PlayPreset getDefault(int userId);

    /**
     * 新增预案， 用户没有默认预案时自动设为默认
     *
     * @param userId 用户ID
     * @param preset 预案信息
     */
    void add(int userId, PlayPreset preset);

    /**
     * 更新预案， 同时覆盖分屏明细
     *
     * @param userId 用户ID
     * @param preset 预案信息
     */
    void update(int userId, PlayPreset preset);

    /**
     * 删除预案及其分屏明细
     *
     * @param presetId 预案ID
     * @param userId   用户ID
     */
    void delete(int presetId, int userId);

    /**
     * 设置默认预案， 同一用户只保留一个默认
     *
     * @param presetId 预案ID
     * @param userId   用户ID
     */
    void setDefault(int presetId, int userId);
}
