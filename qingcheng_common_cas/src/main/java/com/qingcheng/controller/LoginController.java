package com.qingcheng.controller;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.*;

@RestController
@RequestMapping("/login")
public class LoginController {

    /**
     * 获取用户名
     * @return
     */
    @RequestMapping("/username")
    public Map showName(){
        String name = SecurityContextHolder.getContext().getAuthentication().getName();//得到登陆人账号
        if("anonymousUser".equals(name)){
            name="";
        }
        System.out.println(name);
        Map map=new HashMap();
        map.put("username", name);
        return map;
    }


}


