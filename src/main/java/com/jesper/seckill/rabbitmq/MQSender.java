package com.jesper.seckill.rabbitmq;

import com.jesper.seckill.redis.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by jiangyunxiong on 2018/5/29.
 * @Service声明这是业务处理层，其bean也是单例
 */
@Service
public class MQSender {

	private static Logger log = LoggerFactory.getLogger(MQSender.class);

	@Autowired
	AmqpTemplate amqpTemplate;

	public void sendTopic(Object message) {
		String msg = RedisService.beanToString(message);
		log.info("send topic message:" + msg);
		amqpTemplate.convertAndSend(MQConfig.TOPIC_EXCHANGE, "topic.key1", msg + "1");
		amqpTemplate.convertAndSend(MQConfig.TOPIC_EXCHANGE, "topic.key2", msg + "2");
	}

	/**
	 * 将message发送到amqp队列去处理
	 * @param message 
	 */
	public void sendSeckillMessage(SeckillMessage message) {
		String msg = RedisService.beanToString(message);
		log.info("send message:" + msg);
		amqpTemplate.convertAndSend(MQConfig.QUEUE, msg);
	}
}
