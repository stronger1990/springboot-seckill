package com.jesper.seckill.controller;

import com.google.common.util.concurrent.RateLimiter;
import com.jesper.seckill.bean.SeckillOrder;
import com.jesper.seckill.bean.User;
import com.jesper.seckill.rabbitmq.MQSender;
import com.jesper.seckill.rabbitmq.SeckillMessage;
import com.jesper.seckill.redis.GoodsKey;
import com.jesper.seckill.redis.RedisService;
import com.jesper.seckill.result.CodeMsg;
import com.jesper.seckill.result.Result;
import com.jesper.seckill.service.GoodsService;
import com.jesper.seckill.service.OrderService;
import com.jesper.seckill.service.SeckillService;
import com.jesper.seckill.vo.GoodsVo;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 实现InitializingBean的好处是，当该类被初始化的时候，将会执行afterPropertiesSet()
 */
@Controller
@RequestMapping("/seckill")
public class SeckillController implements InitializingBean {

	@Autowired
	GoodsService goodsService;

	@Autowired
	OrderService orderService;

	@Autowired
	SeckillService seckillService;

	@Autowired
	RedisService redisService;

	@Autowired
	MQSender sender;

	// 基于令牌桶算法的限流实现类
	RateLimiter rateLimiter = RateLimiter.create(10);

	// 做标记，判断该商品是否被秒杀过了
	private HashMap<Long, Boolean> localOverMap = new HashMap<Long, Boolean>();
	
	/**
	 * 系统初始化,将商品信息加载到redis和本地内存
	 */
	@Override
	public void afterPropertiesSet() {
		List<GoodsVo> goodsVoList = goodsService.listGoodsVo();
		if (goodsVoList == null) {
			return;
		}
		for (GoodsVo goods : goodsVoList) {
			redisService.set(GoodsKey.getGoodsStock, "" + goods.getId(), goods.getStockCount());
			// 初始化商品都是没有处理过的
			localOverMap.put(goods.getId(), false);
		}
	}

	/**
	 * GET POST 1、GET幂等,服务端获取数据，无论调用多少次结果都一样 2、POST，向服务端提交数据，不是幂等
	 * <p>
	 * 将同步下单改为异步下单
	 *
	 * @param model
	 * @param user
	 * @param goodsId
	 * @return
	 */
	@RequestMapping(value = "/do_seckill", method = RequestMethod.POST)
	@ResponseBody
	public Result<Integer> list(Model model, User user, @RequestParam("goodsId") long goodsId) {

		if (!rateLimiter.tryAcquire(1000, TimeUnit.MILLISECONDS)) {
			return Result.error(CodeMsg.ACCESS_LIMIT_REACHED);
		}

		if (user == null) {
			return Result.error(CodeMsg.SESSION_ERROR);
		}
		model.addAttribute("user", user);
		// 内存标记，减少redis访问
		boolean over = localOverMap.get(goodsId);
		if (over) {
			return Result.error(CodeMsg.SECKILL_OVER);
		}
		// 获取库存，减1，得到新的库存数
		long stock = redisService.decr(GoodsKey.getGoodsStock, "" + goodsId);// 10
		if (stock < 0) {
			// 库存没了？可能数据有问题，刷新一下再试
			afterPropertiesSet();
			long stock2 = redisService.decr(GoodsKey.getGoodsStock, "" + goodsId);// 10
			if (stock2 < 0) {
				// 库存确实为0了。不然也不会减1小于0，赶紧设置该商品已经被秒杀了
				localOverMap.put(goodsId, true);
				return Result.error(CodeMsg.SECKILL_OVER);
			}
		}
		// 判断重复秒杀，同一个用户只能秒杀一次
		SeckillOrder order = orderService.getOrderByUserIdGoodsId(user.getId(), goodsId);
		if (order != null) {
			return Result.error(CodeMsg.REPEATE_SECKILL);
		}
		// 入队，先放在mrabbitmq先进先出队列等待处理，不要秒杀成功马上就进行下一步操作，因为秒杀才是最重要的，秒杀完成后，剩下就是订单的刷新和配送等，这个不需要实时处理，放在队列慢慢处理就可以了。
		// 不要堵塞，赶紧告诉客户端秒杀成功了。
		SeckillMessage message = new SeckillMessage();
		message.setUser(user);
		message.setGoodsId(goodsId);
		// com.jesper.seckill.rabbitmq.MQReceiver将接受处理，最终会调OrderService的createOrder创建订单
		sender.sendSeckillMessage(message);
		return Result.success(0);// 排队中
	}

	/**
	 * orderId：成功 -1：秒杀失败 0： 排队中
	 */
	@RequestMapping(value = "/result", method = RequestMethod.GET)
	@ResponseBody
	public Result<Long> seckillResult(Model model, User user, @RequestParam("goodsId") long goodsId) {
		model.addAttribute("user", user);
		if (user == null) {
			return Result.error(CodeMsg.SESSION_ERROR);
		}
		long orderId = seckillService.getSeckillResult(user.getId(), goodsId);
		return Result.success(orderId);
	}
}
