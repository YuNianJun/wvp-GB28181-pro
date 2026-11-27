package com.genersoft.iot.vmp.service.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 分屏监控预案
 * 每个用户可以保存多套预案， 其中一套作为默认预案， 任意终端登录后进入分屏监控页自动恢复。
 *
 * @author wvp
 * @date 2026-11-25
 */
@Data
@Schema(description = "分屏监控预案")
public class PlayPreset {

    @Schema(description = "预案数据库ID")
    private int id;

    @Schema(description = "所属用户ID")
    private int userId;

    @Schema(description = "预案名称")
    private String name;

    @Schema(description = "分屏数， 支持1/4/6/9")
    private int splitNum;

    @Schema(description = "是否默认预案")
    private Boolean isDefault;

    @Schema(description = "预案包含的通道数量， 仅列表查询时返回")
    private int channelCount;

    @Schema(description = "创建时间")
    private String createTime;

    @Schema(description = "更新时间")
    private String updateTime;

    @Schema(description = "分屏内容， 按 sort 对应分屏序号")
    private List<PlayPresetItem> itemList;
}
