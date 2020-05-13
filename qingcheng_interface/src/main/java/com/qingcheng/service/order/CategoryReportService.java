package com.qingcheng.service.order;

import com.qingcheng.pojo.order.CategoryReport;

import java.time.LocalDate;
import java.util.Date;
import java.util.*;

/**
 * 报表服务层接口
 */
public interface CategoryReportService {

    /**
     * 商品类目按日期统计(订单表关联查询)
     * @param date
     * @return
     */
    public List<CategoryReport> categoryReport(LocalDate date);


    /**
     * 统计后批量插入数据
     */
    public void createData();

    /**
     * 一级类目统计
     * @param date1
     * @param date2
     * @return
     */
    public List<Map> category1Count(String date1 , String date2 );

}
