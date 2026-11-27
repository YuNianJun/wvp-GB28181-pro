package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.service.IPlayPresetService;
import com.genersoft.iot.vmp.service.bean.PlayPreset;
import com.genersoft.iot.vmp.service.bean.PlayPresetItem;
import com.genersoft.iot.vmp.storager.dao.PlayPresetMapper;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 分屏监控预案服务实现
 *
 * @author wvp
 * @date 2026-11-25
 */
@Service
@Slf4j
public class PlayPresetServiceImpl implements IPlayPresetService {

    /** 支持的分屏数， 与前端分屏布局保持一致 */
    private static final List<Integer> SUPPORT_SPLIT_NUM = Arrays.asList(1, 4, 6, 9);

    /** 预案名称最大长度， 与数据库字段保持一致 */
    private static final int MAX_NAME_LENGTH = 50;

    @Autowired
    private PlayPresetMapper playPresetMapper;

    @Autowired
    private IGbChannelService channelService;

    @Override
    public List<PlayPreset> queryByUser(int userId) {
        return playPresetMapper.queryByUser(userId);
    }

    @Override
    public PlayPreset get(int presetId, int userId) {
        PlayPreset preset = playPresetMapper.get(presetId, userId);
        if (preset == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "预案不存在");
        }
        preset.setItemList(playPresetMapper.queryItems(presetId));
        return preset;
    }

    @Override
    public PlayPreset getDefault(int userId) {
        PlayPreset preset = playPresetMapper.getDefault(userId);
        if (preset == null) {
            return null;
        }
        preset.setItemList(playPresetMapper.queryItems(preset.getId()));
        return preset;
    }

    @Override
    @Transactional
    public void add(int userId, PlayPreset preset) {
        checkPreset(userId, preset, -1);
        String now = DateUtil.getNow();
        preset.setUserId(userId);
        preset.setIsDefault(false);
        preset.setCreateTime(now);
        preset.setUpdateTime(now);
        playPresetMapper.add(preset);
        saveItems(preset);
        // 用户还没有默认预案时， 将首个预案自动设为默认， 保证进入分屏监控页即可恢复
        if (playPresetMapper.getDefault(userId) == null) {
            playPresetMapper.setDefault(preset.getId(), userId, DateUtil.getNow());
        }
        log.info("[分屏预案] 新增预案成功， userId：{}， presetId：{}， name：{}", userId, preset.getId(), preset.getName());
    }

    @Override
    @Transactional
    public void update(int userId, PlayPreset preset) {
        PlayPreset exist = playPresetMapper.get(preset.getId(), userId);
        if (exist == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "预案不存在");
        }
        checkPreset(userId, preset, preset.getId());
        preset.setUserId(userId);
        preset.setUpdateTime(DateUtil.getNow());
        playPresetMapper.update(preset);
        // 明细采用整体覆盖， 避免逐条比对产生的脏数据
        playPresetMapper.cleanItems(preset.getId());
        saveItems(preset);
        log.info("[分屏预案] 更新预案成功， userId：{}， presetId：{}， name：{}", userId, preset.getId(), preset.getName());
    }

    @Override
    @Transactional
    public void delete(int presetId, int userId) {
        PlayPreset exist = playPresetMapper.get(presetId, userId);
        if (exist == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "预案不存在");
        }
        playPresetMapper.cleanItems(presetId);
        playPresetMapper.delete(presetId, userId);
        log.info("[分屏预案] 删除预案成功， userId：{}， presetId：{}", userId, presetId);
    }

    @Override
    @Transactional
    public void setDefault(int presetId, int userId) {
        PlayPreset exist = playPresetMapper.get(presetId, userId);
        if (exist == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "预案不存在");
        }
        String now = DateUtil.getNow();
        playPresetMapper.cleanDefault(userId, now);
        playPresetMapper.setDefault(presetId, userId, now);
        log.info("[分屏预案] 设置默认预案成功， userId：{}， presetId：{}", userId, presetId);
    }

    /**
     * 校验预案参数， 并规范化分屏明细
     *
     * @param userId    用户ID
     * @param preset    预案信息
     * @param excludeId 排除的预案ID， 新增时传 -1
     */
    private void checkPreset(int userId, PlayPreset preset, int excludeId) {
        if (preset == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "预案不可为空");
        }
        // 名称校验
        String name = preset.getName() == null ? "" : preset.getName().trim();
        if (name.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "预案名称不可为空");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "预案名称不能超过" + MAX_NAME_LENGTH + "个字符");
        }
        preset.setName(name);
        if (playPresetMapper.countByName(userId, name, excludeId) > 0) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "预案名称已存在");
        }
        // 分屏数校验
        if (!SUPPORT_SPLIT_NUM.contains(preset.getSplitNum())) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "分屏数只支持1/4/6/9");
        }
        List<PlayPresetItem> itemList = preset.getItemList() == null
                ? Collections.emptyList() : preset.getItemList();
        if (itemList.size() > preset.getSplitNum()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "通道数量不能超过分屏数");
        }
        // 明细校验：序号合法且不重复， 通道必须真实存在
        List<PlayPresetItem> normalizedList = new ArrayList<>(itemList.size());
        Set<Integer> usedSortSet = new HashSet<>();
        for (PlayPresetItem item : itemList) {
            if (item == null || item.getChannelId() <= 0) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "通道ID不可为空");
            }
            int sort = item.getSort();
            if (sort < 1 || sort > preset.getSplitNum()) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "分屏序号必须在1-" + preset.getSplitNum() + "之间");
            }
            if (!usedSortSet.add(sort)) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "分屏序号不能重复");
            }
            if (channelService.getOne(item.getChannelId()) == null) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "通道不存在： " + item.getChannelId());
            }
            // 只保留必要字段， 防止前端伪造主键
            PlayPresetItem normalizedItem = new PlayPresetItem();
            normalizedItem.setChannelId(item.getChannelId());
            normalizedItem.setSort(sort);
            normalizedList.add(normalizedItem);
        }
        preset.setItemList(normalizedList);
    }

    /**
     * 保存预案明细
     *
     * @param preset 预案信息， 需已生成主键
     */
    private void saveItems(PlayPreset preset) {
        List<PlayPresetItem> itemList = preset.getItemList();
        if (itemList == null || itemList.isEmpty()) {
            return;
        }
        for (PlayPresetItem item : itemList) {
            item.setPresetId(preset.getId());
        }
        playPresetMapper.batchAddItems(itemList);
    }
}
