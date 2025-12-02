package com.easylive.service;

import com.easylive.entity.dto.VideoRecommendDto;

import java.util.List;

/**
 * AI 视频推荐服务
 */
public interface AiVideoRecommendService {

    /**
     * 根据用户需求智能推荐视频
     *
     * @param userQuery 用户查询内容
     * @param limit 返回数量限制
     * @return 推荐的视频列表
     */
    List<VideoRecommendDto> recommendByQuery(String userQuery, Integer limit);

    /**
     * 获取热门视频
     *
     * @param limit 返回数量限制
     * @return 热门视频列表
     */
    List<VideoRecommendDto> getHotVideos(Integer limit);

    /**
     * 判断用户消息是否需要视频推荐
     *
     * @param message 用户消息
     * @return true-需要推荐视频; false-不需要
     */
    boolean needVideoRecommend(String message);

    /**
     * 提取用户消息中的关键词
     * 使用 AI 提取,如果失败则使用简单规则
     *
     * @param message 用户消息
     * @return 关键词
     */
    String extractKeywords(String message);
}