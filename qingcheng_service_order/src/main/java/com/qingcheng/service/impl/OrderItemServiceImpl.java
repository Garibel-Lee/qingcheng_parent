package com.qingcheng.service.impl;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.OrderItemMapper;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.order.CategoryReport;
import com.qingcheng.pojo.order.OrderItem;
import com.qingcheng.service.order.OrderItemService;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class OrderItemServiceImpl implements OrderItemService {

    @Autowired
    private OrderItemMapper orderItemMapper;

    /**
     * 返回全部记录
     * @return
     */
    public List<OrderItem> findAll() {
        return orderItemMapper.selectAll();
    }

    /**
     * 分页查询
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<OrderItem> findPage(int page, int size) {
        PageHelper.startPage(page,size);
        Page<OrderItem> orderItems = (Page<OrderItem>) orderItemMapper.selectAll();
        return new PageResult<OrderItem>(orderItems.getTotal(),orderItems.getResult());
    }

    /**
     * 条件查询
     * @param searchMap 查询条件
     * @return
     */
    public List<OrderItem> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return orderItemMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<OrderItem> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page,size);
        Example example = createExample(searchMap);
        Page<OrderItem> orderItems = (Page<OrderItem>) orderItemMapper.selectByExample(example);
        return new PageResult<OrderItem>(orderItems.getTotal(),orderItems.getResult());
    }

    /**
     * 根据Id查询
     * @param id
     * @return
     */
    public OrderItem findById(String id) {
        return orderItemMapper.selectByPrimaryKey(id);
    }

    /**
     * 新增
     * @param orderItem
     */
    public void add(OrderItem orderItem) {
        orderItemMapper.insert(orderItem);
    }

    /**
     * 修改
     * @param orderItem
     */
    public void update(OrderItem orderItem) {
        orderItemMapper.updateByPrimaryKeySelective(orderItem);
    }

    /**
     *  删除
     * @param id
     */
    public void delete(String id) {
        orderItemMapper.deleteByPrimaryKey(id);
    }




    /**
     * 构建查询条件
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap){
        Example example=new Example(OrderItem.class);
        Example.Criteria criteria = example.createCriteria();
        if(searchMap!=null){
            // 标题
            if(searchMap.get("title")!=null && !"".equals(searchMap.get("title"))){
                criteria.andLike("title","%"+searchMap.get("title")+"%");
            }
            // 图片地址
            if(searchMap.get("picPath")!=null && !"".equals(searchMap.get("picPath"))){
                criteria.andLike("picPath","%"+searchMap.get("picPath")+"%");
            }

            // 单价
            if(searchMap.get("price")!=null ){
                criteria.andEqualTo("price",searchMap.get("price"));
            }
            // 数量
            if(searchMap.get("num")!=null ){
                criteria.andEqualTo("num",searchMap.get("num"));
            }
            // 总金额
            if(searchMap.get("money")!=null ){
                criteria.andEqualTo("money",searchMap.get("money"));
            }
            // 重量
            if(searchMap.get("weight")!=null ){
                criteria.andEqualTo("weight",searchMap.get("weight"));
            }

        }
        return example;
    }

}
