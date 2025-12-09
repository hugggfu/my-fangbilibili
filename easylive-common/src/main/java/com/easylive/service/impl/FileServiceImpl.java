package com.easylive.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PutObjectResult;
import com.easylive.entity.config.OssConfig;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.enums.ResponseCodeEnum;
import com.easylive.entity.po.VideoInfoFile;
import com.easylive.exception.BusinessException;
import com.easylive.mappers.VideoInfoFileMapper;
import com.easylive.service.FileService;
import com.easylive.utils.FFmpegUtils;
import com.easylive.utils.StringTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static jdk.nashorn.internal.runtime.regexp.joni.Config.log;

@Service
@Slf4j
public class FileServiceImpl implements FileService {

    @Resource
    private OssConfig ossConfig;

    @Resource
    private FFmpegUtils fFmpegUtils;

    @Resource
    private VideoInfoFileMapper videoInfoFileMapper;

    @Override
    public String uploadImage(MultipartFile file, Boolean createThumbnail) {
        // 1. 获取 OSS 配置
        String bucketName = ossConfig.getBucketName();
        String endPoint = ossConfig.getEndPoint();
        String accessKeyId = ossConfig.getAccessKeyId();
        String accessKeySecret = ossConfig.getAccessKeySecret();

        // 2. 创建 OSS 客户端
        OSS ossClient = new OSSClientBuilder().build(endPoint, accessKeyId, accessKeySecret);

        try {
            // 3. 获取原始文件名
            String originalFilename = file.getOriginalFilename();
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
            String folder = formatter.format(now);

            // 4. 生成唯一文件名
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = StringTools.getRandomString(Constants.LENGTH_30) + extension;

            // 5. OSS 对象 Key
            String objectKey = "user/" + folder + "/" + fileName;

            // 6. 上传文件到 OSS
            ossClient.putObject(bucketName, objectKey, file.getInputStream());

            // 7. 拼接返回 URL
            String fileUrl = "https://" + bucketName + "." + endPoint + "/" + objectKey;

            // 8. 如果需要缩略图，直接拼接 OSS 图片处理参数
            if (createThumbnail != null && createThumbnail) {
                fileUrl += "?x-oss-process=image/resize,w_200";
            }

            return fileUrl;

        } catch (IOException e) {
            throw new RuntimeException("上传文件到 OSS 失败", e);
        } finally {
            // 9. 关闭 OSS 客户端
            ossClient.shutdown();
        }
    }

    @Override
    public void getResource(HttpServletResponse response, String sourceName) {
        if (!StringTools.pathIsOk(sourceName)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        // ⚠️ 如果 sourceName 是 URL，需要把 URL 前缀去掉，只保留 Key
        String bucketDomain = "https://" + ossConfig.getBucketName() + "." + ossConfig.getEndPoint() + "/";
        if (sourceName.startsWith(bucketDomain)) {
            sourceName = sourceName.replace(bucketDomain, "");
        }

        readFile(response, sourceName);

    }

    protected void readFile(HttpServletResponse response, String key) {
        String bucketName = ossConfig.getBucketName();
        String endPoint = ossConfig.getEndPoint();
        String accessKeyId = ossConfig.getAccessKeyId();
        String accessKeySecret = ossConfig.getAccessKeySecret();

        OSS ossClient = new OSSClientBuilder().build(endPoint, accessKeyId, accessKeySecret);
        try {
            OSSObject ossObject = ossClient.getObject(bucketName, key);

            try (InputStream in = ossObject.getObjectContent();
                    OutputStream out = response.getOutputStream()) {

                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                out.flush();
                log.info("文件传输完成: {}", key);
            }
        } catch (Exception e) {
            log.error("读取OSS视频文件异常", e);
            response.setStatus(500);
        } finally {
            ossClient.shutdown();
        }
    }
}
