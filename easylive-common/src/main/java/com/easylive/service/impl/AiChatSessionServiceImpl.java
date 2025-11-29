package com.easylive.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.easylive.entity.enums.PageSize;
import com.easylive.entity.query.AiChatSessionQuery;
import com.easylive.entity.po.AiChatSession;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.query.SimplePage;
import com.easylive.mappers.AiChatSessionMapper;
import com.easylive.service.AiChatSessionService;
import com.easylive.utils.StringTools;


/**
 * AI对话会话表 业务接口实现
 */
@Service("aiChatSessionService")
public class AiChatSessionServiceImpl implements AiChatSessionService {

	@Resource
	private AiChatSessionMapper<AiChatSession, AiChatSessionQuery> aiChatSessionMapper;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<AiChatSession> findListByParam(AiChatSessionQuery param) {
		return this.aiChatSessionMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(AiChatSessionQuery param) {
		return this.aiChatSessionMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<AiChatSession> findListByPage(AiChatSessionQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<AiChatSession> list = this.findListByParam(param);
		PaginationResultVO<AiChatSession> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(AiChatSession bean) {
		return this.aiChatSessionMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<AiChatSession> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.aiChatSessionMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<AiChatSession> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.aiChatSessionMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(AiChatSession bean, AiChatSessionQuery param) {
		StringTools.checkParam(param);
		return this.aiChatSessionMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(AiChatSessionQuery param) {
		StringTools.checkParam(param);
		return this.aiChatSessionMapper.deleteByParam(param);
	}

	/**
	 * 根据SessionId获取对象
	 */
	@Override
	public AiChatSession getAiChatSessionBySessionId(Long sessionId) {
		return this.aiChatSessionMapper.selectBySessionId(sessionId);
	}

	/**
	 * 根据SessionId修改
	 */
	@Override
	public Integer updateAiChatSessionBySessionId(AiChatSession bean, Long sessionId) {
		return this.aiChatSessionMapper.updateBySessionId(bean, sessionId);
	}

	/**
	 * 根据SessionId删除
	 */
	@Override
	public Integer deleteAiChatSessionBySessionId(Long sessionId) {
		return this.aiChatSessionMapper.deleteBySessionId(sessionId);
	}
}