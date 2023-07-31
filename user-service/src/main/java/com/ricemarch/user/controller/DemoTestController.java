package com.ricemarch.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class DemoTestController {

    @GetMapping("/test")
    public String test(){
        return "Hello!";
    }
}
