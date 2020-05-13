package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.github.wxpay.sdk.WXPayUtil;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.service.order.OrderService;
import com.qingcheng.service.pay.WeixinPayService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
@RestController
@RequestMapping("/pay")
public class PayController {


    @Reference
    private WeixinPayService weixinPayService;

    @Reference
    private OrderService orderService;

    /**
     * 生成二维码
     * @return
     */
    @GetMapping("/createNative")
    public Map createNative(String orderId){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Order order =   orderService.findById(orderId);
        if(order!=null){
            //校验该订单是否时当前用户的订单，并且时未支付的订单
            if("0".equals(order.getPayStatus()) &&  "0".equals(order.getOrderStatus()) && username.equals(order.getUsername())){
                return weixinPayService.createNative(order.getId(),order.getPayMoney(),"http://qingcheng.cross.echosite.cn/pay/notify.do");
            }else{
                return null;
            }
        }else{
            return null;
        }
    }

    /**
     * 通知
     * @return
     */
    @RequestMapping("/notify")
    public Map notifyLogic(HttpServletRequest request){
        System.out.println("回调了.....");
        InputStream inStream;
        try {
            inStream = request.getInputStream();
            ByteArrayOutputStream outSteam = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = inStream.read(buffer)) != -1) {
                outSteam.write(buffer, 0, len);
            }
            outSteam.close();
            inStream.close();
            String result = new String(outSteam.toByteArray(), "utf-8");// 获取微信调用我们notify_url的返回信息
            System.out.println(result);
            Map<String, String> map = WXPayUtil.xmlToMap(result);
            orderService.updatePayStatus(map.get("out_trade_no"),map.get("transaction_id"));//更新订单状态

        } catch (Exception e) {
            e.printStackTrace();
            //记录错误日志
        }
        return new HashMap();
    }


    @GetMapping("/queryPayStatus")
    public Map queryPayStatus(String orderId){
        return  weixinPayService.queryPayStatus(orderId);
    }

}
