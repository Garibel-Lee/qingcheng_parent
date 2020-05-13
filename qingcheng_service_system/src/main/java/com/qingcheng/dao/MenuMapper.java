package com.qingcheng.dao;

import com.qingcheng.pojo.system.Menu;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface MenuMapper extends Mapper<Menu> {


    @Select("SELECT * FROM tb_menu WHERE id IN ( SELECT menu_id FROM tb_role_menu WHERE role_id IN ( SELECT role_id FROM tb_admin_role WHERE admin_id IN  (SELECT id FROM tb_admin WHERE login_name=#{loginName} ) ) )")
    public List<Menu> findMenuListByLoginName(@Param("loginName") String  loginName);

}
