package com.qingcheng.consumer;
import com.qingcheng.service.order.CartService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
public class CartMessageConsumer implements MessageListener {

	@Autowired
	private CartService cartService;

	public void onMessage(Message message) {
		try {
			String username = new String(message.getBody());
			cartService.deleteCheckedCart(username);
		} catch (Exception e) {
			e.printStackTrace();
			//记录错误日志
		}
	}

}
