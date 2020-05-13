package com.qingcheng.controller.goods;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.entity.Result;
import com.qingcheng.pojo.goods.Sku;
import com.qingcheng.service.goods.SkuSearchService;
import com.qingcheng.service.goods.SkuService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SkuSearchController {

    @Reference
    private SkuSearchService skuSearchService;

    @Reference
    private SkuService skuService;

    @GetMapping("/importSkuList")
    public Result importSkuList(){
        List<Sku> skuList = skuService.findAll();
        skuSearchService.importSkuList(skuList);
        return new Result();
    }


}
