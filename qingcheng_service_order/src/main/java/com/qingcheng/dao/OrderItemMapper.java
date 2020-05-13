package com.qingcheng.dao;

import com.qingcheng.pojo.order.CategoryReport;
import com.qingcheng.pojo.order.OrderItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.time.LocalDate;
import java.util.List;

public interface OrderItemMapper extends Mapper<OrderItem> {



}
