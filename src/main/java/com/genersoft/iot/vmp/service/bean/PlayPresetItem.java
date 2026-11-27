package com.genersoft.iot.vmp.service.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分屏监控预案明细， 一条记录对应一个分屏格子
 *
 * @author wvp
 * @date 2026-11-25
 */
@Data
@Schema(description = "分屏监控预案明细")
public class PlayPresetItem {

    @Schema(description = "明细数据库ID")
    private int id;

    @Schema(description = "所属预案ID")
    private int presetId;

    @Schema(description = "通道数据库ID， 对应 wvp_device_channel.id")
    private int channelId;

    @Schema(description = "分屏序号， 从1开始")
    private int sort;

    @Schema(description = "通道名称， 仅查询时返回")
    private String channelName;
}
