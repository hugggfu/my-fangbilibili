package com.easylive.component;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.easylive.entity.config.AiConfig;
import com.easylive.entity.dto.AiMessageDto;
import com.easylive.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * AI 组件
 * 封装与阿里云通义千问的交互逻辑
 */
@Component
@Slf4j
public class AiComponent {

    @Resource
    private AiConfig aiConfig;

    /**
     * 调用通义千问生成回复
     *
     * @param messages 消息列表 (包含历史上下文)
     * @return AI 回复内容
     */
    public String chat(List<AiMessageDto> messages) {
        try {
            // 转换消息格式
            List<Message> qwenMessages = convertMessages(messages);

            // 构建请求参数
            GenerationParam param = GenerationParam.builder()
                    .apiKey(aiConfig.getApiKey())
                    .model(aiConfig.getModelName())
                    .messages(qwenMessages)
                    .maxTokens(aiConfig.getMaxTokens())
                    .temperature(aiConfig.getTemperature())
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .build();

            // 创建 Generation 实例
            Generation generation = new Generation();

            // 使用 CompletableFuture 实现超时控制
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    GenerationResult result = generation.call(param);

                    // 提取回复内容
                    String response = result.getOutput().getChoices().get(0).getMessage().getContent();

                    // 记录 Token 使用情况
                    log.info("AI 调用成功 - 输入 Token: {}, 输出 Token: {}, 总计: {}",
                            result.getUsage().getInputTokens(),
                            result.getUsage().getOutputTokens(),
                            result.getUsage().getTotalTokens());

                    return response;
                } catch (ApiException | NoApiKeyException | InputRequiredException e) {
                    log.error("通义千问 API 调用失败", e);
                    throw new RuntimeException(e);
                }
            });

            // 等待结果,设置超时时间
            return future.get(aiConfig.getTimeout(), TimeUnit.MILLISECONDS);

        } catch (TimeoutException e) {
            log.error("AI 调用超时", e);
            throw new BusinessException("AI 正在思考人生,请稍后再试~");
        } catch (Exception e) {
            log.error("AI 调用异常", e);
            throw new BusinessException("AI 服务暂时不可用,请稍后重试");
        }
    }

    /**
     * 生成会话标题
     * 根据用户的第一条消息生成简短的标题
     *
     * @param firstMessage 第一条消息
     * @return 生成的标题
     */
    public String generateTitle(String firstMessage) {
        try {
            // 构建标题生成提示词
            List<AiMessageDto> messages = new ArrayList<>();
            messages.add(AiMessageDto.builder()
                    .role("system")
                    .content("你是一个标题生成助手。请根据用户的问题,生成一个简短的标题(不超过15个字),只返回标题内容,不要有其他说明。")
                    .build());
            messages.add(AiMessageDto.builder()
                    .role("user")
                    .content(firstMessage)
                    .build());

            // 调用 AI 生成标题
            String title = chat(messages);

            // 清理标题 (去除引号、换行等)
            title = title.replaceAll("[\"'「」『』]", "").trim();

            // 限制长度
            if (title.length() > 20) {
                title = title.substring(0, 20);
            }

            return title;
        } catch (Exception e) {
            log.error("生成标题失败,使用默认标题", e);
            // 如果生成失败,截取用户消息前 15 个字作为标题
            return firstMessage.length() > 15
                    ? firstMessage.substring(0, 15) + "..."
                    : firstMessage;
        }
    }

    /**
     * 转换消息格式
     * 将我们的 DTO 转换为通义千问 SDK 的 Message 格式
     *
     * @param messages 我们的消息列表
     * @return 通义千问的消息列表
     */
    private List<Message> convertMessages(List<AiMessageDto> messages) {
        List<Message> qwenMessages = new ArrayList<>();

        for (AiMessageDto msg : messages) {
            Role role;
            switch (msg.getRole()) {
                case "system":
                    role = Role.SYSTEM;
                    break;
                case "user":
                    role = Role.USER;
                    break;
                case "assistant":
                    role = Role.ASSISTANT;
                    break;
                default:
                    log.warn("未知的角色类型: {}, 默认使用 USER", msg.getRole());
                    role = Role.USER;
            }

            qwenMessages.add(Message.builder()
                    .role(role.getValue())
                    .content(msg.getContent())
                    .build());
        }

        return qwenMessages;
    }

    /**
     * 测试 API 连接
     * 用于验证 API Key 是否有效
     *
     * @return 是否连接成功
     */
    public boolean testConnection() {
        try {
            List<AiMessageDto> testMessages = new ArrayList<>();
            testMessages.add(AiMessageDto.builder()
                    .role("user")
                    .content("你好")
                    .build());

            String response = chat(testMessages);
            log.info("API 连接测试成功,响应: {}", response);
            return true;
        } catch (Exception e) {
            log.error("API 连接测试失败", e);
            return false;
        }
    }
}