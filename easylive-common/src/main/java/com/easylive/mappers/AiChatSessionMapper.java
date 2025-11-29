package com.easylive.mappers;

import org.apache.ibatis.annotations.Param;

/**
 * AI对话会话表 数据库操作接口
 */
public interface AiChatSessionMapper<T,P> extends BaseMapper<T,P> {

	/**
	 * 根据SessionId更新
	 */
	 Integer updateBySessionId(@Param("bean") T t,@Param("sessionId") Long sessionId);


	/**
	 * 根据SessionId删除
	 */
	 Integer deleteBySessionId(@Param("sessionId") Long sessionId);


	/**
	 * 根据SessionId获取对象
	 */
	 T selectBySessionId(@Param("sessionId") Long sessionId);


}
