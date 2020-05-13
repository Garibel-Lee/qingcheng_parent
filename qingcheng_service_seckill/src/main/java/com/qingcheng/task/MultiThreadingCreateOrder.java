package com.qingcheng.task;

import com.alibaba.fastjson.JSON;
import com.qingcheng.dao.SeckillGoodsMapper;
import com.qingcheng.pojo.seckill.SeckillGoods;
import com.qingcheng.pojo.seckill.SeckillOrder;
import com.qingcheng.util.IdWorker;
import com.qingcheng.util.SeckillStatus;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Date;

/****
 * @Author:itheima
 * @Description:
 *      多线程异步操作类
 *****/
@Component
public class MultiThreadingCreateOrder {



    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private IdWorker idWorker;

    /*****
     * 异步操作方法
     *      run
     * 注解：@Async
     */
    @Async
    public void createOrder(){
        try {
            System.out.println("----准备@Async执行----");
            Thread.sleep(10000);

            SeckillStatus seckillStauts = (SeckillStatus) redisTemplate.boundListOps("SeckillOrderQueue").rightPop();

            //用户抢单数据
            String username=seckillStauts.getUsername();
            String time=seckillStauts.getTime();
            Long id=seckillStauts.getGoodsId();

            //获取队列中的商品id
            Object sid = redisTemplate.boundListOps("SeckillGoodsCountList_" + id).rightPop();

            //售罄
            if(sid==null){
                //清理排队信息
                clearQueue(seckillStauts);
                return;
            }

            //查询商品详情
            SeckillGoods goods = (SeckillGoods) redisTemplate.boundHashOps("SeckillGoods_"+time).get(id);

            Thread.sleep(10000);
            System.out.println(username+":"+Thread.currentThread().getId()+"----查询到 的商品库存："+goods.getStockCount());
            if(goods!=null && goods.getStockCount()>0){
                //创建订单
                SeckillOrder seckillOrder = new SeckillOrder();
                seckillOrder.setId(idWorker.nextId());
                seckillOrder.setSeckillId(id);
                seckillOrder.setMoney(goods.getCostPrice());
                seckillOrder.setUserId(username);
                seckillOrder.setSellerId(goods.getSellerId());
                seckillOrder.setCreateTime(new Date());
                seckillOrder.setStatus("0");
                redisTemplate.boundHashOps("SeckillOrder").put(username,seckillOrder);

                //库存削减
                Long surplusCount = redisTemplate.boundHashOps("SeckillGoodsCount").increment(goods.getId(), -1);
                goods.setStockCount(surplusCount.intValue());
                //商品库存=0->将数据同步到MySQL，并清理Redis缓存
                if(surplusCount<=0){
                    seckillGoodsMapper.updateByPrimaryKeySelective(goods);
                    //清理Redis缓存
                    redisTemplate.boundHashOps("SeckillGoods_"+time).delete(id);
                }else{
                    //将数据同步到Redis
                    redisTemplate.boundHashOps("SeckillGoods_"+time).put(id,goods);
                }

                //变更抢单状态
                seckillStauts.setOrderId(seckillOrder.getId());
                seckillStauts.setMoney(seckillOrder.getMoney().floatValue());
                seckillStauts.setStatus(2); //抢单成功，待支付
                redisTemplate.boundHashOps("UserQueueStatus").put(username,seckillStauts);


                //发送消息
                sendDelayMessage(seckillStauts);
            }
            System.out.println("----正在执行----");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /***
     * 清理用户排队信息
     * @param seckillStauts
     */
    private void clearQueue(SeckillStatus seckillStauts) {
        //清理重复排队标识
        redisTemplate.boundHashOps("UserQueueCount").delete(seckillStauts.getUsername());

        //清理排队存储信息
        redisTemplate.boundHashOps("UserQueueStatus").delete(seckillStauts.getUsername());
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /***
     * 延时消息发送
     * @param seckillStatus
     */
    public void sendDelayMessage(SeckillStatus seckillStatus){
        rabbitTemplate.convertAndSend(
                "exchange.delay.order.begin",
                "delay",
                JSON.toJSONString(seckillStatus),       //发送数据
                new MessagePostProcessor() {
                    @Override
                    public Message postProcessMessage(Message message) throws AmqpException {
                        //消息有效期30分钟
                        message.getMessageProperties().setExpiration(String.valueOf(10000));
                        return message;
                    }
                });
    }
}
