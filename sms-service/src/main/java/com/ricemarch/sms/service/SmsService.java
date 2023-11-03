package com.ricemarch.sms.service;


import com.ricemarch.sms.config.TencentSmsConfig;
import com.ricemarch.sms.processor.RedisCommonProcessor;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.DescribePhoneNumberInfoRequest;
import com.tencentcloudapi.sms.v20210111.models.DescribePhoneNumberInfoResponse;
import com.tencentcloudapi.sms.v20210111.models.PhoneNumberInfo;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class SmsService {

    @Autowired
    private TencentSmsConfig tencentSmsConfig;

    @Autowired
    private RedisCommonProcessor redisCommonProcessor;

    public void sendSms(String phoneNumber) {
        try {
            Credential cred = new Credential(tencentSmsConfig.getSecretId(), tencentSmsConfig.getSecretKey());

            // 选择性的配置内容，是关于HTTPProfile的配置
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setConnTimeout(60);
            httpProfile.setReqMethod("POST");

            // 可选性配置内容
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);

            SmsClient client = new SmsClient(cred, tencentSmsConfig.getRegion(), clientProfile);
            SendSmsRequest req = new SendSmsRequest();
            req.setSmsSdkAppId(tencentSmsConfig.getAppId());
            req.setSignName(tencentSmsConfig.getSignName());
            req.setTemplateId(tencentSmsConfig.getTemplateId().getPhoneCode());

            // req中至少还要组合两个信息，一个是phonenumber， 一个是code
            String code = getRandomPhoneCode();
            String[] templateParamSet = {code};
            req.setTemplateParamSet(templateParamSet);

            // 这个phonenumber是带国家编号的 中国 +86135101010110
            String[] phoneNumberSet = {phoneNumber};
            req.setPhoneNumberSet(phoneNumberSet);

            // 如果有需要发送国际短信，比如港澳台、国际的，我们需要自己申请一个 senderId
//        req.setSenderId("senderId");
            client.SendSms(req);
            redisCommonProcessor.set(phoneNumber, code, 300);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 没有必要的话，别用，让大家知道怎么去使用这个接口，查询phoneNumber的国家代码
    private Map getNationalCode(SmsClient client, String[] phoneNumberSet) {
        DescribePhoneNumberInfoRequest request = new DescribePhoneNumberInfoRequest();
        request.setPhoneNumberSet(phoneNumberSet);
        HashMap<String, String> mapResults = new HashMap<>();
        try {
            DescribePhoneNumberInfoResponse response
                    = client.DescribePhoneNumberInfo(request);
            PhoneNumberInfo[] phoneNumberInfoSet = response.getPhoneNumberInfoSet();
            for (int i = 0; i < phoneNumberInfoSet.length; i++) {
                mapResults.put(phoneNumberInfoSet[0].getPhoneNumber(),
                        phoneNumberInfoSet[0].getNationCode());
            }

        } catch (Exception e) {
            return new HashMap();
        }
        return mapResults;
    }

    private String getRandomPhoneCode() {
        return String.valueOf((Math.random() * 9 + 1) * 100000);
    }
}
