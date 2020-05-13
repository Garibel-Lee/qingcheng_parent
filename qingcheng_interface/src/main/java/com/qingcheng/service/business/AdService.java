package com.qingcheng.service.business;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.business.Ad;

import java.util.*;

/**
 * ad业务逻辑层
 */
public interface AdService {


    public List<Ad> findAll();


    public PageResult<Ad> findPage(int page, int size);


    public List<Ad> findList(Map<String, Object> searchMap);


    public PageResult<Ad> findPage(Map<String, Object> searchMap, int page, int size);


    public Ad findById(Integer id);

    public void add(Ad ad);


    public void update(Ad ad);


    public void delete(Integer id);

    /**
     * 根据广告位置查询广告列表
     * @param position
     * @return
     */
    public List<Ad> findByPosition(String position);

    /**
     * 保存广告到redis
     * @param position
     */
    public void saveAdByPosition(String position);

    /**
     * 保存全部广告到redis
     */
    public void saveAllAd();

}
