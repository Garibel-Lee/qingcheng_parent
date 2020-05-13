package com.qingcheng.consumer;
import com.alibaba.fastjson.JSON;
import com.qingcheng.service.order.CartService;
import com.qingcheng.service.order.OrderService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

public class PayMessageConsumer implements MessageListener {

	@Autowired
	private OrderService orderService;

	public void onMessage(Message message) {
		try {
			String json = new String(message.getBody());
			Map map = JSON.parseObject(json, Map.class);


		} catch (Exception e) {
			e.printStackTrace();
			//记录错误日志
		}
	}

}
