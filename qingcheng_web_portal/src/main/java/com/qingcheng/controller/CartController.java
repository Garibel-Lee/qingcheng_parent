package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.entity.Result;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.pojo.order.OrderItem;
import com.qingcheng.pojo.user.Address;
import com.qingcheng.service.order.CartService;
import com.qingcheng.service.order.OrderService;
import com.qingcheng.service.user.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
@RestController
@RequestMapping("/cart")
public class CartController {

    @Reference
    private CartService cartService;

    /**
     * 从redis中提取购物车
     * @return
     */
    @GetMapping("/findCartList")
    public List<Map<String, Object>> findCartList(){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Map<String, Object>> cartList = cartService.findCartList(username);
        return cartList;
    }

    /**
     * 添加商品到购物车
     * @param skuId
     * @param num
     */
    @GetMapping("/addItem")
    public Result addItem( String  skuId,  Integer num){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        cartService.addItem(username,skuId,num);
        return new Result();
    }

    /**
     * 更改购物车项选中状态
     * @param skuId
     * @param checked
     * @return
     */
    @GetMapping("/updateChecked")
    public Result updateChecked(String skuId, boolean checked){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        cartService.updateChecked(username,skuId,checked);
        return new Result();
    }


    /**
     * 计算当前选中的购物车的优惠金额
     * @param
     * @return
     */
    @GetMapping("/preferential")
    public Map preferential(){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        int preferential = cartService.preferential(username);
        Map map=new HashMap();
        map.put("preferential",preferential);
        return map;
    }

    /**
     * 获取刷新单价后的购物车列表
     * @return
     */
    @GetMapping("/findNewOrderItemList")
    public List<Map<String, Object>> findNewOrderItemList(){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return cartService.findNewOrderItemList(username);
    }

    @Reference
    private AddressService addressService;

    /**
     * 根据用户名查询地址列表
     * @return
     */
    @GetMapping("/findAddressList")
    public List<Address> findAddressList(){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return addressService.findByUsername(username);
    }

    /**
     * 删除选中的购物车
     */
    @GetMapping("/deleteCheckedCart")
    public Result deleteCheckedCart(){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        cartService.deleteCheckedCart(username);
        return  new Result();
    }


    @Reference
    private OrderService orderService;

    /**
     * 保存订单
     * @return
     */
    @PostMapping("/saveOrder")
    public Map<String, Object> saveOrder(@RequestBody Order order){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        order.setUsername(username);
        Map<String, Object> map = orderService.add(order);
        return map;
    }


}