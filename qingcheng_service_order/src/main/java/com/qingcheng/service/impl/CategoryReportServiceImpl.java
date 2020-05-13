package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.dao.CategoryReportMapper;
import com.qingcheng.pojo.order.CategoryReport;
import com.qingcheng.service.order.CategoryReportService;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Date;
import java.util.*;
@Service
public class CategoryReportServiceImpl implements CategoryReportService {

    @Autowired
    private CategoryReportMapper categoryReportMapper;

    @Override
    public List<CategoryReport> categoryReport(LocalDate date) {
        return categoryReportMapper.categoryReport(date);
    }


    @Override
    public void createData() {

        LocalDate localDate = LocalDate.now().minusDays(1);// 得到昨天的日期
        List<CategoryReport> categoryReports = categoryReportMapper.categoryReport(localDate);
        for(CategoryReport categoryReport:categoryReports){
            categoryReportMapper.insert(categoryReport);
        }
    }

    @Override
    public List<Map> category1Count(String date1, String date2) {
        return categoryReportMapper.category1Count(date1,date2);
    }

}
