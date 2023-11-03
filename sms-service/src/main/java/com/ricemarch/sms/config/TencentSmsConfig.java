package com.ricemarch.sms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "tencent.sms")
public class TencentSmsConfig {
    private String secretId;
    private String secretKey;
    private String region; // ap-beijing； ap-nanjing; ap-guangzhou
    private String appId;
    private String signName;
    private TencentSmsTempalteConfig templateId;
}
