package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.dao.SeckillGoodsMapper;
import com.qingcheng.dao.SeckillOrderMapper;
import com.qingcheng.pojo.seckill.SeckillGoods;
import com.qingcheng.pojo.seckill.SeckillOrder;
import com.qingcheng.service.seckill.SeckillOrderService;
import com.qingcheng.task.MultiThreadingCreateOrder;
import com.qingcheng.util.IdWorker;
import com.qingcheng.util.SeckillStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Date;

/****
 * @Author:itheima
 * @Date:2019/5/27 18:06
 * @Description:
 *****/
@Service
public class SeckillOrderServiceImpl implements SeckillOrderService {

    @Autowired
    private MultiThreadingCreateOrder multiThreadingCreateOrder;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;


    /****
     * 根据用户名查询订单后
     * @param username
     * @return
     */
    @Override
    public SeckillOrder queryByUserName(String username) {
        return (SeckillOrder) redisTemplate.boundHashOps("SeckillOrder").get(username);
    }

    /***
     * 修改订单
     * @param outtradeno
     * @param username
     * @param transactionid
     */
    @Override
    public void updateStatus(String outtradeno, String username, String transactionid) {
        //根据用户名查询订单数据
        SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.boundHashOps("SeckillOrder").get(username);

        if(seckillOrder!=null){
            //修改订单->持久化到MySQL中
            seckillOrder.setPayTime(new Date());
            seckillOrder.setStatus("1");        //已支付
            seckillOrderMapper.insertSelective(seckillOrder);

            //Redis中的订单
            redisTemplate.boundHashOps("SeckillOrder").delete(username);

            //清理用户排队信息
            redisTemplate.boundHashOps("UserQueueCount").delete(username);

            //清理排队存储信息
            redisTemplate.boundHashOps("UserQueueStatus").delete(username);
        }
    }

    /****
     * 查询抢单状态
     * @param username
     * @return
     */
    @Override
    public SeckillStatus queryStatus(String username) {
        return (SeckillStatus) redisTemplate.boundHashOps("UserQueueStatus").get(username);
    }

    /***
     * 下单实现
     * @param id:商品ID
     * @param time:商品时区
     * @param username:用户名
     * @return
     */
    @Override
    public Boolean add(Long id, String time, String username) {
        //自增特性
        //  incr(key,value):让指定key的值自增value->返回自增的值->单线程操作
        //  A   第1次:  incr(username,1)->1
        //      第2次:  incr(username,1)->2
        //      利用自增，如果用户多次提交或者多次排队，则递增值>1
        Long userQueueCount = redisTemplate.boundHashOps("UserQueueCount").increment(username, 1);
        if(userQueueCount>1){
            System.out.println("重复抢单.....");
            //100:错误状态码  重复排队
            throw new RuntimeException("100");
        }


        //减少无效排队
        Long size = redisTemplate.boundListOps("SeckillGoodsCountList_" + id).size();
        if(size<=0){
            //101:表示没有库存
            throw  new RuntimeException("101");
        }

        //创建队列所需的排队信息
        SeckillStatus seckillStatus = new SeckillStatus(username,new Date(),1,id, time);
        //将排队信息存入到List中
        redisTemplate.boundListOps("SeckillOrderQueue").leftPush(seckillStatus);

        //存储排队信息
        redisTemplate.boundHashOps("UserQueueStatus").put(username,seckillStatus);

        //异步操作调用
        multiThreadingCreateOrder.createOrder();
        System.out.println("----其他程序正在执行-----");
        return true;
    }
}
