package com.easylive.entity.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;
import com.easylive.entity.enums.DateTimePatternEnum;
import com.easylive.utils.DateUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;


/**
 * AI用户配置与统计表
 */
public class AiUserConfig implements Serializable {


	/**
	 * 用户ID
	 */
	private String userId;

	/**
	 * 今日使用次数
	 */
	private Integer dailyUsageCount;

	/**
	 * 最后使用日期
	 */
	@JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private Date lastUsageDate;

	/**
	 * 用户偏好设置(JSON格式): 主题、音效等
	 */
	private String settings;

	/**
	 * 首次使用时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date createTime;


	public void setUserId(String userId){
		this.userId = userId;
	}

	public String getUserId(){
		return this.userId;
	}

	public void setDailyUsageCount(Integer dailyUsageCount){
		this.dailyUsageCount = dailyUsageCount;
	}

	public Integer getDailyUsageCount(){
		return this.dailyUsageCount;
	}

	public void setLastUsageDate(Date lastUsageDate){
		this.lastUsageDate = lastUsageDate;
	}

	public Date getLastUsageDate(){
		return this.lastUsageDate;
	}

	public void setSettings(String settings){
		this.settings = settings;
	}

	public String getSettings(){
		return this.settings;
	}

	public void setCreateTime(Date createTime){
		this.createTime = createTime;
	}

	public Date getCreateTime(){
		return this.createTime;
	}

	@Override
	public String toString (){
		return "用户ID:"+(userId == null ? "空" : userId)+"，今日使用次数:"+(dailyUsageCount == null ? "空" : dailyUsageCount)+"，最后使用日期:"+(lastUsageDate == null ? "空" : DateUtil.format(lastUsageDate, DateTimePatternEnum.YYYY_MM_DD.getPattern()))+"，用户偏好设置(JSON格式): 主题、音效等:"+(settings == null ? "空" : settings)+"，首次使用时间:"+(createTime == null ? "空" : DateUtil.format(createTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()));
	}
}
