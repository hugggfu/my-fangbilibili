package com.easylive.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.easylive.entity.enums.PageSize;
import com.easylive.entity.query.AiUserConfigQuery;
import com.easylive.entity.po.AiUserConfig;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.query.SimplePage;
import com.easylive.mappers.AiUserConfigMapper;
import com.easylive.service.AiUserConfigService;
import com.easylive.utils.StringTools;


/**
 * AI用户配置与统计表 业务接口实现
 */
@Service("aiUserConfigService")
public class AiUserConfigServiceImpl implements AiUserConfigService {

	@Resource
	private AiUserConfigMapper<AiUserConfig, AiUserConfigQuery> aiUserConfigMapper;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<AiUserConfig> findListByParam(AiUserConfigQuery param) {
		return this.aiUserConfigMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(AiUserConfigQuery param) {
		return this.aiUserConfigMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<AiUserConfig> findListByPage(AiUserConfigQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<AiUserConfig> list = this.findListByParam(param);
		PaginationResultVO<AiUserConfig> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(AiUserConfig bean) {
		return this.aiUserConfigMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<AiUserConfig> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.aiUserConfigMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<AiUserConfig> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.aiUserConfigMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(AiUserConfig bean, AiUserConfigQuery param) {
		StringTools.checkParam(param);
		return this.aiUserConfigMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(AiUserConfigQuery param) {
		StringTools.checkParam(param);
		return this.aiUserConfigMapper.deleteByParam(param);
	}

	/**
	 * 根据UserId获取对象
	 */
	@Override
	public AiUserConfig getAiUserConfigByUserId(String userId) {
		return this.aiUserConfigMapper.selectByUserId(userId);
	}

	/**
	 * 根据UserId修改
	 */
	@Override
	public Integer updateAiUserConfigByUserId(AiUserConfig bean, String userId) {
		return this.aiUserConfigMapper.updateByUserId(bean, userId);
	}

	/**
	 * 根据UserId删除
	 */
	@Override
	public Integer deleteAiUserConfigByUserId(String userId) {
		return this.aiUserConfigMapper.deleteByUserId(userId);
	}
}