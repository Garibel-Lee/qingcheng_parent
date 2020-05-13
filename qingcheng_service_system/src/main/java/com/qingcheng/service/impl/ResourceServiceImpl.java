package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.dao.ResourceMapper;
import com.qingcheng.service.system.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
@Service
public class ResourceServiceImpl implements ResourceService {

    @Autowired
    private ResourceMapper resourceMapper;

    @Override
    public List<String> findResKeyByLoginName(String loginName) {
        return resourceMapper.findResKeyByLoginName(loginName);
    }
}
