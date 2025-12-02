package com.easylive.service.impl;

import com.easylive.component.AiComponent;
import com.easylive.component.EsSearchComponent;
import com.easylive.entity.dto.AiMessageDto;
import com.easylive.entity.dto.VideoRecommendDto;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.query.VideoInfoQuery;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.service.AiVideoRecommendService;
import com.easylive.service.VideoInfoService;
import com.easylive.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 视频推荐服务实现
 */
@Service("aiVideoRecommendService")
@Slf4j
public class AiVideoRecommendServiceImpl implements AiVideoRecommendService {

    @Resource
    private EsSearchComponent esSearchComponent;

    @Resource
    private VideoInfoService videoInfoService;

    @Resource
    private AiComponent aiComponent;

    @Override
    public List<VideoRecommendDto> recommendByQuery(String userQuery, Integer limit) {
        log.info("根据用户需求推荐视频, 查询: {}, 限制: {}", userQuery, limit);

        // 提取关键词
        String keywords = extractKeywords(userQuery);
        log.info("提取的关键词: {}", keywords);

        // 如果关键词是"推荐"或"热门"等通用词,直接返回热门视频
        if (keywords.equals("推荐") || keywords.equals("热门") ||
                keywords.equals("视频") || keywords.isEmpty()) {
            log.info("关键词过于通用,降级为热门视频推荐");
            return getHotVideos(limit);
        }

        // 使用 ES 搜索视频
        PaginationResultVO<VideoInfo> searchResult = esSearchComponent.search(
                false,  // 不需要高亮
                keywords,
                null,   // 不指定排序,使用相关度排序
                1,      // 第一页
                limit   // 限制数量
        );

        List<VideoInfo> videoList = searchResult.getList();

        // 如果搜索结果为空,降级为热门视频
        if (videoList == null || videoList.isEmpty()) {
            log.info("搜索无结果,降级为热门视频推荐");
            return getHotVideos(limit);
        }

        // 转换为 DTO
        return convertToDto(videoList);
    }
    @Override
    public List<VideoRecommendDto> getHotVideos(Integer limit) {
        log.info("获取热门视频, 限制: {}", limit);

        // 构建查询条件: 按播放量排序(去掉24小时限制)
        VideoInfoQuery query = new VideoInfoQuery();
        query.setOrderBy("play_count desc");
        // query.setLastPlayHour(24);  // 注释掉这行,不限制时间
        query.setPageSize(limit);
        query.setQueryUserInfo(true);  // 查询用户信息

        PaginationResultVO<VideoInfo> result = videoInfoService.findListByPage(query);
        List<VideoInfo> videoList = result.getList();

        log.info("查询到热门视频数量: {}", videoList.size());

        // 转换为 DTO
        return convertToDto(videoList);
    }
    @Override
    public boolean needVideoRecommend(String message) {
        // 关键词列表
        String[] keywords = {
                "推荐", "视频", "看", "有什么", "热门",
                "最新", "搜索", "找", "想看", "播放",
                "观看", "有没有", "给我", "来点", "最火"
        };

        String lowerMessage = message.toLowerCase();

        for (String keyword : keywords) {
            if (lowerMessage.contains(keyword)) {
                log.info("检测到视频推荐需求, 关键词: {}", keyword);
                return true;
            }
        }

        return false;
    }

    @Override
    public String extractKeywords(String message) {
        try {
            // 使用 AI 提取关键词
            List<AiMessageDto> messages = new ArrayList<>();
            messages.add(AiMessageDto.builder()
                    .role("system")
                    .content("你是一个关键词提取助手。用户会发送一段话,你需要提取其中与视频内容相关的核心关键词。" +
                            "规则:\n" +
                            "1. 如果用户询问热门/最火/爆款视频,返回'热门'\n" +
                            "2. 如果用户提到具体的内容类型(如:编程、美食、游戏、音乐、女朋友等),返回该关键词\n" +
                            "3. 忽略'推荐'、'视频'、'看'、'有什么'等无意义词\n" +
                            "4. 只返回最核心的1-2个关键词,用空格分隔\n" +
                            "5. 如果没有明确关键词,返回'推荐'\n" +
                            "\n示例:\n" +
                            "输入:'推荐一些编程视频' → 输出:'编程'\n" +
                            "输入:'我想看美食相关的' → 输出:'美食'\n" +
                            "输入:'推荐女朋友视频' → 输出:'女朋友'\n" +
                            "输入:'最近有什么热门的' → 输出:'热门'")
                    .build());

            messages.add(AiMessageDto.builder()
                    .role("user")
                    .content(message)
                    .build());

            String keywords = aiComponent.chat(messages);

            // 清理 AI 返回的内容,去除多余的标点和空格
            keywords = keywords.trim().replaceAll("[,。!?;:，。!?;:]+", " ");

            log.info("AI 提取的关键词: {}", keywords);
            return keywords;
        } catch (Exception e) {
            log.error("AI 提取关键词失败,使用简单规则", e);
            // 降级方案: 使用简单规则提取
            return extractKeywordsByRule(message);
        }
    }

    /**
     * 使用简单规则提取关键词
     */
    private String extractKeywordsByRule(String message) {
        // 检查是否询问热门
        if (message.contains("热门") || message.contains("最火") || message.contains("爆款")) {
            return "热门";
        }

        // 常见分类关键词 (扩展列表)
        String[] categoryKeywords = {
                "编程", "代码", "java", "python", "前端", "后端", "开发",
                "美食", "做饭", "烹饪", "菜谱", "厨房",
                "游戏", "吃鸡", "王者", "英雄联盟", "LOL",
                "音乐", "唱歌", "钢琴", "吉他", "歌曲",
                "舞蹈", "跳舞", "街舞",
                "运动", "健身", "跑步", "瑜伽",
                "旅游", "旅行", "风景", "景点",
                "搞笑", "幽默", "段子", "喜剧",
                "知识", "科普", "教程", "学习",
                "女朋友", "男朋友", "情侣", "恋爱",  // 新增
                "动漫", "二次元", "番剧",
                "电影", "影视", "剧集"
        };

        // 检查是否包含分类关键词
        for (String keyword : categoryKeywords) {
            if (message.contains(keyword)) {
                return keyword;
            }
        }

        // 移除常见的无意义词
        String[] stopWords = {
                "推荐", "视频", "看", "有什么", "给我",
                "想", "找", "搜索", "播放", "观看",
                "一些", "几个", "的", "吗", "呢", "吧", "相关"
        };

        String result = message;
        for (String word : stopWords) {
            result = result.replace(word, " ");
        }

        result = result.trim();

        // 如果提取后为空或过短,返回"推荐"(会触发热门视频)
        if (result.isEmpty() || result.length() < 2) {
            return "推荐";
        }

        return result;
    }
    /**
     * 将 VideoInfo 转换为 VideoRecommendDto
     */
    private List<VideoRecommendDto> convertToDto(List<VideoInfo> videoList) {
        return videoList.stream().map(video -> {
            VideoRecommendDto dto = new VideoRecommendDto();
            dto.setVideoId(video.getVideoId());
            dto.setVideoName(video.getVideoName());
            dto.setVideoCover(video.getVideoCover());
            dto.setNickName(video.getNickName());
            dto.setUserId(video.getUserId());
            dto.setPlayCount(video.getPlayCount());
            dto.setDanmuCount(video.getDanmuCount());
            dto.setDuration(video.getDuration() != null ?
                    formatDuration(video.getDuration()) : "00:00");
            dto.setCreateTime(video.getCreateTime() != null ?
                    DateUtil.format(video.getCreateTime(), "yyyy-MM-dd") : "");
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 格式化视频时长
     * 将秒数转换为 mm:ss 格式
     */
    private String formatDuration(Integer seconds) {
        if (seconds == null || seconds == 0) {
            return "00:00";
        }
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }
}