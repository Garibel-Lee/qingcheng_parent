package com.qingcheng.consumer;
import com.alibaba.fastjson.JSON;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.exceptions.ClientException;
import com.qingcheng.util.SmsUtil;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
public class SmsMessageConsumer implements MessageListener {

	@Autowired
	private SmsUtil smsUtil;

	@Value("${templateCode_smscode}")
	private String templateCode_smscode;//短信模板编号

	@Value("${templateParam_smscode}")
	private String templateParam_smscode;//短信参数

	public void onMessage(Message message) {
		String jsonString = new String(message.getBody());
		Map<String,String> map = JSON.parseObject(jsonString, Map.class);
		String phone = map.get("phone");
		String code=map.get("code");
		System.out.println("手机号："+phone+"验证码："+code);
		String param=  templateParam_smscode.replace("[value]",code);
		try {
			SendSmsResponse smsResponse = smsUtil.sendSms(phone, templateCode_smscode, param);
		} catch (ClientException e) {
			e.printStackTrace();
		}
	}
}
