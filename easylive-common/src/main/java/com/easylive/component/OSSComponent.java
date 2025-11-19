package com.easylive.component;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.*;
import com.easylive.entity.config.OssConfig;
import com.easylive.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class OSSComponent {

    @Resource
    private OssConfig ossConfig;

    /**
     * 获取OSS客户端（单例）
     */
    public OSS getOSSClient() {
        return new OSSClientBuilder().build(
                ossConfig.getEndPoint(),
                ossConfig.getAccessKeyId(),
                ossConfig.getAccessKeySecret()
        );
    }

    /**
     * 下载文件
     */
    public void download(String ossKey, String localFilePath) {
        OSS ossClient = getOSSClient();
        try {
            ossClient.getObject(new GetObjectRequest(ossConfig.getBucketName(), ossKey),
                    new File(localFilePath));
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 上传文件
     */
    public void upload(File file, String ossKey) {
        OSS ossClient = getOSSClient();
        try {
            ossClient.putObject(ossConfig.getBucketName(), ossKey, file);
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 检查文件是否存在
     */
    public boolean exists(String ossKey) {
        OSS ossClient = getOSSClient();
        try {
            return ossClient.doesObjectExist(ossConfig.getBucketName(), ossKey);
        } finally {
            ossClient.shutdown();
        }
    }



    /**
     * 删除文件
     */
    public void delete(String ossKey) {
        OSS ossClient = getOSSClient();
        try {
            ossClient.deleteObject(ossConfig.getBucketName(), ossKey);
            log.debug("删除OSS文件: {}", ossKey);
        } catch (Exception e) {
            log.error("删除OSS文件失败: {}", ossKey, e);
            throw new BusinessException("删除文件失败");
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 批量删除文件（更高效）
     */

    public void batchDelete(String ossKeyPrefix) {
        OSS ossClient = getOSSClient();
        try {
            // 列出所有匹配前缀的文件
            String bucketName = ossConfig.getBucketName();
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName);
            listObjectsRequest.setPrefix(ossKeyPrefix);
            listObjectsRequest.setMaxKeys(1000); // 设置最大数量

            ObjectListing objectListing = ossClient.listObjects(listObjectsRequest);
            List<OSSObjectSummary> sums = objectListing.getObjectSummaries();

            // 批量删除所有匹配的文件
            if (!sums.isEmpty()) {
                List<String> keysToDelete = sums.stream()
                        .map(OSSObjectSummary::getKey)
                        .collect(Collectors.toList());

                DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(bucketName)
                        .withKeys(keysToDelete);
                ossClient.deleteObjects(deleteRequest);

                log.debug("删除OSS文件完成，共删除 {} 个文件，前缀: {}", keysToDelete.size(), ossKeyPrefix);
            } else {
                log.debug("未找到匹配的OSS文件，前缀: {}", ossKeyPrefix);
            }

        } catch (Exception e) {
            log.error("删除OSS文件失败: {}", ossKeyPrefix, e);
            throw new BusinessException("删除文件失败");
        } finally {
            ossClient.shutdown();
        }
    }

}
