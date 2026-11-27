package com.genersoft.iot.vmp.vmanager.playPreset;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.conf.security.SecurityUtils;
import com.genersoft.iot.vmp.service.IPlayPresetService;
import com.genersoft.iot.vmp.service.bean.PlayPreset;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分屏监控预案接口
 * 预案按用户隔离， 任意终端登录后可通过默认预案恢复固定的分屏画面
 *
 * @author wvp
 * @date 2026-11-25
 */
@Tag(name = "分屏监控预案")
@Slf4j
@RestController
@RequestMapping("/api/play/preset")
public class PlayPresetController {

    @Autowired
    private IPlayPresetService playPresetService;

    @ResponseBody
    @GetMapping("/list")
    @Operation(summary = "查询当前用户的全部分屏预案", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public List<PlayPreset> list() {
        return playPresetService.queryByUser(SecurityUtils.getUserId());
    }

    @ResponseBody
    @GetMapping("/get")
    @Operation(summary = "查询分屏预案详情", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "presetId", description = "预案ID", required = true)
    public PlayPreset get(Integer presetId) {
        if (presetId == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "预案ID不可为NULL");
        }
        return playPresetService.get(presetId, SecurityUtils.getUserId());
    }

    @ResponseBody
    @GetMapping("/default")
    @Operation(summary = "查询当前用户的默认分屏预案", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public PlayPreset getDefault() {
        return playPresetService.getDefault(SecurityUtils.getUserId());
    }

    @ResponseBody
    @PostMapping("/add")
    @Operation(summary = "新增分屏预案", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "preset", description = "预案信息", required = true)
    public void add(@RequestBody PlayPreset preset) {
        playPresetService.add(SecurityUtils.getUserId(), preset);
    }

    @ResponseBody
    @PostMapping("/update")
    @Operation(summary = "更新分屏预案", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "preset", description = "预案信息", required = true)
    public void update(@RequestBody PlayPreset preset) {
        if (preset == null || preset.getId() <= 0) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "预案ID不可为NULL");
        }
        playPresetService.update(SecurityUtils.getUserId(), preset);
    }

    @ResponseBody
    @DeleteMapping("/delete")
    @Operation(summary = "删除分屏预案", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "presetId", description = "预案ID", required = true)
    public void delete(Integer presetId) {
        if (presetId == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "预案ID不可为NULL");
        }
        playPresetService.delete(presetId, SecurityUtils.getUserId());
    }

    @ResponseBody
    @GetMapping("/set-default")
    @Operation(summary = "设置默认分屏预案", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "presetId", description = "预案ID", required = true)
    public void setDefault(Integer presetId) {
        if (presetId == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "预案ID不可为NULL");
        }
        playPresetService.setDefault(presetId, SecurityUtils.getUserId());
    }
}
