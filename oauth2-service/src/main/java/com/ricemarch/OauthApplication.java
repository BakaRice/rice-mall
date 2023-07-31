package com.ricemarch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;

@SpringBootApplication
@EnableDiscoveryClient // 开启服务注册发现
@EnableResourceServer
public class OauthApplication {

    public static void main(String[] args) {
        SpringApplication.run(OauthApplication.class);
    }
}
