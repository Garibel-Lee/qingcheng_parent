package com.qingcheng.service.impl;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

/**
 * rest客户端连接工厂
 */
public class RestClientFactory {

    private static RestHighLevelClient restHighLevelClient;

    static{
        restHighLevelClient=new RestHighLevelClient(RestClient.builder(new HttpHost("127.0.0.1", 9200, "http")));
    }

    public static RestHighLevelClient getRestHighLevelClient(){
        return restHighLevelClient;
    }

}
