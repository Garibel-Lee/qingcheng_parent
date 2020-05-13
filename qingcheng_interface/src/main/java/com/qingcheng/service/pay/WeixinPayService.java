package com.qingcheng.service.pay;
import java.util.*;

/**
 * 微信支付接口
 */
public interface WeixinPayService {


    /****
     * 关闭订单
     * @param outtradeno
     */
    Map closePay(String outtradeno);

    /**
     * 生成微信支付二维码
     * @param orderId 订单号
     * @param money 金额(分)
     * @param notifyUrl 回调地址
     * @param attach:附加数据
     * @return
     */
    public Map createNative(String orderId,Integer money,String notifyUrl,String... attach);


    /**
     * 查询支付状态
     * @param orderId 订单号
     */
    public Map queryPayStatus(String orderId);


}
