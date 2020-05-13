package com.qingcheng.service.seckill;

import com.qingcheng.pojo.seckill.SeckillOrder;
import com.qingcheng.util.SeckillStatus;

/****
 * @Author:itheima
 * @Date:2019/5/27 18:06
 * @Description:
 *****/
public interface SeckillOrderService {


    /****
     * 根据用户名查询订单后
     * @param username
     */
    SeckillOrder queryByUserName(String username);

    /***
     * 修改订单
     * @param outtradeno
     * @param username
     * @param transactionid
     */
    void updateStatus(String outtradeno,String username,String transactionid);

    /***
     * 查询抢单状态
     * @param username
     */
    SeckillStatus queryStatus(String username);


    /****
     * 下单实现
     * @param id:商品ID
     * @param time:商品时区
     * @param username:用户名
     * @return
     */
    Boolean add(Long id,String time,String username);

}
