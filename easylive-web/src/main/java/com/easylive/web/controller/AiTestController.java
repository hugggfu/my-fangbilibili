package com.easylive.web.controller;

import com.easylive.component.AiComponent;
import com.easylive.entity.dto.AiMessageDto;
import com.easylive.entity.vo.ResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 测试控制器
 * 用于测试 AI 组件功能
 * 测试完成后可以删除此文件
 */
@RestController
@RequestMapping("/test/ai")
@Slf4j
public class AiTestController extends ABaseController {

    @Resource
    private AiComponent aiComponent;

    /**
     * 测试 API 连接
     * 访问: http://localhost:端口/test/ai/connection
     */
    @RequestMapping("/connection")
    public ResponseVO testConnection() {
        try {
            boolean success = aiComponent.testConnection();
            return getSuccessResponseVO(success ? "API 连接成功 ✓" : "API 连接失败 ✗");
        } catch (Exception e) {
            log.error("API 连接测试失败", e);
            return getBusinessErrorResponseVO(null, "连接失败: " + e.getMessage());
        }
    }

    /**
     * 测试简单对话
     * 访问: http://localhost:端口/test/ai/chat
     */
    @RequestMapping("/chat")
    public ResponseVO testChat() {
        try {
            List<AiMessageDto> messages = new ArrayList<>();
            messages.add(AiMessageDto.builder()
                    .role("user")
                    .content("你好,请介绍一下你自己")
                    .build());

            String response = aiComponent.chat(messages);
            return getSuccessResponseVO(response);
        } catch (Exception e) {
            log.error("对话测试失败", e);
            return getBusinessErrorResponseVO(null, "对话失败: " + e.getMessage());
        }
    }

    /**
     * 测试标题生成
     * 访问: http://localhost:端口/test/ai/title
     */
    @RequestMapping("/title")
    public ResponseVO testGenerateTitle() {
        try {
            String title = aiComponent.generateTitle("我想学习如何制作有趣的短视频");
            return getSuccessResponseVO(title);
        } catch (Exception e) {
            log.error("标题生成测试失败", e);
            return getBusinessErrorResponseVO(null, "生成失败: " + e.getMessage());
        }
    }

    /**
     * 测试多轮对话
     * 访问: http://localhost:端口/test/ai/multiTurn
     */
    @RequestMapping("/multiTurn")
    public ResponseVO testMultiTurnChat() {
        try {
            List<AiMessageDto> messages = new ArrayList<>();

            // 添加系统提示词
            messages.add(AiMessageDto.builder()
                    .role("system")
                    .content("你是 EasyLive 视频平台的智能助手")
                    .build());

            // 第一轮对话
            messages.add(AiMessageDto.builder()
                    .role("user")
                    .content("我想学习视频剪辑")
                    .build());
            messages.add(AiMessageDto.builder()
                    .role("assistant")
                    .content("很好!视频剪辑是一项很有用的技能。你想学习哪方面的剪辑呢?")
                    .build());

            // 第二轮对话
            messages.add(AiMessageDto.builder()
                    .role("user")
                    .content("我想学习如何添加字幕")
                    .build());

            String response = aiComponent.chat(messages);
            return getSuccessResponseVO(response);
        } catch (Exception e) {
            log.error("多轮对话测试失败", e);
            return getBusinessErrorResponseVO(null, "对话失败: " + e.getMessage());
        }
    }
}