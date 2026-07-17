package com.qm.common.exception;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private final String code;

    public BizException(String message) {
        this("QM-5000", message);
    }

    public BizException(String code, String message) {
        super(message);
        this.code = code;
    }

    public static BizException of(String code, String message) {
        return new BizException(code, message);
    }

    public static BizException notFound(String what) {
        return new BizException("QM-3000", what + "不存在");
    }

    public static BizException forbidden(String what) {
        return new BizException("QM-2000", "无权限: " + what);
    }

    public static BizException illegalState(String message) {
        return new BizException("QM-4000", message);
    }

    public static BizException param(String message) {
        return new BizException("QM-1000", message);
    }
}
