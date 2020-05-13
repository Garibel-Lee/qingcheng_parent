package com.qingcheng.service.impl;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.CategoryBrandMapper;
import com.qingcheng.dao.CategoryMapper;
import com.qingcheng.dao.SkuMapper;
import com.qingcheng.dao.SpuMapper;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.goods.*;
import com.qingcheng.service.goods.SkuSearchService;
import com.qingcheng.service.goods.SkuService;
import com.qingcheng.service.goods.SpuService;
import com.qingcheng.util.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.*;

@Service(interfaceClass=SpuService.class)
@Transactional
public class SpuServiceImpl implements SpuService {

    @Autowired
    private SpuMapper spuMapper;


    /**
     * 返回全部记录
     * @return
     */
    public List<Spu> findAll() {
        return spuMapper.selectAll();
    }

    /**
     * 分页查询
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<Spu> findPage(int page, int size) {
        PageHelper.startPage(page,size);
        Page<Spu> spus = (Page<Spu>) spuMapper.selectAll();
        return new PageResult<Spu>(spus.getTotal(),spus.getResult());
    }

    /**
     * 条件查询
     * @param searchMap 查询条件
     * @return
     */
    public List<Spu> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return spuMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<Spu> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page,size);
        Example example = createExample(searchMap);
        Page<Spu> spus = (Page<Spu>) spuMapper.selectByExample(example);
        return new PageResult<Spu>(spus.getTotal(),spus.getResult());
    }

    /**
     * 根据Id查询
     * @param id
     * @return
     */
    public Spu findById(String id) {
        return spuMapper.selectByPrimaryKey(id);
    }

    /**
     * 新增
     * @param spu
     */
    public void add(Spu spu) {
        spuMapper.insert(spu);
    }

    /**
     * 修改
     * @param spu
     */
    public void update(Spu spu) {
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    /**
     *  删除
     * @param id
     */
    public void delete(String id) {
        Spu spu = spuMapper.selectByPrimaryKey(id);
        //检查是否被逻辑删除  ,必须先逻辑删除后才能物理删除
        if(!spu.getIsDelete().equals("1")){
            throw new RuntimeException("此商品不能删除！");
        }
        spuMapper.deleteByPrimaryKey(id);
    }

    /**
     * 逻辑删除
     * @param id
     */
    public void logicDelete(String id) {
        Spu spu = spuMapper.selectByPrimaryKey(id);
        //检查是否下架的商品
        if(!spu.getIsMarketable().equals("0")){
            throw new RuntimeException("必须先下架再删除！");
        }
        spu.setIsDelete("1");//删除
        spu.setStatus("0");//未审核
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    /**
     * 恢复数据
     * @param id
     */
    public void restore(String id) {
        Spu spu = spuMapper.selectByPrimaryKey(id);
        //检查是否删除的商品
        if(!spu.getIsDelete().equals("1")){
            throw new RuntimeException("此商品未删除！");
        }
        spu.setIsDelete("0");//未删除
        spu.setStatus("0");//未审核
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    /**
     *  审核
     * @param id
     */
    public void audit(String id) {

        Spu spu = spuMapper.selectByPrimaryKey(id);
        //检查是否删除的商品
        if(spu.getIsDelete().equals("1")){
            throw new RuntimeException("此商品已删除！");
        }
        spu.setStatus("1");//已审核
        spu.setIsMarketable("1");//审核后自动上架
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    /**
     * 下架商品
     * @param id
     */
    public void pull(String id) {
        Spu spu = spuMapper.selectByPrimaryKey(id);
        if(spu.getIsDelete().equals("1")){
            throw new RuntimeException("此商品已删除！");
        }
        spu.setIsMarketable("0");//下架状态
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    /**
     * 上架商品
     * @param id
     */
    public void put(String id) {
        Spu spu = spuMapper.selectByPrimaryKey(id);
        //检查是否删除的商品
        if(spu.getIsDelete().equals("1")){
            throw new RuntimeException("此商品已删除！");
        }
        if(!spu.getStatus().equals("1")){
            throw new RuntimeException("未通过审核的商品不能！");
        }
        spu.setIsMarketable("1");//上架状态
        spuMapper.updateByPrimaryKeySelective(spu);
    }


    /**
     * 批量上架商品
     * @param ids
     */
    public int putMany(String[] ids) {
        Spu spu=new Spu();
        spu.setIsMarketable("1");//上架
        //批量修改
        Example example=new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andIn("id", Arrays.asList(ids));//id
        criteria.andEqualTo("isMarketable","0");//下架
        criteria.andEqualTo("status","1");//审核通过的
        criteria.andEqualTo("isDelete","0");//非删除的
        return spuMapper.updateByExampleSelective(spu, example);
    }


    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private CategoryMapper categoryMapper;


    @Autowired
    private CategoryBrandMapper categoryBrandMapper;


    @Autowired
    private SkuService skuService;

    @Autowired
    private SkuSearchService skuSearchService;

    /**
     * 保存商品 SPU+SKU列表
     * @param goods 商品组合实体类
     */
    public void saveGoods(Goods goods) {
        boolean isAdd=true;//是否新增
        //取出spu部分
        Spu spu = goods.getSpu();
        if(spu.getId()==null){  //新增
            spu.setId(idWorker.nextId()+"");//设置ID
            System.out.println("新增");
        }else{//如果修改，删除原SKU列表
            isAdd=false;
            //删除原sku列表
            Example example=new Example(Sku.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("spuId",spu.getId());
            skuMapper.deleteByExample(example);
            System.out.println("修改");
        }
        Date date=new Date();//获取当前日期
        //根据商品分类ID查询商品名称
        Category category = categoryMapper.selectByPrimaryKey(spu.getCategory3Id());
        //取出sku列表部分
        List<Sku> skuList = goods.getSkuList();
        for (Sku sku :skuList){
            if(sku.getId()==null){  //新增的SKU
                sku.setId(idWorker.nextId()+"");//分布式ID
                sku.setCreateTime(date);//创建日期
            }
            //构建SKU名称，采用SPU+规格值组装
            if(sku.getSpec()==null || "".equals(sku.getSpec())){
                sku.setSpec("{}");
            }
            String name= spu.getName();
            Map<String,String> specMap=  JSON.parseObject(sku.getSpec(),Map.class);//规格
            for(String value:specMap.values()){
                name+=" "+ value;
            }
            sku.setName(name);//名称
            sku.setSpuId(spu.getId()+"");//设置spu的ID
            sku.setUpdateTime(date);//修改日期
            sku.setCategoryId(category.getId());//商品分类ID
            sku.setCategoryName(category.getName());//商品分类名称
            skuMapper.insert(sku);//插入sku表数据


            skuService.savePriceToRedisBySkuId(sku.getId()+"",sku.getPrice());//更新价格到redis
        }

        skuSearchService.importSkuList(skuList);

        spu.setStatus("0");//无论是新增还是修改，都重新将商品的审核状态设置为未审核
        spu.setIsDelete("0");//是否删除
        spu.setIsMarketable("0"); //上架状态
        if(isAdd){
            spu.setCommentNum(0);//评论数
            spu.setSaleNum(0);//销量
            spuMapper.insert(spu);//保存spu
        }else{
            spuMapper.updateByPrimaryKeySelective(spu);
        }

        //添加 模板品牌关联
        //先查询是否存在记录
        CategoryBrand categoryBrand =new CategoryBrand();
        categoryBrand.setBrandId(spu.getBrandId());
        categoryBrand.setCategoryId(spu.getCategory3Id());
        int count=categoryBrandMapper.selectCount(categoryBrand);
        if(count==0) {
            categoryBrandMapper.insert(categoryBrand);
        }

    }





    /**
     * 根据ID查询商品
     * @param id
     * @return
     */
    public Goods findGoodsById(String id){
        //查询spu
        Spu spu = spuMapper.selectByPrimaryKey(id);

        //查询SKU 列表
        Example example=new Example(Sku.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("spuId",id);
        List<Sku> skuList = skuMapper.selectByExample(example);

        //封装，返回
        Goods goods=new Goods();
        goods.setSpu(spu);
        goods.setSkuList(skuList);
        return goods;
    }

    /**
     * 构建查询条件
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap){
        Example example=new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        if(searchMap!=null){
            // SPU名
            if(searchMap.get("name")!=null && !"".equals(searchMap.get("name"))){
                criteria.andLike("name","%"+searchMap.get("name")+"%");
            }
            // 副标题
            if(searchMap.get("caption")!=null && !"".equals(searchMap.get("caption"))){
                criteria.andLike("caption","%"+searchMap.get("caption")+"%");
            }
            // 图片
            if(searchMap.get("image")!=null && !"".equals(searchMap.get("image"))){
                criteria.andLike("image","%"+searchMap.get("image")+"%");
            }
            // 图片列表
            if(searchMap.get("images")!=null && !"".equals(searchMap.get("images"))){
                criteria.andLike("images","%"+searchMap.get("images")+"%");
            }
            // 售后服务
            if(searchMap.get("saleService")!=null && !"".equals(searchMap.get("saleService"))){
                criteria.andLike("saleService","%"+searchMap.get("saleService")+"%");
            }
            // 介绍
            if(searchMap.get("introduction")!=null && !"".equals(searchMap.get("introduction"))){
                criteria.andLike("introduction","%"+searchMap.get("introduction")+"%");
            }
            // 规格列表
            if(searchMap.get("specItems")!=null && !"".equals(searchMap.get("specItems"))){
                criteria.andLike("specItems","%"+searchMap.get("specItems")+"%");
            }
            // 参数列表
            if(searchMap.get("paraItems")!=null && !"".equals(searchMap.get("paraItems"))){
                criteria.andLike("paraItems","%"+searchMap.get("paraItems")+"%");
            }
            // 是否上架
            if(searchMap.get("isMarketable")!=null && !"".equals(searchMap.get("isMarketable"))){
                criteria.andLike("isMarketable","%"+searchMap.get("isMarketable")+"%");
            }
            // 是否启用规格
            if(searchMap.get("isEnableSpec")!=null && !"".equals(searchMap.get("isEnableSpec"))){
                criteria.andLike("isEnableSpec","%"+searchMap.get("isEnableSpec")+"%");
            }
            // 是否删除
            if(searchMap.get("isDelete")!=null && !"".equals(searchMap.get("isDelete"))){
                criteria.andLike("isDelete","%"+searchMap.get("isDelete")+"%");
            }
            // 审核状态
            if(searchMap.get("status")!=null && !"".equals(searchMap.get("status"))){
                criteria.andLike("status","%"+searchMap.get("status")+"%");
            }

            // 品牌ID
            if(searchMap.get("brandId")!=null ){
                criteria.andEqualTo("brandId",searchMap.get("brandId"));
            }
            // 一级分类
            if(searchMap.get("category1Id")!=null ){
                criteria.andEqualTo("category1Id",searchMap.get("category1Id"));
            }
            // 二级分类
            if(searchMap.get("category2Id")!=null ){
                criteria.andEqualTo("category2Id",searchMap.get("category2Id"));
            }
            // 三级分类
            if(searchMap.get("category3Id")!=null ){
                criteria.andEqualTo("category3Id",searchMap.get("category3Id"));
            }
            // 模板ID
            if(searchMap.get("templateId")!=null ){
                criteria.andEqualTo("templateId",searchMap.get("templateId"));
            }
            // 运费模板id
            if(searchMap.get("freightId")!=null ){
                criteria.andEqualTo("freightId",searchMap.get("freightId"));
            }
            // 销量
            if(searchMap.get("saleNum")!=null ){
                criteria.andEqualTo("saleNum",searchMap.get("saleNum"));
            }
            // 评论数
            if(searchMap.get("commentNum")!=null ){
                criteria.andEqualTo("commentNum",searchMap.get("commentNum"));
            }

        }
        return example;
    }

}
