package com.xuecheng.media.jobhandler;

import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileProcessService;
import com.xuecheng.media.service.MediaFileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

@Component
@Slf4j
public class VideoTask {
    @Autowired
    private MediaFileProcessService mediaProcessService;
    @Autowired
    private MediaFileService mediaFileService;
    @Value("${videoprocess.ffmpegpath}")
    private String ffmpeg_path;

    @XxlJob("VideoJobHandler")
    public void videoJobHandler() throws Exception {
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        int processors = Runtime.getRuntime().availableProcessors();
        // 获取任务列表
        List<MediaProcess> taskList = mediaProcessService.getMediaProcessList(shardIndex, shardTotal, processors);
        int size = taskList.size();
        log.info("获取任务数:{}",size);
        if (size <= 0) {
            log.info("没有需要处理的任务");
            return;
        }

        // 如果任务数大于处理器数量，则使用处理器数量作为线程池大小
        CountDownLatch countDownLatch = new CountDownLatch(size);

        // 创建线程池
        ExecutorService pool = Executors.newFixedThreadPool(size);

        // 遍历任务列表
        taskList.stream().forEach(mediaProcess -> {
            pool.execute(()->{
                try {
                    // 启动任务
                    boolean b = mediaProcessService.startTask(mediaProcess.getId());
                    if (!b){
                        log.info("抢占任务失败，任务id:{}",mediaProcess.getId());
                        return;
                    }
                    // 准备转码参数
                    String fileId = mediaProcess.getFileId();
                    String filename = mediaProcess.getFilename();
                    // 从minio下载文件并转存本地
                    File file = mediaFileService.downloadFileFromMinIO(mediaProcess.getBucket(), mediaProcess.getFilePath());
                    if (file == null || !file.exists()) {
                        log.error("下载文件失败，任务id:{},文件路径:{}", mediaProcess.getId(), mediaProcess.getBucket() + mediaProcess.getFilePath());
                        mediaProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", fileId, null, "下载文件失败");
                        return;
                    }
                    File tempFile = null;
                    try {
                        tempFile = File.createTempFile("mp4", ".mp4");
                    } catch (IOException e) {
                        log.error("创建临时文件失败: {}", e.getMessage());
                        mediaProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", fileId, null, "创建临时文件失败");
                    }
                    String bucket = mediaProcess.getBucket(); // 存储桶
                    String video_path = file.getAbsolutePath(); // 视频文件路径
                    String mp4_name = tempFile.getName(); // 转码后文件名
                    String mp4folder_path = tempFile.getAbsolutePath(); // 转码后文件存储路径
                    // 转码
                    log.info("开始处理视频，任务id:{}", mediaProcess.getId());
                    Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpeg_path,video_path,mp4_name,mp4folder_path);
                    String result = videoUtil.generateMp4();
                    if (!result.equals("success")){
                        log.error("视频转码失败，任务id:{}, 错误信息:{}", mediaProcess.getId(), result);
                        mediaProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", fileId, null, result);
                        return;
                    }
                    String objectName = getFilePathByMd5(mediaProcess.getFileId(), ".mp4");
                    String url = "/" + bucket + "/" + objectName;
                    // 上传转码后的视频到minio
                    try {
                        mediaFileService.addMediaFilesToMinIO(tempFile.getAbsolutePath(), "video/mp4", mediaProcess.getBucket(), objectName);
                        mediaProcessService.saveProcessFinishStatus(mediaProcess.getId(), "2", fileId, url, null);
                    } catch (Exception e) {
                        log.error("处理后视频转码或入库失败，任务id:{}, 错误信息:{}", mediaProcess.getId(), e.getMessage());
                        mediaProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", fileId, null, "处理后视频转码或入库失败");
                        return;
                    }
                    log.info("视频处理完成，任务id:{}", mediaProcess.getId());
                } finally {
                    countDownLatch.countDown();
                }
            });
        });
        // 等待所有任务完成
        countDownLatch.await(30, TimeUnit.MINUTES);
    }

    private String getFilePathByMd5(String fileMd5,String fileExt){
        return fileMd5.charAt(0) + "/" + fileMd5.charAt(1) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }
}
