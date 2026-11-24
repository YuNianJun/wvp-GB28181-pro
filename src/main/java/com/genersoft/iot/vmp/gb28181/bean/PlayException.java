package com.genersoft.iot.vmp.gb28181.bean;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayException extends RuntimeException{
    private int code;
    private String msg;

    public PlayException(int code, String msg) {
        // 调用父类构造，保证 e.getMessage() 也能拿到业务提示，避免全局异常处理返回 msg=null
        super(msg);
        this.code = code;
        this.msg = msg;
    }
}
