package com.hmdp.listener;

import com.hmdp.config.MQConfig;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

@Slf4j
@Component
public class SeckillListener {
    @Resource
    private IVoucherOrderService voucherOrderService;
    @Resource
    private ISeckillVoucherService iSeckillVoucherService;

    // 监听 seckill.queue 队列
    @RabbitListener(queues = MQConfig.SECKILL_QUEUE)
    public void listenSeckillQueue(VoucherOrder voucherOrder,
                                   Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {

        log.info("从队列中接收到秒杀订单：{}", voucherOrder.getId());

        // 执行最终的数据库下单操作
        try {
            voucherOrderService.createVoucherOrder(voucherOrder);
            channel.basicAck(tag,false);//参数2：是否批量确认
            log.info("订单处理成功 已发送ack 消息已在队列中删除");
        }
        catch (DuplicateKeyException e) {//解决幂等性问题
            log.warn("检测到重复消息，订单已存在，跳过处理并确认消息：{}", voucherOrder.getId());
            channel.basicAck(tag, false);
        }
        catch (RuntimeException e) {//解决库存不足问题
            log.error("库存不足，请处理：{}", voucherOrder.getId());
            channel.basicAck(tag, false);
        }
        catch (Exception e){
            log.error("下单异常，拒绝 ACK，消息将尝试重回队列: {}", e.getMessage());
            channel.basicNack(tag,false,true);//参数2：是否批量 参数3：是否重回队列
        }
    }
}

