package com.easylive.entity.dto;

import lombok.Data;

@Data
public class AiChatResponseDto {
    /**
     * 消息 ID
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
     * 会话标题
     */
    private String title;

    /**
     * 消息类型
     * text: 普通文本消息
     * video_recommend: 视频推荐消息
     */
    private String messageType;

    /**
     * 附加数据 (JSON 格式)
     * 用于存储视频列表等额外信息
     */
    private String extraData;

    /**
     * Token 使用统计 (可选)
     */
    private TokenUsageDto tokenUsage;

    @Data
    public static class TokenUsageDto {
        private Integer inputTokens;
        private Integer outputTokens;
        private Integer totalTokens;
    }
}