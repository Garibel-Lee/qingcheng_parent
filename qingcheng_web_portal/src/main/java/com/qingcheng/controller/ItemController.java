package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.qingcheng.pojo.goods.Goods;
import com.qingcheng.pojo.goods.Sku;
import com.qingcheng.pojo.goods.Spu;
import com.qingcheng.service.goods.CategoryService;
import com.qingcheng.service.goods.SpuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/item")
public class ItemController {

    @Reference
    private SpuService spuService;

    @Reference
    private CategoryService categoryService;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${pagePath}")
    private String pagePath;

    /**
     * 根据SPU id生成商品详细页
     * @param id
     */
    @GetMapping("/createPage")
    public void createPage(String id){
        //查询商品信息
        Goods goods = spuService.findGoodsById(id);
        //获取SPU 信息
        Spu spu = goods.getSpu();
        //获取sku列表
        List<Sku> skuList = goods.getSkuList();
        //查询商品分类
        List<String> categoryList=new ArrayList<String>();
        categoryList.add(categoryService.findById(spu.getCategory1Id()).getName());//一级分类
        categoryList.add(categoryService.findById(spu.getCategory2Id()).getName());//二级分类
        categoryList.add(categoryService.findById(spu.getCategory3Id()).getName());//三级分类

        //生成SKU地址列表
        Map urlMap=new HashMap();
        for(Sku sku:skuList){
            //对规格json字符串进行排序
            String specJson=  JSON.toJSONString(  JSON.parseObject(sku.getSpec()), SerializerFeature.SortField.MapSortField );
            urlMap.put(specJson,sku.getId()+".html");
        }

        //创建页面（每个SKU为一个页面）
        for(Sku sku:skuList){
            // 1.上下文
            Context context = new Context();
            //创建数据模型
            Map<String, Object> dataModel =new HashMap();
            dataModel.put("spu",spu);
            dataModel.put("sku",sku);

            dataModel.put("categoryList",categoryList);//商品分类面包屑

            dataModel.put("skuImages",  sku.getImages().split(","));//SKU图片列表
            dataModel.put("spuImages",  spu.getImages().split(","));//SPU图片列表

            Map paraItems = JSON.parseObject(spu.getParaItems());//SPU参数列表
            dataModel.put("paraItems", paraItems);
            Map specItems = JSON.parseObject(sku.getSpec());//当前SKU规格
            dataModel.put("specItems", specItems);

            //规格选择面板
            Map<String,List> specMap  = (Map) JSON.parse(spu.getSpecItems());
            for(String key:specMap.keySet()  ){//循环规格名称
                List<String> list = specMap.get(key);
                List<Map> mapList=new ArrayList<Map>();
                for(String value:list ){//循环规格选项值
                    Map map=new HashMap();
                    map.put("option",value);

                    if(specItems.get(key).equals(value)){  //判断此规格组合是否是当前SKU的，标记选中状态
                        map.put("checked",true);
                    }else{
                        map.put("checked",false);
                    }
                    //商品详细页地址
                    Map spec= JSON.parseObject(sku.getSpec());//当前SKU规格
                    spec.put(key,value);
                    String specJson=  JSON.toJSONString( spec, SerializerFeature.SortField.MapSortField );
                    map.put("url",urlMap.get(specJson));

                    mapList.add(map);
                }
                specMap.put(key,mapList);//用新集合覆盖原集合
            }
            dataModel.put("specMap", specMap);//规格面板
            context.setVariables(dataModel);
            // 2.准备文件
            File dir = new File(pagePath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File dest = new File(dir, sku.getId() + ".html");
            // 3.生成页面
            try {
                PrintWriter writer = new PrintWriter(dest, "UTF-8");
                templateEngine.process("item", context, writer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
