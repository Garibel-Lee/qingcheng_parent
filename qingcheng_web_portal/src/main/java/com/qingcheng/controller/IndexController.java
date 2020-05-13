package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.pojo.business.Ad;
import com.qingcheng.service.business.AdService;
import com.qingcheng.service.goods.CategoryService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class IndexController {

    @Reference
    private AdService adService;


    @Reference
    private CategoryService categoryService;

    /**
     * 网站首页
     * @return
     */
    @GetMapping("/index")
    public String index(Model model){
        //查询首页轮播图
        List<Ad> lbtList = adService.findByPosition("web_index_lb");
        model.addAttribute("lbt",lbtList);
        //查询商品分类
        List<Map> categoryList = categoryService.findCategoryTree();
        model.addAttribute("categoryList",categoryList);


        return "index";
    }

}
