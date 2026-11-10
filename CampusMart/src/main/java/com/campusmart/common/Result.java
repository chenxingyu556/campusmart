package com.campusmart.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class Result<T> implements Serializable {
    private Integer code;
    private String  msg;
    private T       data;

    public static <T> Result<T> ok(T data) {
        return build(0, "ok", data);
    }

    public static <T> Result<T> ok() {
        return build(0, "ok", null);
    }

    public static <T> Result<T> fail(int code, String msg) {
        return build(code, msg, null);
    }

    private static <T> Result<T> build(int code, String msg, T data) {
        Result<T> r = new Result<>();
        r.code = code;
        r.msg = msg;
        r.data = data;
        return r;
    }
}
