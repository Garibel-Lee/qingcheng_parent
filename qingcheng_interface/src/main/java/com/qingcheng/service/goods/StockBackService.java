package com.qingcheng.service.goods;

import com.qingcheng.pojo.goods.StockBack;
import com.qingcheng.pojo.order.OrderItem;

import java.util.List;

public interface StockBackService  {



    public void add(StockBack stockBack);


    public void addList(List<OrderItem> orderItems);

    /**
     *  执行库存回滚
     */
    public void doBack();

}
