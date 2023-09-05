package com.ricemarch.gateway.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Lazy
@FeignClient("oauth2-service")
public interface Oauth2ServiceClient {

    //   /oauth/check_token?token=64f77567-e780-400a-a4b8-8a6da9e5f029
    @RequestMapping("/oauth/check_token")
    Map<String, Object> checkToken(@RequestParam("token") String token);
}
