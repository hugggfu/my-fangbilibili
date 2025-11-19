package com.easylive.service.impl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.easylive.component.EsSearchComponent;
import com.easylive.component.OSSComponent;
import com.easylive.component.RedisComponent;
import com.easylive.entity.config.AppConfig;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.dto.SysSettingDto;
import com.easylive.entity.dto.UploadingFileDto;
import com.easylive.entity.enums.*;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.po.VideoInfoFile;
import com.easylive.entity.po.VideoInfoFilePost;
import com.easylive.entity.query.*;
import com.easylive.exception.BusinessException;
import com.easylive.mappers.VideoInfoFileMapper;
import com.easylive.mappers.VideoInfoFilePostMapper;
import com.easylive.mappers.VideoInfoMapper;
import com.easylive.utils.CopyTools;
import com.easylive.utils.FFmpegUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;

import com.easylive.entity.po.VideoInfoPost;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.mappers.VideoInfoPostMapper;
import com.easylive.service.VideoInfoPostService;
import com.easylive.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;


/**
 * 视频信息 业务接口实现
 */
@Service("videoInfoPostService")
@Slf4j
public class VideoInfoPostServiceImpl implements VideoInfoPostService {

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private VideoInfoPostMapper<VideoInfoPost, VideoInfoPostQuery> videoInfoPostMapper;

    @Resource
    private VideoInfoFilePostMapper<VideoInfoFilePost, VideoInfoFilePostQuery> videoInfoFilePostMapper;

    @Resource
    private VideoInfoMapper<VideoInfo, VideoInfoQuery> videoInfoMapper;

    @Resource
    private VideoInfoFileMapper<VideoInfoFile, VideoInfoFileQuery> videoInfoFileMapper;

    @Resource
    private AppConfig appConfig;

    @Resource
    private OSSComponent ossComponent;

    @Resource
    private FFmpegUtils ffmpegUtils;

    @Resource
    private EsSearchComponent esSearchComponent;
	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<VideoInfoPost> findListByParam(VideoInfoPostQuery param) {
		return this.videoInfoPostMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(VideoInfoPostQuery param) {
		return this.videoInfoPostMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<VideoInfoPost> findListByPage(VideoInfoPostQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<VideoInfoPost> list = this.findListByParam(param);
		PaginationResultVO<VideoInfoPost> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(VideoInfoPost bean) {
		return this.videoInfoPostMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<VideoInfoPost> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.videoInfoPostMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<VideoInfoPost> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.videoInfoPostMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(VideoInfoPost bean, VideoInfoPostQuery param) {
		StringTools.checkParam(param);
		return this.videoInfoPostMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(VideoInfoPostQuery param) {
		StringTools.checkParam(param);
		return this.videoInfoPostMapper.deleteByParam(param);
	}

	/**
	 * 根据VideoId获取对象
	 */
	@Override
	public VideoInfoPost getVideoInfoPostByVideoId(String videoId) {
		return this.videoInfoPostMapper.selectByVideoId(videoId);
	}

	/**
	 * 根据VideoId修改
	 */
	@Override
	public Integer updateVideoInfoPostByVideoId(VideoInfoPost bean, String videoId) {
		return this.videoInfoPostMapper.updateByVideoId(bean, videoId);
	}

	/**
	 * 根据VideoId删除
	 */
	@Override
	public Integer deleteVideoInfoPostByVideoId(String videoId) {
		return this.videoInfoPostMapper.deleteByVideoId(videoId);
	}

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveVideoInfo(VideoInfoPost videoInfoPost, List<VideoInfoFilePost> uploadFileList) {
        if (uploadFileList.size() > redisComponent.getSysSettingDto().getVideoPCount()) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        //判断是否为修改
        if (!StringTools.isEmpty(videoInfoPost.getVideoId())) {
            VideoInfoPost videoInfoPostDb = this.videoInfoPostMapper.selectByVideoId(videoInfoPost.getVideoId());
            if (videoInfoPostDb == null) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
            //状态为转码和待审核时无法修改
            if (ArrayUtils.contains(new Integer[]{VideoStatusEnum.STATUS0.getStatus(), VideoStatusEnum.STATUS2.getStatus()}, videoInfoPostDb.getStatus())) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
        }
        Date curDate = new Date();
        String videoId = videoInfoPost.getVideoId();
        //在修改里面删除了那些文件
        List<VideoInfoFilePost> deleteFileList = new ArrayList<>();
        //在修改里的增加了那些文件
        List<VideoInfoFilePost> addFileList = uploadFileList ;
       //新增
        if(StringTools.isEmpty(videoId)){
            videoId = StringTools.getRandomString(Constants.LENGTH_10);
            videoInfoPost.setVideoId(videoId);
            videoInfoPost.setCreateTime(curDate);
            videoInfoPost.setLastUpdateTime(curDate);
            videoInfoPost.setStatus(VideoStatusEnum.STATUS0.getStatus());
            this.videoInfoPostMapper.insert(videoInfoPost);
        }else {    //修改,说明数据已经入库，视频已经存在VideoInfoFilePost数据库
            //查询已经存在的视频
            VideoInfoFilePostQuery fileQuery = new VideoInfoFilePostQuery();
            fileQuery.setVideoId(videoId);
            fileQuery.setUserId(videoInfoPost.getUserId());
            List<VideoInfoFilePost> dbInfoFileList = this.videoInfoFilePostMapper.selectList(fileQuery);

             // 将数据库结果转换为 Map 便于查找
            Map<String, VideoInfoFilePost> dbInfoMap = dbInfoFileList.stream()
                    .collect(Collectors.toMap(VideoInfoFilePost::getUploadId, Function.identity()));

             // 使用 uploadFileList 构建 Map，但补充数据库中的文件信息
            Map<String, VideoInfoFilePost> uploadFileMap = uploadFileList.stream()
                    .collect(Collectors.toMap(
                            VideoInfoFilePost::getUploadId,
                            uploadItem -> {
                                // 查找对应的数据库记录
                                VideoInfoFilePost dbItem = dbInfoMap.get(uploadItem.getUploadId());
                                if (dbItem != null) {
                                    // 将数据库中的文件信息设置到上传项中
                                    uploadItem.setFilePath(dbItem.getFilePath());
                                    uploadItem.setFileSize(dbItem.getFileSize());
                                    uploadItem.setDuration(dbItem.getDuration());
                                    // 可以设置其他需要的字段...
                                    uploadItem.setTransferResult(dbItem.getTransferResult());
                                }
                                return uploadItem;
                            },
                            (data1, data2) -> data2
                    ));
            //删除的文件 -> 数据库中有，uploadFileList没有
            Boolean updateFileName = false;
            for (VideoInfoFilePost fileInfo : dbInfoFileList) {
                VideoInfoFilePost updateFile = uploadFileMap.get(fileInfo.getUploadId());
                if (updateFile == null) {
                    deleteFileList.add(fileInfo);
                } else if (!updateFile.getFileName().equals(fileInfo.getFileName())) {
                    updateFileName = true;
                }
            }
            //新增的文件  没有fileId就是新增的文件
            addFileList = uploadFileList.stream().filter(item -> item.getFileId() == null).collect(Collectors.toList());
            videoInfoPost.setLastUpdateTime(curDate);

            //判断视频信息是否有更改
            Boolean changeVideoInfo = this.changeVideoInfo(videoInfoPost);
            if (!addFileList.isEmpty()) {
                videoInfoPost.setStatus(VideoStatusEnum.STATUS0.getStatus());
            } else if (changeVideoInfo || updateFileName) {
                videoInfoPost.setStatus(VideoStatusEnum.STATUS2.getStatus());
            } else if (addFileList.isEmpty()&&deleteFileList.isEmpty()&& !changeVideoInfo && !updateFileName) {
                throw new BusinessException("请选择你要修改的内容");
            }
            this.videoInfoPostMapper.updateByVideoId(videoInfoPost, videoInfoPost.getVideoId());
        }
        //清除已经删除的数据
        if (!deleteFileList.isEmpty()) {
            List<String> delFileIdList = deleteFileList.stream().map(item -> item.getFileId()).collect(Collectors.toList());
            this.videoInfoFilePostMapper.deleteBatchByFileId(delFileIdList, videoInfoPost.getUserId());
            //将要删除的视频加入消息队列
            List<String> delFilePathList = deleteFileList.stream().map(item -> item.getFilePath()).collect(Collectors.toList());
            redisComponent.addFile2DelQueue(videoId, delFilePathList);
        }

        //更新视频信息
        Integer index = 1;
        for (VideoInfoFilePost videoInfoFile : uploadFileList) {
            videoInfoFile.setFileIndex(index++);
            videoInfoFile.setVideoId(videoId);
            videoInfoFile.setUserId(videoInfoPost.getUserId());
            if (videoInfoFile.getFileId() == null) {
                videoInfoFile.setFileId(StringTools.getRandomString(Constants.LENGTH_20));
                videoInfoFile.setUpdateType(VideoFileUpdateTypeEnum.UPDATE.getStatus());
                videoInfoFile.setTransferResult(VideoFileTransferResultEnum.TRANSFER.getStatus());
            }
        }
        this.videoInfoFilePostMapper.insertOrUpdateBatch(uploadFileList);


        //将需要转码的视频加入队列
        if (!addFileList.isEmpty()) {
            for (VideoInfoFilePost file : addFileList) {
                file.setUserId(videoInfoPost.getUserId());
                file.setVideoId(videoId);
            }
            redisComponent.addFile2TransferQueue(addFileList);
        }
    }

    @Override
    public void transferVideoFile(VideoInfoFilePost videoInfoFile) {
        String localTempDir = null;
        VideoInfoFilePost updateFilePost = new VideoInfoFilePost();
        try {
            UploadingFileDto fileDto = redisComponent.getUploadingVideoFile(videoInfoFile.getUserId(), videoInfoFile.getUploadId());
            // 1. 创建本地临时工作目录
            localTempDir = createTempWorkDir(fileDto.getFilePath());

            // 2. 从OSS下载所有分片到本地
            downloadAllChunksFromOSS(fileDto, localTempDir);

            // 3. 合并分片文件
            String mergedVideoPath = mergeChunksLocally(fileDto, localTempDir);

            // 4. 获取视频信息并处理
            processVideoFile(fileDto, mergedVideoPath, updateFilePost);

            // 5. 上传处理结果到OSS
            uploadProcessedFiles(fileDto, localTempDir);

            // 6. 重要：删除OSS上的原始分片文件
            cleanupOssChunks(fileDto);

            // 7.重要：确保清理本地临时文件
            cleanupTempFiles(localTempDir,videoInfoFile);

        }catch (Exception e){
            log.error("文件转码失败",e);
            updateFilePost.setTransferResult(VideoFileTransferResultEnum.FAIL.getStatus());
        }finally {
            //更新文件状态
            videoInfoFilePostMapper.updateByUploadIdAndUserId(updateFilePost, videoInfoFile.getUploadId(), videoInfoFile.getUserId());
            //更新视频信息
            VideoInfoFilePostQuery fileQuery = new VideoInfoFilePostQuery();
            fileQuery.setVideoId(videoInfoFile.getVideoId());
            fileQuery.setTransferResult(VideoFileTransferResultEnum.FAIL.getStatus());
            Integer failCount = videoInfoFilePostMapper.selectCount(fileQuery);
            if (failCount > 0) {
                VideoInfoPost videoUpdate = new VideoInfoPost();
                videoUpdate.setStatus(VideoStatusEnum.STATUS1.getStatus());
                videoInfoPostMapper.updateByVideoId(videoUpdate, videoInfoFile.getVideoId());
                return;
            }
            fileQuery.setTransferResult(VideoFileTransferResultEnum.TRANSFER.getStatus());
            Integer transferCount = videoInfoFilePostMapper.selectCount(fileQuery);
            if (transferCount == 0) {
                Integer duration = videoInfoFilePostMapper.sumDuration(videoInfoFile.getVideoId());
                VideoInfoPost videoUpdate = new VideoInfoPost();
                videoUpdate.setStatus(VideoStatusEnum.STATUS2.getStatus());
                videoUpdate.setDuration(duration);
                videoInfoPostMapper.updateByVideoId(videoUpdate, videoInfoFile.getVideoId());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditVideo(String videoId, Integer status, String reason) {
        VideoStatusEnum videoStatusEnum = VideoStatusEnum.getByStatus(status);
        if (videoStatusEnum == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        VideoInfoPost videoInfoPost = new VideoInfoPost();
        videoInfoPost.setStatus(status);

        VideoInfoPostQuery videoInfoPostQuery = new VideoInfoPostQuery();
        videoInfoPostQuery.setStatus(VideoStatusEnum.STATUS2.getStatus());
        videoInfoPostQuery.setVideoId(videoId);
        Integer audioCount = this.videoInfoPostMapper.updateByParam(videoInfoPost, videoInfoPostQuery);
        if (audioCount == 0) {
            throw new BusinessException("审核失败，请稍后重试");
        }
        /**
         * 更新视频状态
         */

        VideoInfoFilePost videoInfoFilePost = new VideoInfoFilePost();
        videoInfoFilePost.setUpdateType(VideoFileUpdateTypeEnum.NO_UPDATE.getStatus());

        VideoInfoFilePostQuery filePostQuery = new VideoInfoFilePostQuery();
        filePostQuery.setVideoId(videoId);
        this.videoInfoFilePostMapper.updateByParam(videoInfoFilePost, filePostQuery);

        if (VideoStatusEnum.STATUS4 == videoStatusEnum) {
            return;
        }
        VideoInfoPost infoPost = this.videoInfoPostMapper.selectByVideoId(videoId);
        /**
         * 第一次发布增加用户积分
         */
        VideoInfo dbVideoInfo = this.videoInfoMapper.selectByVideoId(videoId);
        if (dbVideoInfo == null) {
            SysSettingDto sysSettingDto = redisComponent.getSysSettingDto();

        }

        /**
         * 将发布信息复制到正式表信息
         */
        VideoInfo videoInfo = CopyTools.copy(infoPost, VideoInfo.class);
        this.videoInfoMapper.insertOrUpdate(videoInfo);

        /**
         * 更新视频信息 先删除再添加
         */
        VideoInfoFileQuery videoInfoFileQuery = new VideoInfoFileQuery();
        videoInfoFileQuery.setVideoId(videoId);
        this.videoInfoFileMapper.deleteByParam(videoInfoFileQuery);


        /**
         * 查询发布表中的视频信息
         */
        VideoInfoFilePostQuery videoInfoFilePostQuery = new VideoInfoFilePostQuery();
        videoInfoFilePostQuery.setVideoId(videoId);
        List<VideoInfoFilePost> videoInfoFilePostList = this.videoInfoFilePostMapper.selectList(videoInfoFilePostQuery);

        List<VideoInfoFile> videoInfoFileList = CopyTools.copyList(videoInfoFilePostList, VideoInfoFile.class);
        this.videoInfoFileMapper.insertBatch(videoInfoFileList);

        /**
         * 删除文件
         */
        List<String> filePathList = redisComponent.getDelFileList(videoId);
        if (filePathList != null) {
            for (String path : filePathList) {
                File file = new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER +"/oss_processing/" + path);
                if (file.exists()) {
                    try {
                        FileUtils.deleteDirectory(file);
                    } catch (IOException e) {
                        log.error("删除文件失败", e);
                    }
                }
            }
        }
        redisComponent.cleanDelFileList(videoId);

        /**
         * 保存信息到es
         */

        esSearchComponent.saveDoc(videoInfo);

    }

    /**
     * 创建临时工作目录
     */
    private String createTempWorkDir(String filePath) {
        String tempDir = appConfig.getProjectFolder() + Constants.FILE_FOLDER_TEMP + "/oss_processing/" + filePath;
        File dir = new File(tempDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return tempDir;
    }
    /**
     * 从OSS下载所有分片
     */
    private void downloadAllChunksFromOSS(UploadingFileDto fileDto, String localDir) {
        for (int i = 0; i < fileDto.getChunks(); i++) {
            String ossKey = fileDto.getFilePath() + "/" + i;
            String localPath = localDir + "/" + i;
            ossComponent.download(ossKey, localPath);
        }
    }

    /**
     * 本地合并分片
     */
    private String mergeChunksLocally(UploadingFileDto fileDto, String localDir) throws BusinessException {
        String mergedVideoPath = localDir  + Constants.TEMP_VIDEO_NAME;
        // 复用您原有的union方法
        this.union(localDir, mergedVideoPath, true);
        return mergedVideoPath;
    }

    public static void union(String dirPath, String toFilePath, boolean delSource) throws BusinessException {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            throw new BusinessException("目录不存在");
        }
        File fileList[] = dir.listFiles();
        File targetFile = new File(toFilePath);
        try (RandomAccessFile writeFile = new RandomAccessFile(targetFile, "rw")) {
            byte[] b = new byte[1024 * 10];
            for (int i = 0; i < fileList.length; i++) {
                int len = -1;
                //创建读块文件的对象
                File chunkFile = new File(dirPath + File.separator + i);
                RandomAccessFile readFile = null;
                try {
                    readFile = new RandomAccessFile(chunkFile, "r");
                    while ((len = readFile.read(b)) != -1) {
                        writeFile.write(b, 0, len);
                    }
                } catch (Exception e) {
                    log.error("合并分片失败", e);
                    throw new BusinessException("合并文件失败");
                } finally {
                    readFile.close();
                }
            }
        } catch (Exception e) {
            throw new BusinessException("合并文件" + dirPath + "出错了");
        } finally {
            if (delSource) {
                for (int i = 0; i < fileList.length; i++) {
                    fileList[i].delete();
                }
            }
        }
    }

    /**
     * 处理视频文件（转码、切片等）
     */
    private void processVideoFile(UploadingFileDto fileDto, String videoPath, VideoInfoFilePost updateFilePost) {
        // 获取视频时长
        Integer duration = ffmpegUtils.getVideoInfoDuration(videoPath);
        updateFilePost.setDuration(duration);
        updateFilePost.setFileSize(new File(videoPath).length());
        updateFilePost.setFilePath(fileDto.getFilePath());
        updateFilePost.setTransferResult(VideoFileTransferResultEnum.SUCCESS.getStatus());
        // 视频转码和切片（复用您原有的convertVideo2Ts逻辑）
        this.convertVideo2Ts(videoPath);
    }

    private void convertVideo2Ts(String videoFilePath) {
        File videoFile = new File(videoFilePath);
        //创建同名切片目录
        File tsFolder = videoFile.getParentFile();
        String codec = ffmpegUtils.getVideoCodec(videoFilePath);
        //转码
        if (Constants.VIDEO_CODE_HEVC.equals(codec)) {
            String tempFileName = videoFilePath + Constants.VIDEO_CODE_TEMP_FILE_SUFFIX;
            new File(videoFilePath).renameTo(new File(tempFileName));
            ffmpegUtils.convertHevc2Mp4(tempFileName, videoFilePath);
            new File(tempFileName).delete();
        }

        //视频转为ts
        ffmpegUtils.convertVideo2Ts(tsFolder, videoFilePath);

        //删除视频文件
        videoFile.delete();
    }
    /**
     * 上传处理后的文件到OSS - 基于您现有代码的修正版本
     */
    private void uploadProcessedFiles(UploadingFileDto fileDto, String localDir) {
        try {
            log.info("开始上传处理后的文件，本地目录: {}", localDir);

            // 1. 首先检查目录结构
            File localDirFile = new File(localDir);
            if (!localDirFile.exists()) {
                log.error("本地目录不存在: {}", localDir);
                return;
            }

            log.info("本地目录内容:");
            File[] allFiles = localDirFile.listFiles();
            if (allFiles != null) {
                for (File file : allFiles) {
                    log.info("  - {} (目录: {})", file.getName(), file.isDirectory());
                    if (file.isDirectory() && "ts".equals(file.getName())) {
                        File[] tsFiles = file.listFiles();
                        log.info("    TS文件数量: {}", tsFiles != null ? tsFiles.length : 0);
                        if (tsFiles != null) {
                            for (File tsFile : tsFiles) {
                                log.info("      - {} ({} bytes)", tsFile.getName(), tsFile.length());
                            }
                        }
                    }
                }
            }

            // 2. 上传TS切片文件（从ts子目录）
            File tsDir = new File(localDir + "/ts");
            if (tsDir.exists() && tsDir.isDirectory()) {
                File[] tsFiles = tsDir.listFiles((dir, name) -> name.endsWith(".ts"));
                if (tsFiles != null && tsFiles.length > 0) {
                    log.info("开始上传 {} 个TS文件", tsFiles.length);
                    for (File tsFile : tsFiles) {
                        String ossKey = fileDto.getFilePath() + "/ts/" + tsFile.getName();
                        ossComponent.upload(tsFile, ossKey);
                        log.info(" 上传TS文件: {} -> {}", tsFile.getName(), ossKey);
                    }
                } else {
                    log.warn("TS目录为空: {}", tsDir.getAbsolutePath());
                }
            } else {
                log.warn("TS目录不存在: {}", tsDir.getAbsolutePath());
            }

            // 3. 上传m3u8文件
            File[] m3u8Files = new File(localDir).listFiles((dir, name) -> name.endsWith(".m3u8"));
            if (m3u8Files != null && m3u8Files.length > 0) {
                for (File m3u8File : m3u8Files) {
                    String ossKey = fileDto.getFilePath() + "/" + m3u8File.getName();
                    ossComponent.upload(m3u8File, ossKey);
                    log.info("✅ 上传m3u8文件: {} -> {}", m3u8File.getName(), ossKey);
                }
            } else {
                log.warn("未找到m3u8文件");
            }

            // 4. 上传MP4文件（如果有）
            File[] mp4Files = new File(localDir).listFiles((dir, name) ->
                    name.endsWith(".mp4") && !name.contains("converted") && !name.contains("temp")
            );
            if (mp4Files != null && mp4Files.length > 0) {
                for (File mp4File : mp4Files) {
                    String ossKey = fileDto.getFilePath() + "/video.mp4";
                    ossComponent.upload(mp4File, ossKey);
                    log.info("上传MP4文件: {} -> {}", mp4File.getName(), ossKey);
                    break; // 只上传第一个
                }
            }

            // 5. 上传封面图（如果有）
            File[] coverFiles = new File(localDir).listFiles((dir, name) ->
                    name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png")
            );
            if (coverFiles != null && coverFiles.length > 0) {
                for (File coverFile : coverFiles) {
                    String ossKey = fileDto.getFilePath() + "/cover.jpg";
                    ossComponent.upload(coverFile, ossKey);
                    log.info("✅ 上传封面图: {} -> {}", coverFile.getName(), ossKey);
                    break; // 只上传第一个
                }
            }

            log.info("文件上传完成");

        } catch (Exception e) {
            log.error("上传文件到OSS失败", e);
            throw new BusinessException("上传处理后的文件失败");
        }
    }

    /**
     * 清理临时文件
     */
    private void cleanupTempFiles(String localTempDir, VideoInfoFilePost videoInfoFile) {
        if (localTempDir != null) {
            try {
                File directory = new File(localTempDir);
                // 记录父目录路径，用于后续检查
                File parentDir = directory.getParentFile();
                FileUtils.deleteDirectory(new File(localTempDir));
                //递归清理空父目录
                cleanEmptyParentDirectories(parentDir);
            } catch (IOException e) {
                log.warn("清理临时文件失败: {}", localTempDir, e);
            }finally {
                redisComponent.delVideoFileInfo(videoInfoFile.getUserId(), videoInfoFile.getUploadId());
            }
        }
    }

    private void cleanEmptyParentDirectories(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return;
        }
        // 检查目录是否为空
        String[] files = directory.list();
        if (files != null && files.length == 0) {
            // 目录为空，删除它
            boolean deleted = directory.delete();
            if (deleted) {
                System.out.println("删除空目录: " + directory.getAbsolutePath());

                // 继续检查上一级目录
                cleanEmptyParentDirectories(directory.getParentFile());
            }
        }
    }

    /**
     * 清理OSS上的分片文件
     */
    private void cleanupOssChunks(UploadingFileDto fileDto) {
        try {
            log.info("开始清理OSS分片文件，路径: {}", fileDto.getFilePath());
            int deletedCount = 0;

            for (int i = 0; i < fileDto.getChunks(); i++) {
                String chunkKey = fileDto.getFilePath() + "/" + i;
                if (ossComponent.exists(chunkKey)) {
                    ossComponent.delete(chunkKey);
                    deletedCount++;
                    log.info("✅ 删除分片文件: {}", chunkKey);
                }
            }

            log.info("分片文件清理完成，共删除 {} 个文件", deletedCount);

        } catch (Exception e) {
            // 分片文件删除失败不应该影响主要流程，只记录警告
            log.warn("清理分片文件失败，但不影响主流程", e);
        }
    }
    private boolean changeVideoInfo(VideoInfoPost videoInfoPost) {
        VideoInfoPost dbInfo = this.videoInfoPostMapper.selectByVideoId(videoInfoPost.getVideoId());
        //标题，封面，标签，简介
        if (!videoInfoPost.getVideoCover().equals(dbInfo.getVideoCover()) || !videoInfoPost.getVideoName().equals(dbInfo.getVideoName()) || !videoInfoPost.getTags().equals(dbInfo.getTags()) || !videoInfoPost.getIntroduction().equals(
                dbInfo.getIntroduction())) {
            return true;
        }
        return false;
    }
}