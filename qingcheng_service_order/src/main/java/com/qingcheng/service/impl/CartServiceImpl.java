package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.qingcheng.pojo.goods.Sku;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.pojo.order.OrderItem;
import com.qingcheng.service.goods.SkuService;
import com.qingcheng.service.order.CartService;
import com.qingcheng.service.order.PreferentialService;
import com.qingcheng.util.CacheKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
public class CartServiceImpl implements CartService {


    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public List<Map<String, Object>> findCartList(String username) {
        System.out.println("从redis中提取购物车"+username);
        List<Map<String,Object>> cartList = (List<Map<String,Object>>) redisTemplate.boundHashOps(CacheKey.CART_LIST).get(username);
        if(cartList==null){
            return new ArrayList<>();
        }
        return cartList;
    }

    @Reference
    private SkuService skuService;

    @Override
    public void addItem(String username, String skuId, Integer num) {

        //获取购物车
        List<Map<String,Object>> cartList = findCartList(username);
        boolean flag=false;//是否是购物车中的商品
        //判断缓存中是含有已购物商品
        for (Map map:cartList) {
            OrderItem orderItem=(OrderItem)map.get("item");//提取购物车
            if(orderItem.getSkuId().equals(skuId)){ //如果在购物车中找到
                //计算单个商品重量
                int weight= orderItem.getWeight()/orderItem.getNum();//单个重量
                orderItem.setNum(orderItem.getNum()+num);//数量添加
                orderItem.setWeight(weight*orderItem.getNum());//重量计算
                orderItem.setMoney(orderItem.getNum()*orderItem.getPrice());//金额
                //如果购物数量小于等于0说明客户不购买了,应删除购物明细
                if(orderItem.getNum()<=0){
                    cartList.remove(map);
                }
                flag = true;
                break;
            }
        }
        if(flag==false){ //如果没找到，则增加商品到购物车
            //添加商品
            Sku sku = skuService.findById(skuId);
            if(sku==null){
                throw new RuntimeException("商品不存在");
            }
            if(!sku.getStatus().equals("1")){
                throw new RuntimeException("商品状态不合法");
            }
            //数量不能为0或负数
            if(num<=0){
                throw new RuntimeException("商品数量不合法");
            }
            OrderItem orderItem=new OrderItem();
            orderItem.setCategoryId3(sku.getCategoryId());//分类ID
            orderItem.setSpuId(sku.getSpuId());
            orderItem.setSkuId(sku.getId());
            orderItem.setNum(num);
            orderItem.setImage(sku.getImage());
            orderItem.setPrice(sku.getPrice());
            orderItem.setName(sku.getName());
            orderItem.setMoney( orderItem.getPrice()* num);
            if(sku.getWeight()==null){
                sku.setWeight(0);
            }
            orderItem.setWeight(sku.getWeight()*num);//重量计算
            Map map=new HashMap();
            map.put("item",orderItem);
            map.put("checked",true);//默认被选中
            cartList.add(map);
        }
        redisTemplate.boundHashOps(CacheKey.CART_LIST).put(username,cartList);//存入缓存

    }

    @Override
    public boolean updateChecked(String username, String skuId, boolean checked) {
        //获取购物车
        List<Map<String,Object>> cartList= findCartList(username);
        //判断缓存中是含有已购物商品
        boolean isOk=false;
        for (Map map:cartList) {
            OrderItem orderItem=(OrderItem)map.get("item");
            if(orderItem.getSkuId().equals(skuId)){
                map.put("checked",checked);
                isOk=true;//执行成功
                break;
            }
        }
        if(isOk){
            redisTemplate.boundHashOps(CacheKey.CART_LIST).put(username,cartList);//存入缓存
        }
        return isOk;
    }

    /**
     * 获取选中的购物车
     * @param username
     * @return
     */
    private List<OrderItem> findCheckedCartList(String username) {
        return  findCartList(username)
                .stream()
                .filter(cart -> (boolean) cart.get("checked"))
                .map(cart -> (OrderItem) cart.get("item"))
                .collect(Collectors.toList());
    }

    @Autowired
    private PreferentialService preferentialService;

    /**
     * 计算当前选中的购物车的优惠金额
     * @param
     * @return
     */
    public int preferential(String username){
        //获取选中的购物车
        List<OrderItem> cartList = findCheckedCartList(username);
        //按分类聚合统计每个分类的金额
        Map<Integer, IntSummaryStatistics> collect = cartList.stream().collect(Collectors.groupingBy(OrderItem::getCategoryId3, Collectors.summarizingInt(OrderItem::getMoney)));
        int allPreMoney=0;//优惠金额
        //循环分类
        for( Integer categoryId :collect.keySet() ){
            int  money = (int)collect.get(categoryId).getSum();//品类消费金额合计
            System.out.println("分类："+categoryId+"  金额："+money);
            allPreMoney += preferentialService.findPreMoneyByCategoryId(categoryId, money);//根据分类ID和消费金额查询优惠金额
        }
        return allPreMoney;
    }

    @Override
    public List<Map<String, Object>> findNewOrderItemList(String username) {
        //获取选中的购物车
        List<Map<String, Object>> cartList = findCartList(username);
        //循环购物车列表，重新读取每个商品的最新价格
        for(Map<String, Object> cart:cartList){
            OrderItem orderItem=(OrderItem)cart.get("item");
            Sku sku = skuService.findById(orderItem.getSkuId());
            orderItem.setPrice(sku.getPrice());//更新价格
            orderItem.setMoney(sku.getPrice()*orderItem.getNum());//更新金额
        }
        redisTemplate.boundHashOps(CacheKey.CART_LIST).put(username,cartList);//存入缓存
        return cartList;
    }

    @Override
    public void deleteCheckedCart(String username) {
        //获取未选中的购物车
        List<Map<String, Object>> cartList = findCartList(username).stream()
                .filter(cart -> (boolean) cart.get("checked") == false)
                .collect(Collectors.toList());

        redisTemplate.boundHashOps(CacheKey.CART_LIST).put(username,cartList);//存入缓存
    }


}
