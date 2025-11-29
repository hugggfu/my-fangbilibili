package com.easylive.service;

import java.util.List;

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
	Integer updateByParam(AiChatMessage bean,AiChatMessageQuery param);

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
	Integer updateAiChatMessageByMessageId(AiChatMessage bean,Long messageId);


	/**
	 * 根据MessageId删除
	 */
	Integer deleteAiChatMessageByMessageId(Long messageId);

}