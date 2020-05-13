package com.qingcheng.service.impl;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.OrderConfigMapper;
import com.qingcheng.dao.OrderItemMapper;
import com.qingcheng.dao.OrderLogMapper;
import com.qingcheng.dao.OrderMapper;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.pojo.order.OrderConfig;
import com.qingcheng.pojo.order.OrderItem;
import com.qingcheng.pojo.order.OrderLog;
import com.qingcheng.service.goods.SkuService;
import com.qingcheng.service.order.CartService;
import com.qingcheng.service.order.OrderService;
import com.qingcheng.util.IdWorker;
import jdk.nashorn.internal.runtime.options.LoggingOption;
import org.apache.commons.logging.LogFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service(interfaceClass = OrderService.class)
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);


    /**
     * 返回全部记录
     * @return
     */
    public List<Order> findAll() {
        return orderMapper.selectAll();
    }

    /**
     * 分页查询
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<Order> findPage(int page, int size) {
        PageHelper.startPage(page,size);
        Page<Order> orders = (Page<Order>) orderMapper.selectAll();
        return new PageResult<Order>(orders.getTotal(),orders.getResult());
    }

    /**
     * 条件查询
     * @param searchMap 查询条件
     * @return
     */
    public List<Order> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return orderMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<Order> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page,size);
        Example example = createExample(searchMap);
        Page<Order> orders = (Page<Order>) orderMapper.selectByExample(example);
        return new PageResult<Order>(orders.getTotal(),orders.getResult());
    }

    /**
     * 根据Id查询
     * @param id
     * @return
     */
    public Order findById(String id) {
        return orderMapper.selectByPrimaryKey(id);
    }

    @Autowired
    private CartService cartService;


    @Autowired
    private IdWorker idWorker;

    @Autowired
    private OrderItemMapper orderItemMapper;


    @Reference
    private SkuService skuService;

    @Reference
    private RabbitTemplate rabbitTemplate;


    /**
     * 新增
     * @param order
     */
    @Transactional
    public Map<String, Object> add(Order order) {
        //获取购物车(刷新单价)
        List<Map<String, Object>> oderItemList = cartService.findNewOrderItemList(order.getUsername());
        //获取选中的购物车
        List<OrderItem> orderItems = oderItemList.stream()
                .filter(cart -> (boolean) cart.get("checked"))
                .map(cart -> (OrderItem) cart.get("item"))
                .collect(Collectors.toList());
        //合计数计算
        IntStream numStream = orderItems.stream().mapToInt(OrderItem::getNum);
        IntStream moneyStream = orderItems.stream().mapToInt(OrderItem::getMoney);
        int totalNum=numStream.sum();//总数量
        int totalMoney=moneyStream.sum();//订单总金额
        int preferential = cartService.preferential(order.getUsername());  //计算优惠金额
        //扣减库存
        boolean b = skuService.deductionStock(orderItems);
        if(b==false){
            throw new RuntimeException("库存扣减失败");
        }
        try{
            //保存订单主表
            order.setId(idWorker.nextId()+"");
            order.setTotalNum(totalNum);//总数量
            order.setTotalMoney(totalMoney);//总金额
            order.setPreMoney(preferential);//优惠金额
            order.setPayMoney(totalMoney-preferential);//支付金额=总金额+优惠金额
            order.setCreateTime(new Date());//订单创建日期
            order.setOrderStatus("0"); // 订单状态
            order.setPayStatus("0"); // 支付状态：未支付
            order.setConsignStatus("0");   //发货状态：未发货
            orderMapper.insert(order);
            double proportion = (double)order.getPayMoney()/totalMoney;
            //保存订单明细
            for(OrderItem orderItem:orderItems){
                orderItem.setOrderId(order.getId());//订单主表ID
                orderItem.setId(idWorker.nextId()+"");
                orderItem.setPayMoney( (int)(orderItem.getMoney()* proportion) );//支付金额
                orderItemMapper.insert(orderItem);
            }

        }catch (Exception ex){
            for(OrderItem orderItem:orderItems){
                orderItem.setOrderId(order.getId());//订单主表ID
            }
            rabbitTemplate.convertAndSend("exchange.skuback","skuback", JSON.toJSONString(orderItems));
            logger.error("订单生成失败，订单号："+order.getId()+"用户名："+order.getUsername()+"购物车列表："+ JSON.toJSONString(orderItems));
            throw new RuntimeException("订单生成失败"); //抛出异常，让其回滚！
        }
        //异步清除购物车
        try{
            rabbitTemplate.convertAndSend("exchange.delete_cart","delete_cart",order.getUsername());
        }catch (Exception ex){
            //记录loginfo日志
            logger.error("购物车清除失败，订单号："+order.getId()+"用户名："+order.getUsername()+"购物车列表："+ JSON.toJSONString(orderItems));
        }
        //cartService.deleteCheckedCart(order.getUsername());//清除选中的购物车
        //返回订单号和支付的金额
        Map<String, Object> map=new HashMap();
        map.put("ordersn", order.getId() );
        map.put("money",order.getPayMoney());

        return map;
    }



    /**
     * 修改
     * @param order
     */
    public void update(Order order) {
        orderMapper.updateByPrimaryKeySelective(order);
    }

    /**
     *  删除
     * @param id
     */
    public void delete(String id) {
        orderMapper.deleteByPrimaryKey(id);
    }


    /**
     * 批量发货
     * @param orders
     */
    public void batchSend(List<Order> orders) {
        //判断运单号和物流公司是否为空
        for(Order order :orders){
            if(order.getShippingCode()==null || order.getShippingName()==null){
                throw new RuntimeException("请选择快递公司和填写快递单号");
            }
        }
        //循环订单
        for(Order order :orders){
            order.setOrderStatus("3");//订单状态  已发货
            order.setConsignStatus("2"); //发货状态  已发货
            order.setConsignTime(new Date());//发货时间
            orderMapper.updateByPrimaryKeySelective(order);

            saveOrderLog(order,"admin");
        }
    }

    /**
     *  存储订单日志
     * @param order
     * @param operater
     */
    private void saveOrderLog(Order order,String operater){
        //记录订单变动日志
        OrderLog orderLog=new OrderLog();
        orderLog.setOperater( operater );// 管理员
        orderLog.setOperateTime(new Date());//当前日期
        orderLog.setOrderStatus(order.getOrderStatus());
        orderLog.setPayStatus(order.getPayStatus());
        orderLog.setConsignStatus(order.getConsignStatus());
        orderLog.setRemarks("超时订单，系统自动关闭");
        orderLog.setOrderId(order.getId());
        orderLogMapper.insert(orderLog);
    }


    @Autowired
    private OrderConfigMapper orderConfigMapper;  //订单设置

    @Autowired
    private OrderLogMapper orderLogMapper;

    /**
     * 订单超时处理
     */
    public void orderTimeOutLogic() {
        System.out.println("订单超时自动关闭");
        //订单超时未付款 自动关闭
        //查询超时时间
        OrderConfig orderConfig = orderConfigMapper.selectByPrimaryKey(1);
        Integer orderTimeout = orderConfig.getOrderTimeout();  //超时时间（分） 60
        LocalDateTime localDateTime = LocalDateTime.now().minusMinutes(orderTimeout);  //得到超时的时间点

        //设置查询条件
        Example example=new Example(Order.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andLessThan("createTime",localDateTime);//创建时间小于超时时间
        criteria.andEqualTo("orderStatus","0");//未付款的
        criteria.andEqualTo("isDelete","0");//未删除的

        //查询超时订单
        List<Order> orders = orderMapper.selectByExample(example);
        for(Order order :orders){
            //记录订单变动日志
            OrderLog orderLog=new OrderLog();
            orderLog.setOperater("system");// 系统
            orderLog.setOperateTime(new Date());//当前日期
            orderLog.setOrderStatus("4");
            orderLog.setPayStatus(order.getPayStatus());
            orderLog.setConsignStatus(order.getConsignStatus());
            orderLog.setRemarks("超时订单，系统自动关闭");
            orderLog.setOrderId(order.getId());
            orderLogMapper.insert(orderLog);
            //更改订单状态
            order.setOrderStatus("4");
            order.setCloseTime(new Date());//关闭日期
            orderMapper.updateByPrimaryKeySelective(order);
        }
    }

    @Override
    @Transactional
    public void updatePayStatus(String orderId, String transactionId) {
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if(order!=null  && "0".equals(order.getPayStatus())){  //存在订单且状态为0
            order.setPayStatus("1");
            order.setOrderStatus("1");
            order.setUpdateTime(new Date());
            order.setPayTime(new Date());
            order.setTransactionId(transactionId);//微信返回的交易流水号
            orderMapper.updateByPrimaryKeySelective(order);
            //记录订单变动日志
            OrderLog orderLog=new OrderLog();
            orderLog.setOperater("system");// 系统
            orderLog.setOperateTime(new Date());//当前日期
            orderLog.setOrderStatus("1");
            orderLog.setPayStatus("1");
            orderLog.setRemarks("支付流水号"+transactionId);
            orderLog.setOrderId(order.getId());
            orderLogMapper.insert(orderLog);
        }
    }


    /**
     * 构建查询条件
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap){
        Example example=new Example(Order.class);
        Example.Criteria criteria = example.createCriteria();
        if(searchMap!=null){
            // 支付类型，1、微信支付，2、支付宝 3、银联支付 、4 货到付款
            if(searchMap.get("payType")!=null && !"".equals(searchMap.get("payType"))){
                criteria.andLike("payType","%"+searchMap.get("payType")+"%");
            }
            // 物流名称
            if(searchMap.get("shippingName")!=null && !"".equals(searchMap.get("shippingName"))){
                criteria.andLike("shippingName","%"+searchMap.get("shippingName")+"%");
            }
            // 物流单号
            if(searchMap.get("shippingCode")!=null && !"".equals(searchMap.get("shippingCode"))){
                criteria.andLike("shippingCode","%"+searchMap.get("shippingCode")+"%");
            }
            // 用户名称
            if(searchMap.get("userName")!=null && !"".equals(searchMap.get("userName"))){
                criteria.andLike("userName","%"+searchMap.get("userName")+"%");
            }
            // 买家留言
            if(searchMap.get("buyerMessage")!=null && !"".equals(searchMap.get("buyerMessage"))){
                criteria.andLike("buyerMessage","%"+searchMap.get("buyerMessage")+"%");
            }
            // 是否评价
            if(searchMap.get("buyerRate")!=null && !"".equals(searchMap.get("buyerRate"))){
                criteria.andLike("buyerRate","%"+searchMap.get("buyerRate")+"%");
            }
            // 收货人
            if(searchMap.get("receiverContact")!=null && !"".equals(searchMap.get("receiverContact"))){
                criteria.andLike("receiverContact","%"+searchMap.get("receiverContact")+"%");
            }
            // 收货人手机
            if(searchMap.get("receiverMobile")!=null && !"".equals(searchMap.get("receiverMobile"))){
                criteria.andLike("receiverMobile","%"+searchMap.get("receiverMobile")+"%");
            }
            // 收货人地址
            if(searchMap.get("receiverAddress")!=null && !"".equals(searchMap.get("receiverAddress"))){
                criteria.andLike("receiverAddress","%"+searchMap.get("receiverAddress")+"%");
            }
            // 订单来源：1:web，2：app，3：微信公众号，4：微信小程序  5 H5手机页面
            if(searchMap.get("sourceType")!=null && !"".equals(searchMap.get("sourceType"))){
                criteria.andLike("sourceType","%"+searchMap.get("sourceType")+"%");
            }
            // 交易流水号
            if(searchMap.get("transactionId")!=null && !"".equals(searchMap.get("transactionId"))){
                criteria.andLike("transactionId","%"+searchMap.get("transactionId")+"%");
            }
            // 订单状态
            if(searchMap.get("orderStatus")!=null && !"".equals(searchMap.get("orderStatus"))){
                criteria.andLike("orderStatus","%"+searchMap.get("orderStatus")+"%");
            }
            // 支付状态
            if(searchMap.get("payStatus")!=null && !"".equals(searchMap.get("payStatus"))){
                criteria.andLike("payStatus","%"+searchMap.get("payStatus")+"%");
            }
            // 发货状态
            if(searchMap.get("sendStatus")!=null && !"".equals(searchMap.get("sendStatus"))){
                criteria.andLike("sendStatus","%"+searchMap.get("sendStatus")+"%");
            }
            // 退货状态
            if(searchMap.get("returnStatus")!=null && !"".equals(searchMap.get("returnStatus"))){
                criteria.andLike("returnStatus","%"+searchMap.get("returnStatus")+"%");
            }
            // 退款状态
            if(searchMap.get("refundStauts")!=null && !"".equals(searchMap.get("refundStauts"))){
                criteria.andLike("refundStauts","%"+searchMap.get("refundStauts")+"%");
            }
            // 是否删除
            if(searchMap.get("isDelete")!=null && !"".equals(searchMap.get("isDelete"))){
                criteria.andLike("isDelete","%"+searchMap.get("isDelete")+"%");
            }

            // 数量合计
            if(searchMap.get("totalNum")!=null ){
                criteria.andEqualTo("totalNum",searchMap.get("totalNum"));
            }
            // 金额合计
            if(searchMap.get("totalMoney")!=null ){
                criteria.andEqualTo("totalMoney",searchMap.get("totalMoney"));
            }
            // 优惠金额
            if(searchMap.get("preMoney")!=null ){
                criteria.andEqualTo("preMoney",searchMap.get("preMoney"));
            }
            // 邮费
            if(searchMap.get("postFee")!=null ){
                criteria.andEqualTo("postFee",searchMap.get("postFee"));
            }
            // 实付金额
            if(searchMap.get("payMoney")!=null ){
                criteria.andEqualTo("payMoney",searchMap.get("payMoney"));
            }
            // 根据  id 数组查询 查询
            if(searchMap.get("ids")!=null   ){
                criteria.andIn("id", Arrays.asList((Integer[])searchMap.get("ids")));
            }
        }
        return example;
    }

}
