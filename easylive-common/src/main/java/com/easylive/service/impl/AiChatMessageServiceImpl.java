package com.easylive.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.easylive.entity.enums.PageSize;
import com.easylive.entity.query.AiChatMessageQuery;
import com.easylive.entity.po.AiChatMessage;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.query.SimplePage;
import com.easylive.mappers.AiChatMessageMapper;
import com.easylive.service.AiChatMessageService;
import com.easylive.utils.StringTools;


/**
 * AI对话消息表 业务接口实现
 */
@Service("aiChatMessageService")
public class AiChatMessageServiceImpl implements AiChatMessageService {

	@Resource
	private AiChatMessageMapper<AiChatMessage, AiChatMessageQuery> aiChatMessageMapper;

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
		PaginationResultVO<AiChatMessage> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
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
}