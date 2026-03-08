package com.demo.aiknowledge.common;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(200, "Success"),
    SYSTEM_ERROR(500, "System Error"),
    INVALID_PARAMS(400, "Invalid Parameters"),
    
    // Auth related
    INVALID_PHONE_FORMAT(4001, "手机号格式错误，请输入11位有效的手机号"),
    VERIFICATION_CODE_REQUIRED(4002, "验证码不能为空"),
    VERIFICATION_CODE_EXPIRED(4003, "验证码已过期或未发送"),
    INVALID_VERIFICATION_CODE(4004, "验证码错误"),
    PHONE_ALREADY_REGISTERED(4005, "该手机号已注册"),
    USERNAME_SENSITIVE(4006, "用户名包含敏感词"),
    USER_NOT_FOUND(4007, "用户不存在，请先注册"),
    INVALID_PASSWORD(4008, "密码错误"),
    PASSWORD_TOO_SHORT(4009, "密码长度不能少于6位");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
