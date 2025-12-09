package com.easylive.entity.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 * 定义交换机、队列、绑定关系
 */
@Configuration
public class RabbitMQConfig {

    // ===== 添加这个构造函数 =====
    public RabbitMQConfig() {
        System.out.println("\n========================================");
        System.out.println("🚀 RabbitMQConfig 配置类已加载!");
        System.out.println("========================================\n");
    }

    // ==================== 常量定义 ====================

    /**
     * 弹幕交换机名称
     * Exchange: 消息的路由中心,决定消息发送到哪个队列
     */
    public static final String DANMU_EXCHANGE = "easylive.danmu.exchange";

    /**
     * 弹幕队列名称
     * Queue: 存储消息的容器,消费者从这里拉取消息
     */
    public static final String DANMU_QUEUE = "easylive.danmu.queue";

    /**
     * 弹幕路由键
     * RoutingKey: Exchange根据这个key决定消息路由到哪个Queue
     */
    public static final String DANMU_ROUTING_KEY = "danmu.post";

    /**
     * 弹幕死信交换机
     * DLX (Dead Letter Exchange): 处理失败消息的交换机
     */
    public static final String DANMU_DLX_EXCHANGE = "easylive.danmu.dlx.exchange";

    /**
     * 弹幕死信队列
     * DLQ (Dead Letter Queue): 存储处理失败的消息
     */
    public static final String DANMU_DLX_QUEUE = "easylive.danmu.dlx.queue";

    /**
     * 弹幕死信路由键
     */
    public static final String DANMU_DLX_ROUTING_KEY = "danmu.dlx";

    // ==================== 基础配置 ====================

    /**
     * 消息转换器 - 使用JSON格式
     * 将Java对象转换为JSON字符串进行传输
     */
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate配置
     * RabbitTemplate: 发送消息的工具类
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());

        // 发送确认回调 - 消息是否到达Exchange
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                System.err.println("❌ 消息发送到Exchange失败: " + cause);
            } else {
                System.out.println("✅ 消息成功发送到Exchange");
            }
        });

        // 发送失败回调 - 消息是否到达Queue
        template.setReturnsCallback(returned -> {
            System.err.println("❌ 消息未路由到Queue: " + returned.getMessage());
        });

        return template;
    }

    /**
     * 监听器容器工厂
     * 用于消费者监听队列
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        return factory;
    }

    // ==================== 弹幕队列配置 ====================

    /**
     * 创建弹幕交换机
     * DirectExchange: 直连交换机,根据RoutingKey精确匹配
     */
    @Bean
    public DirectExchange danmuExchange() {
        // 参数: 名称, 是否持久化, 是否自动删除
        return new DirectExchange(DANMU_EXCHANGE, true, false);
    }

    /**
     * 创建弹幕队列
     * 配置死信队列参数
     */
    @Bean
    public Queue danmuQueue() {

        System.out.println("📦 正在创建弹幕队列: " + DANMU_QUEUE);
        return QueueBuilder.durable(DANMU_QUEUE) // 持久化队列
                // 消息处理失败后,发送到死信交换机
                .withArgument("x-dead-letter-exchange", DANMU_DLX_EXCHANGE)
                // 死信消息的路由键
                .withArgument("x-dead-letter-routing-key", DANMU_DLX_ROUTING_KEY)
                .build();

    }

    /**
     * 绑定弹幕队列到交换机
     * Binding: 定义Exchange和Queue的绑定关系
     */
    @Bean
    public Binding danmuBinding() {
        return BindingBuilder
                .bind(danmuQueue()) // 绑定队列
                .to(danmuExchange()) // 到交换机
                .with(DANMU_ROUTING_KEY); // 使用路由键
    }

    // ==================== 死信队列配置 ====================

    /**
     * 创建死信交换机
     */
    @Bean
    public DirectExchange danmuDlxExchange() {
        return new DirectExchange(DANMU_DLX_EXCHANGE, true, false);
    }

    /**
     * 创建死信队列
     */
    @Bean
    public Queue danmuDlxQueue() {
        return QueueBuilder.durable(DANMU_DLX_QUEUE).build();
    }

    /**
     * 绑定死信队列到死信交换机
     */
    @Bean
    public Binding danmuDlxBinding() {
        return BindingBuilder
                .bind(danmuDlxQueue())
                .to(danmuDlxExchange())
                .with(DANMU_DLX_ROUTING_KEY);
    }

    // ==================== 点赞/收藏 配置 ====================

    public static final String ACTION_EXCHANGE = "easylive.action.exchange";
    public static final String ACTION_QUEUE = "easylive.action.queue";
    public static final String ACTION_ROUTING_KEY = "action.post";

    // 死信队列可以复用现有的，或者新建，这里为了简化先省略
    @Bean
    public DirectExchange actionExchange() {
        return new DirectExchange(ACTION_EXCHANGE, true, false);
    }
    @Bean
    public Queue actionQueue() {
        return QueueBuilder.durable(ACTION_QUEUE).build();
    }
    @Bean
    public Binding actionBinding() {
        return BindingBuilder.bind(actionQueue()).to(actionExchange()).with(ACTION_ROUTING_KEY);
    }
}
