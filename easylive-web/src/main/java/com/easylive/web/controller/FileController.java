package com.easylive.web.controller;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.easylive.component.OSSComponent;
import com.easylive.component.RedisComponent;
import com.easylive.entity.config.OssConfig;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.dto.SysSettingDto;
import com.easylive.entity.dto.TokenUserInfoDto;
import com.easylive.entity.dto.UploadingFileDto;
import com.easylive.entity.dto.VideoPlayInfoDto;
import com.easylive.entity.enums.DateTimePatternEnum;
import com.easylive.entity.enums.ResponseCodeEnum;
import com.easylive.entity.po.VideoInfoFile;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.exception.BusinessException;
import com.easylive.service.FileService;
import com.easylive.service.VideoInfoFileService;
import com.easylive.utils.DateUtil;
import com.easylive.utils.StringTools;
import com.easylive.web.annotation.GlobalInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/file")
@Validated
@Slf4j
public class FileController extends ABaseController{
    @Resource
    private FileService fileService;

    @Resource
    private RedisComponent  redisComponent;

    @Resource
    private OssConfig ossConfig;

    @Resource
    private OSSComponent ossComponent;

    @Resource
    private VideoInfoFileService videoInfoFileService;


    @RequestMapping("/getResource")
    @GlobalInterceptor
    public void getResource(HttpServletResponse response, @NotEmpty String sourceName){
        String suffix = StringTools.getFileSuffix(sourceName);
        response.setContentType("image/" + suffix.replace(".", ""));
        response.setHeader("Cache-Control", "max-age=2592000");
         fileService.getResource(response, sourceName);

    }

    @RequestMapping("/preUploadVideo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO preUploadVideo(@NotEmpty String fileName,@NotNull Integer chunks){
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        String uploadId = redisComponent.savePreVideoFileInfo(tokenUserInfoDto.getUserId(), fileName, chunks);
        return getSuccessResponseVO(uploadId);

    }

    @RequestMapping("/uploadVideo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO uploadVideo(@NotNull MultipartFile chunkFile,@NotNull Integer chunkIndex,@NotEmpty String uploadId) throws IOException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        UploadingFileDto fileDto = redisComponent.getUploadingVideoFile(tokenUserInfoDto.getUserId(), uploadId);
        if(fileDto == null){
            throw new BusinessException("文件不存在请重新上传");
        }
        SysSettingDto sysSettingDto = redisComponent.getSysSettingDto();
        if (fileDto.getFileSize() > sysSettingDto.getVideoSize() * Constants.MB_SIZE) {
            throw new BusinessException("文件超过最大文件限制");
        }
        //判断分片
        if ((chunkIndex - 1) > fileDto.getChunkIndex() || chunkIndex > fileDto.getChunks() - 1) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        // 1. 获取 OSS 配置
        String bucketName = ossConfig.getBucketName();
        String endPoint = ossConfig.getEndPoint();
        String accessKeyId = ossConfig.getAccessKeyId();
        String accessKeySecret = ossConfig.getAccessKeySecret();
        // 2. 创建 OSS 客户端
        OSS ossClient = new OSSClientBuilder().build(endPoint, accessKeyId, accessKeySecret);

        try {
    // 7. 拼接返回 URL
    String objectKey = fileDto.getFilePath()+"/" + chunkIndex; ;

    // 6. 上传文件到 OSS
    ossClient.putObject(bucketName, objectKey, chunkFile.getInputStream());

    // 7. 拼接返回 URL
    String fileUrl = "https://" + bucketName + "." + endPoint + "/" + objectKey;
    //记录文件上传的分片数
    fileDto.setChunkIndex(chunkIndex);
    fileDto.setFileSize(fileDto.getFileSize() + chunkFile.getSize());
    redisComponent.updateVideoFileInfo(tokenUserInfoDto.getUserId(), fileDto);
    return getSuccessResponseVO(null);
     }finally {
    // 9. 关闭 OSS 客户端
    ossClient .shutdown();
   }
  }

    @RequestMapping("/delUploadVideo")
    public ResponseVO delUploadVideo(@NotEmpty String uploadId) throws IOException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        UploadingFileDto fileDto = redisComponent.getUploadingVideoFile(tokenUserInfoDto.getUserId(), uploadId);
        if (fileDto == null) {
            throw new BusinessException("文件不存在请重新上传");
        }
        redisComponent.delVideoFileInfo(tokenUserInfoDto.getUserId(), uploadId);

        String bucketName = ossConfig.getBucketName();
        String endPoint = ossConfig.getEndPoint();
        String accessKeyId = ossConfig.getAccessKeyId();
        String accessKeySecret = ossConfig.getAccessKeySecret();
        OSS ossClient = new OSSClientBuilder().build(endPoint, accessKeyId, accessKeySecret);

        try {
            // 方法1：删除目录下的所有对象（推荐）
            String prefix = fileDto.getFilePath() + "/";
            ObjectListing objectListing = ossClient.listObjects(bucketName, prefix);
            List<OSSObjectSummary> objectSummaries = objectListing.getObjectSummaries();

            for (OSSObjectSummary objectSummary : objectSummaries) {
                ossClient.deleteObject(bucketName, objectSummary.getKey());
            }

            // 如果还有更多对象，继续删除
            while (objectListing.isTruncated()) {
                objectListing = ossClient.listObjects(new ListObjectsRequest(bucketName)
                        .withPrefix(prefix)
                        .withMarker(objectListing.getNextMarker()));
                objectSummaries = objectListing.getObjectSummaries();
                for (OSSObjectSummary objectSummary : objectSummaries) {
                    ossClient.deleteObject(bucketName, objectSummary.getKey());
                }
            }

            return getSuccessResponseVO(uploadId);
        } catch (Exception e) {
            throw new RuntimeException("文件删除失败", e);
        } finally {
            ossClient.shutdown();
        }
    }


    @RequestMapping("/uploadImage")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO uploadCover(@NotNull MultipartFile file, @NotNull Boolean createThumbnail){
        String s = fileService.uploadImage(file, createThumbnail);
        return getSuccessResponseVO(s);

    }

    @RequestMapping("/videoResource/{fileId}")
    @GlobalInterceptor
    public void getVideoResource(HttpServletResponse response, @PathVariable @NotEmpty String fileId) {
        VideoInfoFile videoInfoFile = videoInfoFileService.getVideoInfoFileByFileId(fileId);
        String filePath = videoInfoFile.getFilePath();
        fileService.getResource(response, filePath + "/" + Constants.M3U8_NAME);

        //更新视频的阅读信息
        VideoPlayInfoDto videoPlayInfoDto = new VideoPlayInfoDto();
        videoPlayInfoDto.setVideoId(videoInfoFile.getVideoId());
        videoPlayInfoDto.setFileIndex(videoInfoFile.getFileIndex());

        TokenUserInfoDto tokenUserInfoDto = getTokenInfoFromCookie();
        if (tokenUserInfoDto != null) {
            videoPlayInfoDto.setUserId(tokenUserInfoDto.getUserId());
        }
        redisComponent.addVideoPlay(videoPlayInfoDto);

    }

    @RequestMapping("/videoResource/{fileId}/{ts}")
    @GlobalInterceptor
    public void getVideoResourceTs(HttpServletResponse response, @PathVariable @NotEmpty String fileId, @PathVariable @NotNull String ts) {
        VideoInfoFile videoInfoFile = videoInfoFileService.getVideoInfoFileByFileId(fileId);
        String filePath = videoInfoFile.getFilePath();
        fileService.getResource(response, filePath + "/" + "ts/"+ts);

    }


}
