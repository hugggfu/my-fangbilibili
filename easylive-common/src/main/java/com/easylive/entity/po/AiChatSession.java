package com.easylive.entity.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;
import com.easylive.entity.enums.DateTimePatternEnum;
import com.easylive.utils.DateUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;


/**
 * AI对话会话表
 */
public class AiChatSession implements Serializable {


	/**
	 * 会话ID
	 */
	private Long sessionId;

	/**
	 * 用户ID
	 */
	private String userId;

	/**
	 * 会话标题
	 */
	private String title;

	/**
	 * 创建时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date createTime;

	/**
	 * 最后更新时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date updateTime;

	/**
	 * 是否删除: 0-否, 1-是
	 */
	private Integer isDeleted;


	public void setSessionId(Long sessionId){
		this.sessionId = sessionId;
	}

	public Long getSessionId(){
		return this.sessionId;
	}

	public void setUserId(String userId){
		this.userId = userId;
	}

	public String getUserId(){
		return this.userId;
	}

	public void setTitle(String title){
		this.title = title;
	}

	public String getTitle(){
		return this.title;
	}

	public void setCreateTime(Date createTime){
		this.createTime = createTime;
	}

	public Date getCreateTime(){
		return this.createTime;
	}

	public void setUpdateTime(Date updateTime){
		this.updateTime = updateTime;
	}

	public Date getUpdateTime(){
		return this.updateTime;
	}

	public void setIsDeleted(Integer isDeleted){
		this.isDeleted = isDeleted;
	}

	public Integer getIsDeleted(){
		return this.isDeleted;
	}

	@Override
	public String toString (){
		return "会话ID:"+(sessionId == null ? "空" : sessionId)+"，用户ID:"+(userId == null ? "空" : userId)+"，会话标题:"+(title == null ? "空" : title)+"，创建时间:"+(createTime == null ? "空" : DateUtil.format(createTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()))+"，最后更新时间:"+(updateTime == null ? "空" : DateUtil.format(updateTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()))+"，是否删除: 0-否, 1-是:"+(isDeleted == null ? "空" : isDeleted);
	}
}
