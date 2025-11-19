package com.easylive.entity.vo;



import java.util.List;

public class VideoInfoResultVo {
    private VideoInfoVo videoInfo;
    private List userActionList;

    public VideoInfoVo getVideoInfo() {
        return videoInfo;
    }

    public void setVideoInfo(VideoInfoVo videoInfo) {
        this.videoInfo = videoInfo;
    }

    public List getUserActionList() {
        return userActionList;
    }

    public void setUserActionList(List userActionList) {
        this.userActionList = userActionList;
    }
}
