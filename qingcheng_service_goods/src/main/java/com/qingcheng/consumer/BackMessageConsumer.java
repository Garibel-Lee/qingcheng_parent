package com.qingcheng.consumer;
import com.alibaba.fastjson.JSON;
import com.qingcheng.pojo.order.OrderItem;
import com.qingcheng.service.goods.StockBackService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

public class BackMessageConsumer implements MessageListener {

	@Autowired
	private StockBackService stockBackService;

	public void onMessage(Message message) {

		try {
			String jsonString = new String(message.getBody());
			List<OrderItem> orderItems = JSON.parseArray(jsonString, OrderItem.class);
			stockBackService.addList(orderItems);
		} catch (Exception e) {
			e.printStackTrace();
			//记录日志
		}
	}

}
