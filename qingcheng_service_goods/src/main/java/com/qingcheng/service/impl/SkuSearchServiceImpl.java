package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.qingcheng.dao.BrandMapper;
import com.qingcheng.dao.SkuMapper;
import com.qingcheng.dao.SpecMapper;
import com.qingcheng.pojo.goods.Sku;
import com.qingcheng.service.goods.BrandService;
import com.qingcheng.service.goods.SkuSearchService;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import tk.mybatis.mapper.entity.Example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SkuSearchServiceImpl implements SkuSearchService {

    @Autowired
    @Qualifier("restHighLevelClient")
    private RestHighLevelClient restHighLevelClient;


    private String indexName="sku";

    private String typeName="doc";

    //字段列表
    //private String fields="id,name,price,num,image,createTime,spuid,categoryName,brandName,salenum,commentnum";

    @Autowired
    private SkuMapper skuMapper;

    /**
     * 批量导入SKU
     */
    /*
    public void importSkuList() {
        System.out.println("导入数据开始");
        //1.查询需要导入的SKU列表数据
        Example example=new Example(Sku.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("status","1");
        List<Sku> skuList = skuMapper.selectByExample(example);
        importSkuList(skuList);
    }*/

    @Override
    public void importSkuList(List<Sku> skuList) {
        //2.构建BulkRequest
        BulkRequest bulkRequest = new BulkRequest();
        for (Sku sku : skuList) {
            if("1".equals(sku.getStatus())){
                IndexRequest indexRequest = new IndexRequest(indexName, typeName, sku.getId().toString());
                Map skuMap=new HashMap();
                skuMap.put("name",sku.getName());
                skuMap.put("brandName",sku.getBrandName());
                skuMap.put("categoryName",sku.getCategoryName());
                skuMap.put("image",sku.getImage());
                skuMap.put("price",sku.getPrice());
                skuMap.put("createTime",sku.getCreateTime());
                skuMap.put("saleNum",sku.getSaleNum());
                skuMap.put("commentNum",sku.getCommentNum());
                skuMap.put("spec",JSON.parseObject(sku.getSpec(),Map.class) );
                indexRequest.source(skuMap);
                bulkRequest.add(indexRequest);
            }
        }
        //BulkResponse BulkResponse= restHighLevelClient.bulk(bulkRequest);  同步调用方式
        //异步调用方式
        restHighLevelClient.bulkAsync(bulkRequest,new ActionListener<BulkResponse>() {
            @Override
            public void onResponse(BulkResponse bulkResponse) {
                //成功
                System.out.println("导入成功"+bulkResponse.status());
            }
            @Override
            public void onFailure(Exception e) {
                //失败
                System.out.println("导入失败"+e.getMessage());
            }
        });
        System.out.println("调用完成");
    }


    @Autowired
    private BrandService brandService;

    @Autowired
    private BrandMapper brandMapper;

    /**
     * 查询
     * @param searchMap
     * @return
     */
    public Map search(Map<String, String> searchMap){
        Map resultMap = new HashMap();//返回结果
        //1.列表查询
        resultMap.putAll(searchSkuList(searchMap));
        //2.查询商品分类列表
        List<String> categoryList = searchCategoryList(searchMap);
        resultMap.put("categoryList",categoryList);
        System.out.println("categoryList："+categoryList.size());
        //3.根据商品分类查询品牌
        String categoryName="";//商品分类名称
        if(searchMap.get("category") == null){  //如果没有分类条件
            if(categoryList.size()>0){
                categoryName=categoryList.get(0);
            }
        }else{//如果有商品分类条件
            categoryName=searchMap.get("category");
        }
        resultMap.put("brandList",brandMapper.findListByCategoryName(categoryName));

        //4.根据商品分类查询规格
        resultMap.put("specList",getSpecList(categoryName));


        return resultMap;
    }


    /**
     * 查询列表
     *
     * @param searchMap
     * @return
     */
    private Map searchSkuList(Map<String, String> searchMap) {
        Map resultMap = new HashMap();//返回结果
        //创建搜索请求对象 参数索引名称
        SearchRequest searchRequest = new SearchRequest(indexName);
        //设置索引类型 doc
        searchRequest.types(typeName);
        // 查询构建器
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //String[] sourceFieldArray = fields.split(",");
        //根据需要果过滤不需要查询的字段,提高查询性能呢
        //searchSourceBuilder.fetchSource(sourceFieldArray, new String[]{});
        //根据关键字进行搜索
        BoolQueryBuilder boolQueryBuilder = buildBasicQuery(searchMap);
        //查询列表
        searchSourceBuilder.query(boolQueryBuilder);

        //分页
        Integer pageNo = Integer.parseInt(searchMap.get("pageNo"));//页码
        if(pageNo<=0){
            pageNo = 1;
        }
        Integer pageSize = 30;//页大小
        //起始记录下标
        int fromIndex = (pageNo - 1) * pageSize;
        searchSourceBuilder.from(fromIndex);//开始索引
        searchSourceBuilder.size(pageSize);//页大小

        //排序
        String sortRule = searchMap.get("sortRule");// 排序规则 ASC  DESC
        String sortField = searchMap.get("sortField");//排序字段  price
        if (sortField != null && !"".equals(sortField)) {//有排序
            searchSourceBuilder.sort(SortBuilders.fieldSort(sortField).order(SortOrder.valueOf(sortRule)));
        }

        //设置高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<font style='color:red'>");
        highlightBuilder.postTags("</font>");
        highlightBuilder.fields().add(new HighlightBuilder.Field("name"));
        searchSourceBuilder.highlighter(highlightBuilder);

        searchRequest.source(searchSourceBuilder);
        try {
            //搜索查询
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest);
            //获取查询结果
            SearchHits searchHits = searchResponse.getHits();
            //获取内容数据
            List<Map<String, Object>> skuContentList = getSkuContent(searchHits);
            resultMap.put("rows", skuContentList);

            //计算页码数量
            long totalCount = searchHits.getTotalHits();//总记录数
            long pageCount = (totalCount % pageSize == 0) ? totalCount / pageSize : (totalCount / pageSize + 1);
            resultMap.put("totalPages", pageCount);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultMap;
    }


    /**
     * 构建基本查询
     *
     * @param searchMap
     * @return
     */
    private BoolQueryBuilder buildBasicQuery(Map<String, String> searchMap) {
        // 构建布尔查询
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        //1.关键字查询
        queryBuilder.must(QueryBuilders.matchQuery("name", searchMap.get("keywords")));

        //2.商品分类筛选
        if(searchMap.get("category")!=null){
            queryBuilder.filter(QueryBuilders.matchQuery("categoryName", searchMap.get("category")));
        }

        //3.品牌筛选
        if (searchMap.get("brand") != null) {
            queryBuilder.filter(QueryBuilders.matchQuery("brandName", searchMap.get("brand")));
        }

        //4.规格筛选
        for (String key : searchMap.keySet()) {
            if (key.startsWith("spec.")) {//如果是规格参数
                queryBuilder.filter(QueryBuilders.matchQuery(key , searchMap.get(key)));
                //queryBuilder.filter(QueryBuilders.matchQuery("spec." + key.substring(5) , searchMap.get(key)));
            }
        }

        //5.价格筛选
        if(searchMap.get("price")!=null){
            String[] price = ((String)searchMap.get("price")).split("-");
            if(!price[0].equals("0")){//最低价格不等于0
                queryBuilder.filter(QueryBuilders.rangeQuery("price").gt(price[0]+"00"));
            }
            if(!price[1].equals("*")){//如果价格有上限
                queryBuilder.filter(QueryBuilders.rangeQuery("price").lt(price[1]+"00"));
            }
        }

        return queryBuilder;
    }


    //获取内容数据
    private List<Map<String, Object>> getSkuContent(SearchHits searchHits) {
        SearchHit[] searchHit = searchHits.getHits();
        List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();
        for (SearchHit hit : searchHits) {
            //获取原文档数据
            Map<String, Object> skuMap = hit.getSourceAsMap();
            //提取高亮内容
            String name ="";
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if(highlightFields!=null) {
                HighlightField highlightFieldName = highlightFields.get("name");
                if (highlightFieldName != null) {
                    Text[] fragments = highlightFieldName.fragments();
                    name = fragments[0].toString();
                }
            }
            skuMap.put("name",name); //设置高亮
            resultList.add(skuMap);
        }
        return resultList;
    }


    /**
     * 查询分类列表
     * @param searchMap
     * @return
     */
    private List<String> searchCategoryList(Map searchMap) {
        //创建搜索请求对象
        SearchRequest searchRequest = new SearchRequest(indexName);
        //设置搜索类型
        searchRequest.types(typeName);
        //查询列表
        BoolQueryBuilder boolQueryBuilder = buildBasicQuery(searchMap);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        String groupName = "sku_category";
        //聚合分类查询
        TermsAggregationBuilder aggregation = AggregationBuilders.terms(groupName).field("categoryName.keyword");
        searchSourceBuilder.aggregation(aggregation);
        searchSourceBuilder.size(0);
        //执行查询
        searchSourceBuilder.query(boolQueryBuilder);
        searchRequest.source(searchSourceBuilder);

        List<String> categoryList = null;
        try {
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest);
            Aggregations aggregations = searchResponse.getAggregations();
            Map<String, Aggregation> asMap = aggregations.getAsMap();
            Terms terms = (Terms) asMap.get(groupName);
            categoryList = terms.getBuckets().stream().map(bucket -> bucket.getKeyAsString()).collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return categoryList;
    }

    @Autowired
    private SpecMapper specMapper;

    /**
     * 返回规格列表
     * @param categoryName
     * @return
     */
    private List<Map> getSpecList(String categoryName){
        List<Map> specList = specMapper.findListByCategoryName(categoryName);
        for(Map spec:specList){
            String[] options = spec.get("options").toString().split(",");
            spec.put("options", options);
        }
        return specList;
    }


}
