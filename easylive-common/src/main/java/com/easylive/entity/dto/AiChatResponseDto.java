package com.easylive.entity.dto;

import lombok.Data;

/**
 * AI 聊天响应 DTO
 * 返回给前端的数据结构
 */
@Data
public class AiChatResponseDto {

    /**
     * AI 消息 ID
     */
    private Long messageId;

    /**
     * AI 回复内容
     */
    private String content;

    /**
     * 会话 ID
     */
    private Long chatId;

    /**
     * 会话标题 (如果有更新)
     */
    private String title;

    /**
     * Token 消耗信息 (可选)
     */
    private TokenUsageDto tokenUsage;

    /**
     * Token 使用统计
     */
    @Data
    public static class TokenUsageDto {
        /**
         * 输入 Token 数
         */
        private Integer inputTokens;

        /**
         * 输出 Token 数
         */
        private Integer outputTokens;

        /**
         * 总 Token 数
         */
        private Integer totalTokens;
    }
}