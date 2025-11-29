package com.easylive.service;

import java.util.List;

import com.easylive.entity.query.AiUserConfigQuery;
import com.easylive.entity.po.AiUserConfig;
import com.easylive.entity.vo.PaginationResultVO;


/**
 * AI用户配置与统计表 业务接口
 */
public interface AiUserConfigService {

	/**
	 * 根据条件查询列表
	 */
	List<AiUserConfig> findListByParam(AiUserConfigQuery param);

	/**
	 * 根据条件查询列表
	 */
	Integer findCountByParam(AiUserConfigQuery param);

	/**
	 * 分页查询
	 */
	PaginationResultVO<AiUserConfig> findListByPage(AiUserConfigQuery param);

	/**
	 * 新增
	 */
	Integer add(AiUserConfig bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<AiUserConfig> listBean);

	/**
	 * 批量新增/修改
	 */
	Integer addOrUpdateBatch(List<AiUserConfig> listBean);

	/**
	 * 多条件更新
	 */
	Integer updateByParam(AiUserConfig bean,AiUserConfigQuery param);

	/**
	 * 多条件删除
	 */
	Integer deleteByParam(AiUserConfigQuery param);

	/**
	 * 根据UserId查询对象
	 */
	AiUserConfig getAiUserConfigByUserId(String userId);


	/**
	 * 根据UserId修改
	 */
	Integer updateAiUserConfigByUserId(AiUserConfig bean,String userId);


	/**
	 * 根据UserId删除
	 */
	Integer deleteAiUserConfigByUserId(String userId);

}