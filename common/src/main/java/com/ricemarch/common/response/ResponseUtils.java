package com.ricemarch.common.response;

public class ResponseUtils {
    // 成功的访问
    public static CommonResponse okResponse(Object content) {
        return CommonResponse.builder()
                .code(ResponseCode.SUCCESS.getCode())
                .message(ResponseCode.SUCCESS.getMessage())
                .content(content)
                .build();
    }
    // 失败的访问
    public static CommonResponse failResponse(Integer code, Object content, String errMsg) {
        return CommonResponse.builder()
                .content(content)
                .code(code)
                .message(errMsg)
                .build();
    }
}
