package com.easylive.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import com.alibaba.dashscope.common.Message;
import com.easylive.component.AiComponent;
import com.easylive.component.RedisComponent;
import com.easylive.entity.config.AiConfig;
import com.easylive.entity.dto.AiChatResponseDto;
import com.easylive.entity.dto.AiMessageDto;
import com.easylive.entity.po.AiChatSession;
import com.easylive.entity.po.AiUserConfig;
import com.easylive.exception.BusinessException;
import com.easylive.service.AiChatSessionService;
import com.easylive.service.AiUserConfigService;
import org.springframework.stereotype.Service;

import com.easylive.entity.enums.PageSize;
import com.easylive.entity.query.AiChatMessageQuery;
import com.easylive.entity.po.AiChatMessage;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.query.SimplePage;
import com.easylive.mappers.AiChatMessageMapper;
import com.easylive.service.AiChatMessageService;
import com.easylive.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI对话消息表 业务接口实现
 */
@Service("aiChatMessageService")
public class AiChatMessageServiceImpl implements AiChatMessageService {

	@Resource
	private AiChatMessageMapper<AiChatMessage, AiChatMessageQuery> aiChatMessageMapper;

    @Resource
    private AiChatSessionService aiChatSessionService;



    @Resource
    private AiUserConfigService aiUserConfigService;

    @Resource
    private AiComponent aiComponent;

    @Resource
    private AiConfig aiConfig;

    @Resource
    private RedisComponent redisComponent;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<AiChatMessage> findListByParam(AiChatMessageQuery param) {
		return this.aiChatMessageMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(AiChatMessageQuery param) {
		return this.aiChatMessageMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<AiChatMessage> findListByPage(AiChatMessageQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<AiChatMessage> list = this.findListByParam(param);
		PaginationResultVO<AiChatMessage> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(),
				page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(AiChatMessage bean) {
		return this.aiChatMessageMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<AiChatMessage> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.aiChatMessageMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<AiChatMessage> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.aiChatMessageMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(AiChatMessage bean, AiChatMessageQuery param) {
		StringTools.checkParam(param);
		return this.aiChatMessageMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(AiChatMessageQuery param) {
		StringTools.checkParam(param);
		return this.aiChatMessageMapper.deleteByParam(param);
	}

	/**
	 * 根据MessageId获取对象
	 */
	@Override
	public AiChatMessage getAiChatMessageByMessageId(Long messageId) {
		return this.aiChatMessageMapper.selectByMessageId(messageId);
	}

	/**
	 * 根据MessageId修改
	 */
	@Override
	public Integer updateAiChatMessageByMessageId(AiChatMessage bean, Long messageId) {
		return this.aiChatMessageMapper.updateByMessageId(bean, messageId);
	}

	/**
	 * 根据MessageId删除
	 */
	@Override
	public Integer deleteAiChatMessageByMessageId(Long messageId) {
		return this.aiChatMessageMapper.deleteByMessageId(messageId);
	}

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChatResponseDto sendMessage(String userId, String message, Long chatId) {

        // ========== 步骤 1: 检查每日使用限制 ==========
        AiUserConfig aiUserConfig = aiUserConfigService.getAiUserConfigByUserId(userId);
        if(aiUserConfig==null){
            // 首次使用,创建配置
            aiUserConfig = new AiUserConfig();
            aiUserConfig.setUserId(userId);
            aiUserConfig.setDailyUsageCount(0);
            aiUserConfig.setLastUsageDate(new Date());
            aiUserConfig.setCreateTime(new Date());
            aiUserConfigService.add(aiUserConfig);
        }else{
            // 检查是否需要重置每日计数(跨天了)
            if(needResetDailyCount(aiUserConfig.getLastUsageDate())){
                aiUserConfig.setDailyUsageCount(0);
                aiUserConfig.setLastUsageDate(new Date());
            }
            // 检查是否达到限制(使用配置文件中的限制)
            if (aiUserConfig.getDailyUsageCount() >= aiConfig.getDailyLimit()) {
                throw new BusinessException("今日使用次数已达上限 (" + aiConfig.getDailyLimit() + " 次),请明天再试");
            }
        }
                          // ========== 步骤 2: 处理会话 ==========
        AiChatSession aiChatSession;
        Boolean IsNewChat=false;
        if(chatId==null){
            // 创建新会话
            aiChatSession = new AiChatSession();
            aiChatSession.setUserId(userId);
            aiChatSession.setTitle("新对话");
            aiChatSession.setCreateTime(new Date());
            aiChatSession.setUpdateTime(new Date());
            aiChatSession.setIsDeleted(0);

            aiChatSessionService.add(aiChatSession);
            chatId = aiChatSession.getSessionId();
            IsNewChat = true;
        }
        // 验证会话归属权
        aiChatSession = aiChatSessionService.getAiChatSessionBySessionId(chatId);

        if (aiChatSession == null) {
            throw new BusinessException("会话不存在");
        }

        if (!aiChatSession.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该会话");
        }

        // ========== 步骤 3: 保存用户消息 ==========
        AiChatMessage userMessage = new AiChatMessage();
        userMessage.setSessionId(chatId);
        userMessage.setRole("user");
        userMessage.setContent(message);
        userMessage.setCreateTime(new Date());

        aiChatMessageMapper.insert(userMessage);

        // ========== 步骤 4: 构建 AI 上下文 ==========
        List<AiMessageDto> contextMessages = buildContext(chatId);
        // 添加当前用户消息
        contextMessages.add(AiMessageDto.builder()
                .role("user")
                .content(message)
                .build());
        // ========== 步骤 5: 调用 AI ==========
        String aiResponse;
        try {
            aiResponse = aiComponent.chat(contextMessages);
        } catch (Exception e) {
            throw new BusinessException("AI 服务暂时不可用,请稍后重试");
        }

        // ========== 步骤 6: 保存 AI 消息 ==========
        AiChatMessage aiMessage = new AiChatMessage();
        aiMessage.setSessionId(chatId);
        aiMessage.setRole("assistant");
        aiMessage.setContent(aiResponse);
        aiMessage.setCreateTime(new Date());

        aiChatMessageMapper.insert(aiMessage);
// ========== 新增: 更新 Redis 上下文 ==========
// 添加用户消息到上下文
        redisComponent.addMessageToContext(chatId,
                AiMessageDto.builder()
                        .role("user")
                        .content(message)
                        .build(),
                aiConfig.getContextLimit());

// 添加 AI 回复到上下文
        redisComponent.addMessageToContext(chatId,
                AiMessageDto.builder()
                        .role("assistant")
                        .content(aiResponse)
                        .build(),
                aiConfig.getContextLimit());


        // ========== 步骤 7: 更新会话时间 ==========
        AiChatSession updateSession = new AiChatSession();
        updateSession.setUpdateTime(new Date());
        aiChatSessionService.updateAiChatSessionBySessionId(updateSession, chatId);

        // ========== 步骤 8: 自动生成标题 (如果是新对话或标题为"新对话") ==========
        if (IsNewChat || "新对话".equals(aiChatSession.getTitle())) {
            try {
                String newTitle = aiComponent.generateTitle(message);

                AiChatSession titleUpdate = new AiChatSession();
                titleUpdate.setTitle(newTitle);
                aiChatSessionService.updateAiChatSessionBySessionId(titleUpdate, chatId);

                aiChatSession.setTitle(newTitle);

            } catch (Exception e) {
                // 使用用户消息的前 15 个字作为标题
                String defaultTitle = message.length() > 15
                        ? message.substring(0, 15) + "..."
                        : message;

                AiChatSession titleUpdate = new AiChatSession();
                titleUpdate.setTitle(defaultTitle);
                aiChatSessionService.updateAiChatSessionBySessionId(titleUpdate, chatId);
                aiChatSession.setTitle(defaultTitle);
            }
        }

        // ========== 步骤 9: 增加使用次数 ==========
        aiUserConfig.setDailyUsageCount(aiUserConfig.getDailyUsageCount() + 1);
        aiUserConfig.setLastUsageDate(new Date());  // 更新最后使用日期
        aiUserConfigService.updateAiUserConfigByUserId(aiUserConfig, userId);


        // ========== 步骤 10: 返回结果 ==========
        AiChatResponseDto response = new AiChatResponseDto();
        response.setMessageId(aiMessage.getMessageId());
        response.setContent(aiResponse);
        response.setChatId(chatId);
        response.setTitle(aiChatSession.getTitle());

        return response;
    }

    /**
     * 构建 AI 上下文
     * 获取最近的历史消息,加上系统提示词
     */
    private List<AiMessageDto> buildContext(Long sessionId) {
        // 尝试从 Redis 获取上下文
        List<AiMessageDto> messages = redisComponent.getAiContext(sessionId);

        if (messages != null && !messages.isEmpty()) {
            // 刷新过期时间
            redisComponent.refreshAiContextExpire(sessionId);
            return messages;
        }

        // Redis 中没有,从数据库加载

        messages = new ArrayList<>();

        // 添加系统提示词
        AiMessageDto systemMessage = AiMessageDto.builder()
                .role("system")
                .content(aiConfig.getSystemPrompt())
                .build();
        messages.add(systemMessage);

        // 获取最近的历史消息
        AiChatMessageQuery query = new AiChatMessageQuery();
        query.setSessionId(sessionId);
        query.setOrderBy("create_time desc");
        query.setPageSize(aiConfig.getContextLimit() - 1);

        List<AiChatMessage> historyMessages = aiChatMessageMapper.selectList(query);

        // 反转列表,使其按时间正序
        java.util.Collections.reverse(historyMessages);

        // 转换为 DTO
        for (AiChatMessage msg : historyMessages) {
            messages.add(AiMessageDto.builder()
                    .role(msg.getRole())
                    .content(msg.getContent())
                    .build());
        }

        // 保存到 Redis
        if (!messages.isEmpty()) {
            redisComponent.saveAiContext(sessionId, messages);

        }

        return messages;
    }

    private boolean needResetDailyCount(Date lastUsageDate) {
        if (lastUsageDate == null) {
            return true;
        }

        Date today = new Date();
        String todayStr = String.format("%tF", today);
        String lastUsageStr = String.format("%tF", lastUsageDate);

        return !todayStr.equals(lastUsageStr);
    }
}