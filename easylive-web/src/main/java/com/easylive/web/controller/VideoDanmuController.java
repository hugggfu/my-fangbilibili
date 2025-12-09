package com.easylive.web.controller;

import com.alibaba.fastjson.JSON;
import com.easylive.component.RedisComponent;
import com.easylive.entity.config.RabbitMQConfig;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.po.VideoDanmu;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.query.VideoDanmuQuery;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.service.VideoDanmuService;
import com.easylive.service.impl.VideoInfoServiceImpl;
import com.easylive.utils.JsonUtils;
import com.easylive.web.annotation.GlobalInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Validated
@RequestMapping("/danmu")
@Slf4j
public class VideoDanmuController extends ABaseController {

    @Resource
    private VideoDanmuService videoDanmuService;

    @Resource
    private VideoInfoServiceImpl videoInfoService;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    RedisComponent redisComponent;

    @RequestMapping("/loadDanmu")
    @GlobalInterceptor
    public ResponseVO loadDanmu(@NotEmpty String fileId, @NotEmpty String videoId) {

        VideoInfo videoInfo = videoInfoService.getVideoInfoByVideoId(videoId);
        if (videoInfo.getInteraction() != null && videoInfo.getInteraction().contains(Constants.ZERO.toString())) {
            return getSuccessResponseVO(new ArrayList<>());
        }
        List<String > damu = redisComponent.getDamu(fileId);
        List<VideoDanmu> result = damu.stream().map(s -> JSON.parseObject(s, VideoDanmu.class)).collect(Collectors.toList());
        if(!result.isEmpty()&&result.size()!=0){
            return getSuccessResponseVO(result);
        }else {
            VideoDanmuQuery videoDanmuQuery = new VideoDanmuQuery();
            videoDanmuQuery.setFileId(fileId);
            videoDanmuQuery.setOrderBy("danmu_id asc");

            return getSuccessResponseVO(videoDanmuService.findListByParam(videoDanmuQuery));
        }
    }

    @RequestMapping("/postDanmu")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO postDanmu(@NotEmpty String videoId,
                                @NotEmpty String fileId,
                                @NotEmpty @Size(max = 200) String text,
                                @NotNull Integer mode,
                                @NotEmpty String color,
                                @NotNull Integer time) {
        VideoDanmu videoDanmu = new VideoDanmu();
        videoDanmu.setVideoId(videoId);
        videoDanmu.setFileId(fileId);
        videoDanmu.setText(text);
        videoDanmu.setMode(mode);
        videoDanmu.setColor(color);
        videoDanmu.setTime(time);
        videoDanmu.setUserId(getTokenUserInfoDto().getUserId());
        videoDanmu.setPostTime(new Date());
        videoDanmuService.saveVideoDanmu(videoDanmu);
        return getSuccessResponseVO(null);
    }
}
