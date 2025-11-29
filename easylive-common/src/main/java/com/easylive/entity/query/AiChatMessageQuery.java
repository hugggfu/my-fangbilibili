package com.easylive.entity.query;

import java.util.Date;


/**
 * AI对话消息表参数
 */
public class AiChatMessageQuery extends BaseParam {


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

	private String roleFuzzy;

	/**
	 * 消息内容
	 */
	private String content;

	private String contentFuzzy;

	/**
	 * 扩展数据(JSON格式): 推荐视频列表、Token消耗等
	 */
	private String extraData;

	private String extraDataFuzzy;

	/**
	 * 发送时间
	 */
	private String createTime;

	private String createTimeStart;

	private String createTimeEnd;


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

	public void setRoleFuzzy(String roleFuzzy){
		this.roleFuzzy = roleFuzzy;
	}

	public String getRoleFuzzy(){
		return this.roleFuzzy;
	}

	public void setContent(String content){
		this.content = content;
	}

	public String getContent(){
		return this.content;
	}

	public void setContentFuzzy(String contentFuzzy){
		this.contentFuzzy = contentFuzzy;
	}

	public String getContentFuzzy(){
		return this.contentFuzzy;
	}

	public void setExtraData(String extraData){
		this.extraData = extraData;
	}

	public String getExtraData(){
		return this.extraData;
	}

	public void setExtraDataFuzzy(String extraDataFuzzy){
		this.extraDataFuzzy = extraDataFuzzy;
	}

	public String getExtraDataFuzzy(){
		return this.extraDataFuzzy;
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
