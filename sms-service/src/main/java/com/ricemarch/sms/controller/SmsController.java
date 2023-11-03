package com.ricemarch.sms.controller;

import com.ricemarch.sms.service.SmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sms")
public class SmsController {

    @Autowired
    private SmsService smsService;

    @RequestMapping("/send-msg-code")
    public void sendSms(@RequestParam String phoneNumber) {
        smsService.sendSms(phoneNumber);
    }
}