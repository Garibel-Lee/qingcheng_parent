package com.qingcheng.consumer;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.qingcheng.dao.SeckillGoodsMapper;
import com.qingcheng.pojo.seckill.SeckillGoods;
import com.qingcheng.pojo.seckill.SeckillOrder;
import com.qingcheng.service.pay.WeixinPayService;
import com.qingcheng.util.SeckillStatus;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;

/****
 * @Author:itheima
 * @Description:
 *****/
public class OrderMessageListener implements MessageListener {

    @Autowired
    private RedisTemplate redisTemplate;

    @Reference
    private WeixinPayService weixinPayService;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    /***
     * 消息监听
     * @param message
     */
    @Override
    public void onMessage(Message message) {
        String content = new String(message.getBody());
        System.out.println("监听到的消息："+content);

        //回滚操作
        rollbackOrder(JSON.parseObject(content,SeckillStatus.class));
    }


    /*****
     * 订单回滚操作
     * @param seckillStatus
     */
    public void rollbackOrder(SeckillStatus seckillStatus){
        if(seckillStatus==null){
            return;
        }
        //判断Redis中是否存在对应的订单
        SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.boundHashOps("SeckillOrder").get(seckillStatus.getUsername());

        //如果存在，开始回滚
        if(seckillOrder!=null){
            //1.关闭微信支付
            Map<String,String> map = weixinPayService.closePay(seckillStatus.getOrderId().toString());

            if(map.get("return_code").equals("SUCCESS") && map.get("result_code").equals("SUCCESS")){
                //2.删除用户订单
                redisTemplate.boundHashOps("SeckillOrder").delete(seckillOrder.getUserId());

                //3.查询出商品数据
                SeckillGoods goods = (SeckillGoods) redisTemplate.boundHashOps("SeckillGoods_"+seckillStatus.getTime()).get(seckillStatus.getGoodsId());
                if(goods==null){
                    //数据库中加载数据
                    goods = seckillGoodsMapper.selectByPrimaryKey(seckillStatus.getGoodsId());
                }

                //4.递增库存  incr
                Long seckillGoodsCount = redisTemplate.boundHashOps("SeckillGoodsCount").increment(seckillStatus.getGoodsId(), 1);
                goods.setStockCount(seckillGoodsCount.intValue());

                //5.将商品数据同步到Redis
                redisTemplate.boundHashOps("SeckillGoods_"+seckillStatus.getTime()).put(seckillStatus.getGoodsId(),goods);
                redisTemplate.boundListOps("SeckillGoodsCountList_"+seckillStatus.getGoodsId()).leftPush(seckillStatus.getGoodsId());
                //6.清理用户抢单排队信息
                //清理重复排队标识
                redisTemplate.boundHashOps("UserQueueCount").delete(seckillStatus.getUsername());

                //清理排队存储信息
                redisTemplate.boundHashOps("UserQueueStatus").delete(seckillStatus.getUsername());
            }
        }
    }
}
