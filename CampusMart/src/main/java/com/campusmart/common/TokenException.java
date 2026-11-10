package com.campusmart.common;
/**
 * 认证异常：token 缺失、过期、非法
 */
public class TokenException extends RuntimeException{

    public TokenException(String msg){
        super(msg);
    }
}
