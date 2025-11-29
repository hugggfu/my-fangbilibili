package com.easylive.service;

import java.util.List;

import com.easylive.entity.query.AiChatSessionQuery;
import com.easylive.entity.po.AiChatSession;
import com.easylive.entity.vo.PaginationResultVO;


/**
 * AI对话会话表 业务接口
 */
public interface AiChatSessionService {

	/**
	 * 根据条件查询列表
	 */
	List<AiChatSession> findListByParam(AiChatSessionQuery param);

	/**
	 * 根据条件查询列表
	 */
	Integer findCountByParam(AiChatSessionQuery param);

	/**
	 * 分页查询
	 */
	PaginationResultVO<AiChatSession> findListByPage(AiChatSessionQuery param);

	/**
	 * 新增
	 */
	Integer add(AiChatSession bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<AiChatSession> listBean);

	/**
	 * 批量新增/修改
	 */
	Integer addOrUpdateBatch(List<AiChatSession> listBean);

	/**
	 * 多条件更新
	 */
	Integer updateByParam(AiChatSession bean,AiChatSessionQuery param);

	/**
	 * 多条件删除
	 */
	Integer deleteByParam(AiChatSessionQuery param);

	/**
	 * 根据SessionId查询对象
	 */
	AiChatSession getAiChatSessionBySessionId(Long sessionId);


	/**
	 * 根据SessionId修改
	 */
	Integer updateAiChatSessionBySessionId(AiChatSession bean,Long sessionId);


	/**
	 * 根据SessionId删除
	 */
	Integer deleteAiChatSessionBySessionId(Long sessionId);

}