package com.easylive.component;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.easylive.entity.config.AppConfig;
import com.easylive.entity.config.OssConfig;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.dto.*;
import com.easylive.entity.enums.DateTimePatternEnum;

import com.easylive.entity.po.CategoryInfo;
import com.easylive.entity.po.VideoInfoFilePost;
import com.easylive.redis.RedisUtils;
import com.easylive.utils.DateUtil;
import com.easylive.utils.StringTools;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class RedisComponent {

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private AppConfig appConfig;

    @Resource
    private OssConfig ossConfig;

    public String                                                                     saveCheckCode(String code){
        String  checkCodeKey= UUID.randomUUID().toString();
        redisUtils. setex(Constants.REDIS_KEY_CHECK_CODE+checkCodeKey,code,Constants.REDIS_KEY_EXPIRES_ONE_MIN*10);
        return checkCodeKey;
    }

    public String getCheckCode(String checkCodeKey){
        return (String) redisUtils.get(Constants.REDIS_KEY_CHECK_CODE+checkCodeKey);
    }


    public void cleanCheckCode(String checkCodeKey){
        redisUtils.delete(Constants.REDIS_KEY_CHECK_CODE+checkCodeKey);
    }
    public void saveTokenInfo(TokenUserInfoDto tokenUserInfoDto) {
        //生成一个token
        String token = UUID.randomUUID().toString();
        //设置过期时间
        tokenUserInfoDto.setExpireAt(System.currentTimeMillis() + Constants.REDIS_KEY_EXPIRES_DAY * 7);
        //设置token
        tokenUserInfoDto.setToken(token);
        //信息保存在redis中，有效期 7天
        redisUtils.setex(Constants.REDIS_KEY_TOKEN_WEB + token, tokenUserInfoDto,  Constants.REDIS_KEY_EXPIRES_DAY * 7);
    }

    public void cleanToken(String token) {
        //删除redis数据
        redisUtils.delete(Constants.REDIS_KEY_TOKEN_WEB + token);
    }

    public TokenUserInfoDto getTokenInfo(String token) {
        //从redis中获取与token匹配信息
        return (TokenUserInfoDto) redisUtils.get(Constants.REDIS_KEY_TOKEN_WEB + token);
    }

    public String getLoginInfo4Admin(String token) {
        return (String) redisUtils.get(Constants.REDIS_KEY_TOKEN_ADMIN + token);
    }

    public String saveTokenInfo4Admin(String account) {
        //生成一个token
        String token = UUID.randomUUID().toString();
        redisUtils.setex(Constants.REDIS_KEY_TOKEN_ADMIN + token, account, Constants.REDIS_KEY_EXPIRES_DAY);
        return token;
    }

    public String getTokenInfo4Admin(String token) {
        //查寻一个token
        return (String) redisUtils.get(Constants.REDIS_KEY_TOKEN_ADMIN + token);
    }

    public void cleanToken4Admin(String token) {
        //删除redis数据
        redisUtils.delete(Constants.REDIS_KEY_TOKEN_ADMIN + token);
    }


    /**
     * 保存上传文件信息
     *
     * @param fileName
     * @param chunks
     * @return
     */

    public String savePreVideoFileInfo(String userId, String fileName, Integer chunks) {

        String uploadId = StringTools.getRandomString(Constants.LENGTH_15);
        UploadingFileDto fileDto = new UploadingFileDto();
        fileDto.setChunks(chunks);
        fileDto.setFileName(fileName);
        fileDto.setUploadId(uploadId);
        fileDto.setChunkIndex(0);

        String day = DateUtil.format(new Date(), DateTimePatternEnum.YYYYMMDD.getPattern());


        // 5. OSS 对象 Key
        String filePath = "video/" + day + "/"  + userId + uploadId;


        fileDto.setFilePath(filePath);
        redisUtils.setex(Constants.REDIS_KEY_UPLOADING_FILE + userId + uploadId, fileDto, Constants.REDIS_KEY_EXPIRES_DAY);
        return uploadId;

    }

    public void updateVideoFileInfo(String userId, UploadingFileDto fileDto) {
        redisUtils.setex(Constants.REDIS_KEY_UPLOADING_FILE + userId + fileDto.getUploadId(), fileDto, Constants.REDIS_KEY_EXPIRES_DAY);
    }

    public UploadingFileDto getUploadingVideoFile(String userId, String uploadId) {
        return (UploadingFileDto) redisUtils.get(Constants.REDIS_KEY_UPLOADING_FILE + userId + uploadId);
    }

    public void delVideoFileInfo(String userId, String uploadId) {
        redisUtils.delete(Constants.REDIS_KEY_UPLOADING_FILE + userId + uploadId);
    }

    /**
     * 获取系统设置
     *
     * @return
     */
    public SysSettingDto getSysSettingDto() {
        SysSettingDto sysSettingDto = (SysSettingDto) redisUtils.get(Constants.REDIS_KEY_SYS_SETTING);
        if (sysSettingDto == null) {
            sysSettingDto = new SysSettingDto();
        }
        return sysSettingDto;
    }

    public void saveSettingDto(SysSettingDto sysSettingDto) {
        redisUtils.set(Constants.REDIS_KEY_SYS_SETTING, sysSettingDto);
    }


    public void saveCategoryList(List<CategoryInfo> categoryInfoList) {
        redisUtils.set(Constants.REDIS_KEY_CATEGORY_LIST, categoryInfoList);
    }

    public List<CategoryInfo> getCategoryList() {
       return (List<CategoryInfo>) redisUtils.get(Constants.REDIS_KEY_CATEGORY_LIST);
    }

    public void addFile2DelQueue(String videoId, List<String> fileIdList) {
        redisUtils.lpushAll(Constants.REDIS_KEY_FILE_DEL + videoId, fileIdList, Constants.REDIS_KEY_EXPIRES_DAY * 7);
    }

    public void addFile2TransferQueue(List<VideoInfoFilePost> addFileList) {
        redisUtils.lpushAll(Constants.REDIS_KEY_QUEUE_TRANSFER, addFileList, 0);
    }

    public List<String> getDelFileList(String videoId) {
        List<String> filePathList = redisUtils.getQueueList(Constants.REDIS_KEY_FILE_DEL + videoId);
        return filePathList;
    }

    public void cleanDelFileList(String videoId) {
            redisUtils.delete(Constants.REDIS_KEY_FILE_DEL + videoId);

    }

    public Integer reportVideoPlayOnline(String fileId, String deviceId) {
        //某个视频下某个设备的标识（临时存在，短期过期）
        String userPlayOnlineKey = String.format(Constants.REDIS_KEY_VIDEO_PLAY_COUNT_USER, fileId, deviceId);
        //某个视频的“当前在线人数”
        String playOnlineCountKey = String.format(Constants.REDIS_KEY_VIDEO_PLAY_COUNT_ONLINE, fileId);
        /*只要一个设备播放视频，就会在 Redis 里创建一个用户 key。
        当这个用户 key 过期（设备停止播放或断线），在线人数会自动 -1*/
        if (!redisUtils.keyExists(userPlayOnlineKey)) {

            // 如果设备还没有播放过这个视频（即首次播放）
            redisUtils.setex(userPlayOnlineKey, fileId, Constants.REDIS_KEY_EXPIRES_ONE_SECONDS * 8);
            return redisUtils.incrementex(playOnlineCountKey, Constants.REDIS_KEY_EXPIRES_ONE_SECONDS * 10).intValue();
        }
        //给视频在线总数量续期
        redisUtils.expire(playOnlineCountKey, Constants.REDIS_KEY_EXPIRES_ONE_SECONDS * 10);
        //给播放用户续期
        redisUtils.expire(userPlayOnlineKey, Constants.REDIS_KEY_EXPIRES_ONE_SECONDS * 8);
        Integer count = (Integer) redisUtils.get(playOnlineCountKey);
        return count == null ? 1 : count;
    }

    /**
     * 减少数量
     *
     * @param key
     */
    public void decrementPlayOnlineCount(String key) {
        redisUtils.decrement(key);
    }

    public void updateTokenInfo(TokenUserInfoDto tokenUserInfoDto) {
        redisUtils.setex(Constants.REDIS_KEY_TOKEN_WEB + tokenUserInfoDto.getToken(), tokenUserInfoDto, Constants.REDIS_KEY_EXPIRES_DAY * 7);
    }

    public void addKeywordCount(String keyword) {
        redisUtils.zaddCount(Constants.REDIS_KEY_VIDEO_SEARCH_COUNT, keyword);
    }

    public List<String> getKeywordTop(Integer top) {
        return redisUtils.getZSetList(Constants.REDIS_KEY_VIDEO_SEARCH_COUNT, top - 1);
    }

    public void addVideoPlay(VideoPlayInfoDto videoPlayInfoDto) {
        redisUtils.lpush(Constants.REDIS_KEY_QUEUE_VIDEO_PLAY, videoPlayInfoDto, null);
    }

    public void recordVideoPlayCount(String videoId) {
        String date = DateUtil.format(new Date(), DateTimePatternEnum.YYYY_MM_DD.getPattern());
        redisUtils.incrementex(Constants.REDIS_KEY_VIDEO_PLAY_COUNT + date + ":" + videoId, Constants.REDIS_KEY_EXPIRES_DAY * 2L);
    }

    public Map<String, Integer> getVideoPlayCount(String date) {
        Map<String, Integer> videoPlayMap = redisUtils.getBatch(Constants.REDIS_KEY_VIDEO_PLAY_COUNT + date);
        return videoPlayMap;
    }

    // ==================== AI 上下文管理 ====================

    /**
     * 保存 AI 对话上下文到 Redis
     *
     * @param sessionId 会话 ID
     * @param messages 消息列表
     */
    public void saveAiContext(Long sessionId, List<AiMessageDto> messages) {
        String key = Constants.REDIS_KEY_AI_CONTEXT + sessionId;
        redisUtils.setex(key, messages, Constants.REDIS_KEY_AI_CONTEXT_EXPIRE);
    }

    /**
     * 获取 AI 对话上下文
     *
     * @param sessionId 会话 ID
     * @return 消息列表,如果不存在返回 null
     */
    public List<AiMessageDto> getAiContext(Long sessionId) {
        String key = Constants.REDIS_KEY_AI_CONTEXT + sessionId;
        return (List<AiMessageDto>) redisUtils.get(key);
    }

    /**
     * 添加消息到上下文
     * 如果上下文不存在,则创建新的
     *
     * @param sessionId 会话 ID
     * @param message 新消息
     * @param maxContextSize 最大上下文数量
     */
    public void addMessageToContext(Long sessionId, AiMessageDto message, Integer maxContextSize) {
        List<AiMessageDto> context = getAiContext(sessionId);

        if (context == null) {
            context = new ArrayList<>();
        }

        // 添加新消息
        context.add(message);

        // 如果超过最大数量,移除最早的消息(保留系统提示词)
        if (context.size() > maxContextSize) {
            // 保留第一条(系统提示词)和最新的消息
            List<AiMessageDto> newContext = new ArrayList<>();
            newContext.add(context.get(0)); // 系统提示词
            newContext.addAll(context.subList(context.size() - maxContextSize + 1, context.size()));
            context = newContext;
        }

        // 保存回 Redis
        saveAiContext(sessionId, context);
    }

    /**
     * 清除 AI 对话上下文
     *
     * @param sessionId 会话 ID
     */
    public void clearAiContext(Long sessionId) {
        String key = Constants.REDIS_KEY_AI_CONTEXT + sessionId;
        redisUtils.delete(key);
    }

    /**
     * 刷新上下文过期时间
     * 每次对话时调用,保持上下文活跃
     *
     * @param sessionId 会话 ID
     */
    public void refreshAiContextExpire(Long sessionId) {
        String key = Constants.REDIS_KEY_AI_CONTEXT + sessionId;
        redisUtils.expire(key, Constants.REDIS_KEY_AI_CONTEXT_EXPIRE);
    }


}
