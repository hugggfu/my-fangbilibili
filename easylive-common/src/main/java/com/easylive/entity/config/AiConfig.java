package com.easylive.entity.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 配置类
 * 用于管理通义千问相关配置参数
 */
@Component
@ConfigurationProperties(prefix = "ai")
@Data
public class AiConfig {

    /**
     * 阿里云通义千问 API Key
     * 从阿里云控制台获取
     */
    private String apiKey;

    /**
     * 使用的模型名称
     * 可选值: qwen-turbo, qwen-plus, qwen-max
     * qwen-turbo: 速度快,成本低
     * qwen-plus: 平衡性能和成本
     * qwen-max: 效果最好,成本最高
     */
    private String modelName = "qwen-turbo";

    /**
     * 最大生成 Token 数
     * 控制 AI 回复的最大长度
     */
    private Integer maxTokens = 2000;

    /**
     * 上下文消息数量限制
     * 发送给 AI 的历史消息条数
     * 建议 5-10 条,太多会消耗更多 Token
     */
    private Integer contextLimit = 10;

    /**
     * 用户每日使用次数限制
     * 防止滥用
     */
    private Integer dailyLimit = 50;

    /**
     * AI 调用超时时间 (毫秒)
     * 默认 30 秒
     */
    private Integer timeout = 30000;

    /**
     * 温度参数 (0.0 - 2.0)
     * 控制回复的随机性
     * 0.0: 最确定性的回复
     * 1.0: 平衡
     * 2.0: 最有创造性
     */
    private Float temperature = 0.8f;

    /**
     * 系统提示词
     * 定义 AI 的角色和行为
     */
    private String systemPrompt = "你是 EasyLive 视频平台的智能助手,名字叫「小易」。" +
            "你可以帮助用户推荐视频、优化标题、解答问题。" +
            "请用友好、专业的语气回答用户问题,回答要简洁明了。";
}
