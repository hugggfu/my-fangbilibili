package com.easylive.service;

import java.util.List;

import com.easylive.entity.dto.AiChatResponseDto;
import com.easylive.entity.query.AiChatMessageQuery;
import com.easylive.entity.po.AiChatMessage;
import com.easylive.entity.vo.PaginationResultVO;

/**
 * AI对话消息表 业务接口
 */
public interface AiChatMessageService {

	/**
	 * 根据条件查询列表
	 */
	List<AiChatMessage> findListByParam(AiChatMessageQuery param);

	/**
	 * 根据条件查询列表
	 */
	Integer findCountByParam(AiChatMessageQuery param);

	/**
	 * 分页查询
	 */
	PaginationResultVO<AiChatMessage> findListByPage(AiChatMessageQuery param);

	/**
	 * 新增
	 */
	Integer add(AiChatMessage bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<AiChatMessage> listBean);

	/**
	 * 批量新增/修改
	 */
	Integer addOrUpdateBatch(List<AiChatMessage> listBean);

	/**
	 * 多条件更新
	 */
	Integer updateByParam(AiChatMessage bean, AiChatMessageQuery param);

	/**
	 * 多条件删除
	 */
	Integer deleteByParam(AiChatMessageQuery param);

	/**
	 * 根据MessageId查询对象
	 */
	AiChatMessage getAiChatMessageByMessageId(Long messageId);

	/**
	 * 根据MessageId修改
	 */
	Integer updateAiChatMessageByMessageId(AiChatMessage bean, Long messageId);

	/**
	 * 根据MessageId删除
	 */
	Integer deleteAiChatMessageByMessageId(Long messageId);

    /**
     * 发送消息并获取 AI 回复
     * 这是最核心的方法,整合了所有业务逻辑
     *
     * @param userId 用户 ID
     * @param message 用户消息内容
     * @param chatId 会话 ID (可为空,为空时自动创建新会话)
     * @return AI 回复结果
     */
    AiChatResponseDto sendMessage(String userId, String message, Long chatId);

}