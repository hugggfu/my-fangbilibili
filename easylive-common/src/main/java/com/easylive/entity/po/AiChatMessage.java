package com.easylive.entity.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;
import com.easylive.entity.enums.DateTimePatternEnum;
import com.easylive.utils.DateUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;


/**
 * AI对话消息表
 */
public class AiChatMessage implements Serializable {


	/**
	 * 消息ID
	 */
	private Long messageId;

	/**
	 * 所属会话ID
	 */
	private Long sessionId;

	/**
	 * 角色: user-用户, assistant-AI, system-系统
	 */
	private String role;

	/**
	 * 消息内容
	 */
	private String content;

	/**
	 * 扩展数据(JSON格式): 推荐视频列表、Token消耗等
	 */
	private String extraData;

	/**
	 * 发送时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date createTime;


	public void setMessageId(Long messageId){
		this.messageId = messageId;
	}

	public Long getMessageId(){
		return this.messageId;
	}

	public void setSessionId(Long sessionId){
		this.sessionId = sessionId;
	}

	public Long getSessionId(){
		return this.sessionId;
	}

	public void setRole(String role){
		this.role = role;
	}

	public String getRole(){
		return this.role;
	}

	public void setContent(String content){
		this.content = content;
	}

	public String getContent(){
		return this.content;
	}

	public void setExtraData(String extraData){
		this.extraData = extraData;
	}

	public String getExtraData(){
		return this.extraData;
	}

	public void setCreateTime(Date createTime){
		this.createTime = createTime;
	}

	public Date getCreateTime(){
		return this.createTime;
	}

	@Override
	public String toString (){
		return "消息ID:"+(messageId == null ? "空" : messageId)+"，所属会话ID:"+(sessionId == null ? "空" : sessionId)+"，角色: user-用户, assistant-AI, system-系统:"+(role == null ? "空" : role)+"，消息内容:"+(content == null ? "空" : content)+"，扩展数据(JSON格式): 推荐视频列表、Token消耗等:"+(extraData == null ? "空" : extraData)+"，发送时间:"+(createTime == null ? "空" : DateUtil.format(createTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()));
	}
}
