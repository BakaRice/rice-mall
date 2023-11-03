package com.ricemarch.user.controller;

import com.ricemarch.common.response.CommonResponse;
import com.ricemarch.user.pojo.User;
import com.ricemarch.user.service.UserRegisterLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/user/register")
public class UserRegisterLoginController {

    @Autowired
    private UserRegisterLoginService userService;

    // 用户名 + 密码
    @PostMapping("/name-password")
    public CommonResponse namePasswordRegister(@RequestBody User user) {
        return userService.namePasswordRegister(user);
    }

    @PostMapping("/phone-code")
    public CommonResponse phoneCodeRegister(@RequestParam String phone, @RequestParam String code) {
        return userService.phoneCodeRegister(phone, code);
    }

    // gitee 第三方账号登录
    // 这个接口是 第三方平台调用咱们的，这个叫回调接口
    @RequestMapping("/gitee")
    public CommonResponse thirdPartGiteeCallback(HttpServletRequest request) {
        return userService.thirdPartGiteeCallback(request);
    }

    @RequestMapping("/login")
    public CommonResponse login(@RequestParam String username,
                                @RequestParam String password) {
        return userService.login(username, password);
    }

}
