package com.qingcheng.dao;

import com.qingcheng.pojo.goods.Sku;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

public interface SkuMapper extends Mapper<Sku> {


    /**
     * 更改库存数
     * @param id
     * @param changeNum
     * @return
     */
    @Select("update tb_sku set num=num+ #{changeNum} where id=#{id}")
    public void updateNum(@Param("id") String id, @Param("changeNum") Integer changeNum);

}
