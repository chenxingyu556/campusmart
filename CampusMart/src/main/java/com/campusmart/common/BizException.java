package com.campusmart.common;

/**
 * 业务异常：可预期的错误，对外暴露具体原因
 */

public class BizException extends RuntimeException{

    private final int code;

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public BizException(String msg) {
        this(ErrorCode.SYSTEM_ERROR, msg);
    }

    public int getCode() {
        return code;
    }
}
