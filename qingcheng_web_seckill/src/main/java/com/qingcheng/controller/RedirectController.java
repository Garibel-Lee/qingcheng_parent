package com.qingcheng.controller;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

/****
 * @Author:itheima
 * @Date:2019/5/27 17:54
 * @Description:
 *****/
@Controller
@RequestMapping(value = "/redirect")
public class RedirectController {


    /****
     * URL:/redirect/back
     * 跳转方法
     * referer:用户访问该方法的来源页面地址
     */
    @RequestMapping(value = "/back")
    public String back(@RequestHeader(value = "referer",required = false)String referer){
        if(!StringUtils.isEmpty(referer)){
            return "redirect:"+referer;
        }
        return "/seckill-index.html";
    }


}
