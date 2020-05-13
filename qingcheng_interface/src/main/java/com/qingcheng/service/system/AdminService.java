package com.qingcheng.service.system;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.system.Admin;

import java.util.*;

/**
 * admin业务逻辑层
 */
public interface AdminService {


    public List<Admin> findAll();


    public PageResult<Admin> findPage(int page, int size);


    public List<Admin> findList(Map<String, Object> searchMap);


    public PageResult<Admin> findPage(Map<String, Object> searchMap, int page, int size);


    public Admin findById(Integer id);

    public void add(Admin admin);


    public void update(Admin admin);


    public void delete(Integer id);

    /**
     * 保存管理员角色
     * @param adminId
     * @param roleIds
     */
    public void saveAdminRoles(Integer adminId,Integer [] roleIds);

}
