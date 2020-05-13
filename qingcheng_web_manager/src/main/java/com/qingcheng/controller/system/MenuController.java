package com.qingcheng.controller.system;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.service.system.MenuService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.*;

@RestController
@RequestMapping("/menu")
public class MenuController {

    @Reference
    private MenuService menuService;


    @GetMapping("/findMenu")
    public List<Map> findMenu(){
        String loginName = SecurityContextHolder.getContext().getAuthentication().getName();
        return menuService.findMenuListByLoginName(loginName);
    }



}
