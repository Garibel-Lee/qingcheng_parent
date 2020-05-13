package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.pojo.seckill.SeckillGoods;
import com.qingcheng.service.seckill.SeckillGoodsService;
import com.qingcheng.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

/****
 * @Author:shenkunlin
 * @Date:2019/5/27 12:22
 * @Description:
 *****/
@RestController
@RequestMapping(value = "/seckill/goods")
public class SeckillGoodsController {

    @Reference
    private SeckillGoodsService seckillGoodsService;


    /***
     * URL:/seckill/goods/one
     * 根据商品ID查询商品详情
     * @param time:时间
     * @param id:商品ID
     */
    @GetMapping(value = "/one")
    public SeckillGoods one(String time,Long id){
        return seckillGoodsService.one(time,id);
    }

    /*****
     * URL:/seckill/goods/list
     * 加载对应时区的秒杀商品
     * @param  time:2019052715
     */
    @GetMapping(value = "/list")
    public List<SeckillGoods> list(String time){
        return seckillGoodsService.list(time);
    }


    /****
     * 加载所有时间菜单
     * @return
     */
    @RequestMapping(value = "/menus")
    public List<Date> loadMenus(){
        return DateUtil.getDateMenus();
    }



}
