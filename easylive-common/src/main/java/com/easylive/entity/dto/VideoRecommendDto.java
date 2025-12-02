package com.easylive.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 视频推荐 DTO
 * 用于 AI 推荐视频时返回给前端
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoRecommendDto {
    /**
     * 视频 ID
     */
    private String videoId;

    /**
     * 视频名称
     */
    private String videoName;

    /**
     * 视频封面
     */
    private String videoCover;

    /**
     * UP主昵称
     */
    private String nickName;

    /**
     * UP主 ID
     */
    private String userId;

    /**
     * 播放量
     */
    private Integer playCount;

    /**
     * 弹幕数
     */
    private Integer danmuCount;

    /**
     * 视频时长 (格式化后的字符串,如 "05:30")
     */
    private String duration;

    /**
     * 创建时间
     */
    private String createTime;
}