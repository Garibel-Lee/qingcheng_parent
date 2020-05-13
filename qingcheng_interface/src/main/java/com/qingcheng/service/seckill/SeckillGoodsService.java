package com.qingcheng.service.seckill;

import com.qingcheng.pojo.seckill.SeckillGoods;

import java.util.List;

/****
 * @Author:shenkunlin
 * @Date:2019/5/27 12:12
 * @Description:
 *****/
public interface SeckillGoodsService {


    /***
     * 根据商品ID查询商品详情
     * @param time:商品秒杀时区
     * @param id:商品ID
     */
    SeckillGoods one(String time,Long id);

    /***
     * 根据时间区间查询秒杀商品列表
     * @param time
     */
    List<SeckillGoods> list(String time);

}
