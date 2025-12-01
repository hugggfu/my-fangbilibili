package com.easylive.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 消息 DTO
 * 用于与通义千问 API 交互
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiMessageDto {

    /**
     * 消息角色
     * system: 系统提示词
     * user: 用户消息
     * assistant: AI 回复
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;
}