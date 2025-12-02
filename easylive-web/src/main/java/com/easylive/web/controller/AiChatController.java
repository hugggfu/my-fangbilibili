package com.easylive.web.controller;

import com.easylive.component.RedisComponent;
import com.easylive.entity.dto.AiChatResponseDto;
import com.easylive.entity.po.AiChatMessage;
import com.easylive.entity.po.AiChatSession;
import com.easylive.entity.query.AiChatMessageQuery;
import com.easylive.entity.query.AiChatSessionQuery;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.exception.BusinessException;
import com.easylive.service.AiChatMessageService;
import com.easylive.service.AiChatSessionService;
import com.easylive.web.annotation.GlobalInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import java.util.Date;
import java.util.List;

/**
 * AI 聊天控制器
 */
@RestController
@RequestMapping("/ai/chat")
@Slf4j
public class AiChatController extends ABaseController {

    @Resource
    private AiChatSessionService aiChatSessionService;

    @Resource
    private AiChatMessageService aiChatMessageService;

    @Resource
    private RedisComponent redisComponent;

    /**
     * 获取会话列表
     * 前端调用: GET /ai/chat/list
     *
     * @return 会话列表
     */
    @RequestMapping("/list")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO getChatList() {
        // 获取当前登录用户 ID
        String userId = getTokenUserInfoDto().getUserId();

        // 构建查询条件
        AiChatSessionQuery query = new AiChatSessionQuery();
        query.setUserId(userId);           // 查询指定用户的会话
        query.setIsDeleted(0);             // 只查询未删除的会话
        query.setOrderBy("update_time desc"); // 按更新时间倒序排列

        // 调用已有的 Service 方法查询
        List<AiChatSession> sessions = aiChatSessionService.findListByParam(query);

        log.info("用户 {} 查询会话列表,共 {} 个会话", userId, sessions.size());

        return getSuccessResponseVO(sessions);
    }

    /**
     * 创建新对话
     * 前端调用: POST /ai/chat/create
     *
     * @return 新创建的会话信息
     */
    @RequestMapping("/create")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO createChat() {
        // 获取当前登录用户 ID
        String userId = getTokenUserInfoDto().getUserId();

        // 创建新会话对象
        AiChatSession session = new AiChatSession();
        session.setUserId(userId);
        session.setTitle("新对话"); // 默认标题
        session.setCreateTime(new Date());
        session.setUpdateTime(new Date());
        session.setIsDeleted(0);

        // 调用已有的 Service 方法插入数据库
        aiChatSessionService.add(session);

        log.info("用户 {} 创建新会话,会话 ID: {}", userId, session.getSessionId());

        return getSuccessResponseVO(session);
    }

    /**
     * 删除对话
     */
    @RequestMapping("/delete")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO deleteChat(Long chatId) {
        try {
            if (chatId == null) {
                throw new BusinessException("会话ID不能为空");
            }

            String userId = getTokenUserInfoDto().getUserId();
            AiChatSession session = aiChatSessionService.getAiChatSessionBySessionId(chatId);

            if (session == null) {
                throw new BusinessException("会话不存在");
            }

            if (!session.getUserId().equals(userId)) {
                throw new BusinessException("无权删除该会话");
            }

            // 软删除会话
            AiChatSession updateBean = new AiChatSession();
            updateBean.setIsDeleted(1);
            updateBean.setUpdateTime(new Date());
            aiChatSessionService.updateAiChatSessionBySessionId(updateBean, chatId);

            // 清除 Redis 上下文
            redisComponent.clearAiContext(chatId);



            return getSuccessResponseVO(null);
        } catch (BusinessException e) {
            return getBusinessErrorResponseVO(e, null);
        }
    }

    @RequestMapping("/history")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO getChatHistory(Long chatId) {
        try {
            if (chatId == null) {
                throw new BusinessException("会话ID不能为空");
            }

            String userId = getTokenUserInfoDto().getUserId();
            AiChatSession session = aiChatSessionService.getAiChatSessionBySessionId(chatId);

            if (session == null) {
                throw new BusinessException("会话不存在");
            }

            if (!session.getUserId().equals(userId)) {
                throw new BusinessException("无权访问该会话");
            }

            AiChatMessageQuery query = new AiChatMessageQuery();
            query.setSessionId(chatId);
            query.setOrderBy("create_time asc");

            List<AiChatMessage> messages = aiChatMessageService.findListByParam(query);

            log.info("获取会话 {} 历史消息,数量: {}", chatId, messages.size());

            return getSuccessResponseVO(messages);  // 直接返回消息列表,包含所有字段
        } catch (BusinessException e) {
            return getBusinessErrorResponseVO(e, null);
        }
    }
    /**
     * 发送消息
     * 前端调用: POST /ai/chat/send
     *
     * @param message 用户消息内容
     * @param chatId 会话 ID (可选,为空时自动创建新会话)
     * @return AI 回复结果
     */
    @RequestMapping("/send")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO sendMessage(@NotEmpty String message, Long chatId) {
        try {
            // 获取当前登录用户 ID
            String userId = getTokenUserInfoDto().getUserId();

            // 调用核心业务服务
            AiChatResponseDto response = aiChatMessageService.sendMessage(userId, message, chatId);


            return getSuccessResponseVO(response);
        } catch (BusinessException e) {
            return getBusinessErrorResponseVO(e, null);
        }
    }
}