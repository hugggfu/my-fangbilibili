package com.easylive.utils;

import com.easylive.entity.config.AppConfig;
import com.easylive.entity.constants.Constants;
import com.easylive.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.math.BigDecimal;

@Component
@Slf4j
public class FFmpegUtils {

    @Resource
    private AppConfig appConfig;


    /**
     * 生成图片缩略图
     *
     * @param filePath
     * @return
     */
    public void createImageThumbnail(String filePath) {
        final String CMD_CREATE_IMAGE_THUMBNAIL = "ffmpeg -i \"%s\" -vf scale=200:-1 \"%s\"";
        String cmd = String.format(CMD_CREATE_IMAGE_THUMBNAIL, filePath, filePath + Constants.IMAGE_THUMBNAIL_SUFFIX);
        ProcessUtils.executeCommand(cmd, appConfig.getShowFFmpegLog());
    }


    /**
     * 获取视频编码
     *
     * @param videoFilePath
     * @return
     */
    public String getVideoCodec(String videoFilePath) {
        final String CMD_GET_CODE = "ffprobe -v error -select_streams v:0 -show_entries stream=codec_name \"%s\"";
        String cmd = String.format(CMD_GET_CODE, videoFilePath);
        String result = ProcessUtils.executeCommand(cmd, appConfig.getShowFFmpegLog());
        result = result.replace("\n", "");
        result = result.substring(result.indexOf("=") + 1);
        String codec = result.substring(0, result.indexOf("["));
        return codec;
    }

    public void convertHevc2Mp4(String newFileName, String videoFilePath) {
        String CMD_HEVC_264 = "ffmpeg -i \"%s\" -c:v libx264 -crf 20 \"%s\" -y";
        String cmd = String.format(CMD_HEVC_264, newFileName, videoFilePath);
        ProcessUtils.executeCommand(cmd, appConfig.getShowFFmpegLog());
    }

    public void  convertVideo2Ts(File tsFolder, String videoFilePath) {
        try {
            log.info("开始视频转码，输出目录: {}", tsFolder.getAbsolutePath());

            // 创建ts子目录
            File tsSubFolder = new File(tsFolder, "ts");
            if (!tsSubFolder.exists()) {
                tsSubFolder.mkdirs();
                log.info("创建TS子目录: {}", tsSubFolder.getAbsolutePath());
            }

            final String CMD_TRANSFER_2TS = "ffmpeg -y -i \"%s\"  -vcodec copy -acodec copy -bsf:v h264_mp4toannexb \"%s\"";
            final String CMD_CUT_TS = "ffmpeg -i \"%s\" -c copy -map 0 -f segment -segment_list \"%s\" -segment_time 10 %s/%%4d.ts";

            String tsPath = tsFolder + "/" + Constants.TS_NAME;

            // 1. 生成.ts临时文件
            String cmd = String.format(CMD_TRANSFER_2TS, videoFilePath, tsPath);
            log.info("执行第一步转码命令: {}", cmd);
            ProcessUtils.executeCommand(cmd, appConfig.getShowFFmpegLog());

            // 2. 生成索引文件.m3u8 和切片.ts到ts子目录
            cmd = String.format(CMD_CUT_TS, tsPath,
                    tsFolder.getPath() + "/" + Constants.M3U8_NAME,
                    tsSubFolder.getPath());  // 重要：输出到ts子目录
            log.info("执行第二步切片命令: {}", cmd);
            ProcessUtils.executeCommand(cmd, appConfig.getShowFFmpegLog());

            // 3. 删除临时的index.ts
            File tempTsFile = new File(tsPath);
            if (tempTsFile.exists()) {
                tempTsFile.delete();
                log.info("删除临时TS文件: {}", tsPath);
            }

            // 4. 验证生成的文件
            File m3u8File = new File(tsFolder, Constants.M3U8_NAME);
            File[] tsFiles = tsSubFolder.listFiles((dir, name) -> name.endsWith(".ts"));

            log.info("转码完成验证:");
            log.info("m3u8文件: {} (存在: {})", m3u8File.getAbsolutePath(), m3u8File.exists());
            log.info("TS目录: {} (存在: {})", tsSubFolder.getAbsolutePath(), tsSubFolder.exists());
            log.info("TS文件数量: {}", tsFiles != null ? tsFiles.length : 0);

            if (tsFiles != null) {
                for (File tsFile : tsFiles) {
                    log.info("  - {} ({} bytes)", tsFile.getName(), tsFile.length());
                }
            }

        } catch (Exception e) {
            log.error("视频转码失败", e);
            throw new BusinessException("视频转码失败: " + e.getMessage());
        }
    }


    public Integer getVideoInfoDuration(String completeVideo) {
        final String CMD_GET_CODE = "ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 \"%s\"";
        String cmd = String.format(CMD_GET_CODE, completeVideo);
        String result = ProcessUtils.executeCommand(cmd, appConfig.getShowFFmpegLog());
        if (StringTools.isEmpty(result)) {
            return 0;
        }
        result = result.replace("\n", "");
        return new BigDecimal(result).intValue();
    }
}
