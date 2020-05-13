package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.entity.Result;
import com.qingcheng.pojo.user.User;
import com.qingcheng.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {


    @Reference
    private UserService userService;

    /**
     * 发送短信验证码
     * @param phone
     */
    @GetMapping(value="/sendSms")
    public Result sendSms(String phone){
        userService.sendSms(phone);
        return new Result();
    }


    @PostMapping("/save")
    public Result save(@RequestBody User user,String smsCode){
        userService.add(user,smsCode);
        return new Result();
    }


}
