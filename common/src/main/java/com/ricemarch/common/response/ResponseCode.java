package com.ricemarch.common.response;

public enum ResponseCode {
    SUCCESS(200, "Request Successfully!"),
    BAD_REQUEST(400, "Request Successfully!"),
    UNAUTHORIZED(401, "Request Successfully!");

    private final Integer code;
    private final String message;

    private ResponseCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
