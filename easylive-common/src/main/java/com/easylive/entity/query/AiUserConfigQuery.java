package com.easylive.entity.query;

import java.util.Date;


/**
 * AI用户配置与统计表参数
 */
public class AiUserConfigQuery extends BaseParam {


	/**
	 * 用户ID
	 */
	private String userId;

	private String userIdFuzzy;

	/**
	 * 今日使用次数
	 */
	private Integer dailyUsageCount;

	/**
	 * 最后使用日期
	 */
	private String lastUsageDate;

	private String lastUsageDateStart;

	private String lastUsageDateEnd;

	/**
	 * 用户偏好设置(JSON格式): 主题、音效等
	 */
	private String settings;

	private String settingsFuzzy;

	/**
	 * 首次使用时间
	 */
	private String createTime;

	private String createTimeStart;

	private String createTimeEnd;


	public void setUserId(String userId){
		this.userId = userId;
	}

	public String getUserId(){
		return this.userId;
	}

	public void setUserIdFuzzy(String userIdFuzzy){
		this.userIdFuzzy = userIdFuzzy;
	}

	public String getUserIdFuzzy(){
		return this.userIdFuzzy;
	}

	public void setDailyUsageCount(Integer dailyUsageCount){
		this.dailyUsageCount = dailyUsageCount;
	}

	public Integer getDailyUsageCount(){
		return this.dailyUsageCount;
	}

	public void setLastUsageDate(String lastUsageDate){
		this.lastUsageDate = lastUsageDate;
	}

	public String getLastUsageDate(){
		return this.lastUsageDate;
	}

	public void setLastUsageDateStart(String lastUsageDateStart){
		this.lastUsageDateStart = lastUsageDateStart;
	}

	public String getLastUsageDateStart(){
		return this.lastUsageDateStart;
	}
	public void setLastUsageDateEnd(String lastUsageDateEnd){
		this.lastUsageDateEnd = lastUsageDateEnd;
	}

	public String getLastUsageDateEnd(){
		return this.lastUsageDateEnd;
	}

	public void setSettings(String settings){
		this.settings = settings;
	}

	public String getSettings(){
		return this.settings;
	}

	public void setSettingsFuzzy(String settingsFuzzy){
		this.settingsFuzzy = settingsFuzzy;
	}

	public String getSettingsFuzzy(){
		return this.settingsFuzzy;
	}

	public void setCreateTime(String createTime){
		this.createTime = createTime;
	}

	public String getCreateTime(){
		return this.createTime;
	}

	public void setCreateTimeStart(String createTimeStart){
		this.createTimeStart = createTimeStart;
	}

	public String getCreateTimeStart(){
		return this.createTimeStart;
	}
	public void setCreateTimeEnd(String createTimeEnd){
		this.createTimeEnd = createTimeEnd;
	}

	public String getCreateTimeEnd(){
		return this.createTimeEnd;
	}

}
