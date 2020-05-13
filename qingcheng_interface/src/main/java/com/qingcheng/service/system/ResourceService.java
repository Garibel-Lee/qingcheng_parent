package com.qingcheng.service.system;

import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.system.Admin;

import java.util.List;
import java.util.Map;

/**
 * 资源业务逻辑层
 */
public interface ResourceService {



    public List<String> findResKeyByLoginName(String  loginName);


}
