package com.ricemarch.user.controller;

import com.ricemarch.common.response.CommonResponse;
import com.ricemarch.user.pojo.User;
import com.ricemarch.user.service.UserRegisterLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/register")
public class UserRegisterLoginController {

    @Autowired
    private UserRegisterLoginService userService;

    //用户名 + 密码
    @PostMapping("/name-password")
    public CommonResponse namePasswordRegister(@RequestBody User user) {
        return userService.namePasswordRegister(user);
    }
}
