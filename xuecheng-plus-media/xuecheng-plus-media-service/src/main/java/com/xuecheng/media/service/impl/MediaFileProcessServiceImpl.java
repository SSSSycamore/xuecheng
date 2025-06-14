package com.xuecheng.media.service.impl;

import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessHistoryMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.model.po.MediaProcessHistory;
import com.xuecheng.media.service.MediaFileProcessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaFileProcessServiceImpl implements MediaFileProcessService {
    private final MediaProcessMapper mediaProcessMapper;
    private final MediaFilesMapper mediaFilesMapper;
    private final MediaProcessHistoryMapper mediaProcessHistoryMapper;
    @Override
    public List<MediaProcess> getMediaProcessList(int shardIndex, int shardTotal, int count) {
        return mediaProcessMapper.selectListByShardIndex(shardIndex, shardTotal, count);
    }

    public boolean startTask(long id) {
        int result = mediaProcessMapper.startTask(id);
        return result<=0?false:true;
    }

    @Override
    @Transactional
    public void saveProcessFinishStatus(Long taskId, String status, String fileId, String url, String errorMsg) {
        MediaProcess mediaProcess = mediaProcessMapper.selectById(taskId);
        if (mediaProcess == null) {
            return; // 任务不存在
        }
        // 处理失败
        if (status.equals("3")){
            mediaProcess.setStatus("3");
            mediaProcess.setFailCount(mediaProcess.getFailCount() + 1);
            mediaProcess.setErrormsg(errorMsg);
            mediaProcessMapper.updateById(mediaProcess);
            log.error("任务处理失败,errorMsg:{}",errorMsg);
            return;
        }
        // 处理成功
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileId);
        if (mediaFiles != null) {
            mediaFiles.setUrl(url);
            mediaFilesMapper.updateById(mediaFiles);
        }
        // 更新处理任务状态
        mediaProcess.setStatus("2");
        mediaProcess.setUrl(url);
        mediaProcess.setFinishDate(LocalDateTime.now());
        mediaProcessMapper.updateById(mediaProcess);

        // 添加到历史记录
        MediaProcessHistory mediaProcessHistory = new MediaProcessHistory();
        BeanUtils.copyProperties(mediaProcess, mediaProcessHistory);
        mediaProcessHistoryMapper.insert(mediaProcessHistory); // 保存处理记录
        // 删除处理任务
        mediaProcessMapper.deleteById(taskId);
    }
}
