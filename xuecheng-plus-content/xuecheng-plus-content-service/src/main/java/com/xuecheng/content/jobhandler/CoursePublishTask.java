package com.xuecheng.content.jobhandler;

import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.feignclient.CourseIndex;
import com.xuecheng.content.feignclient.SearchServiceClient;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Objects;

@Slf4j
@Component
public class CoursePublishTask extends MessageProcessAbstract {
    @Autowired
    MqMessageService mqMessageService;

    @Autowired
    CoursePublishService coursePublishService;

    @Autowired
    SearchServiceClient searchServiceClient;

    @Autowired
    CoursePublishMapper coursePublishMapper;
    /**
     * 任务处理
     *
     * @param mqMessage 执行任务内容
     * @return boolean true:处理成功，false处理失败
     */
    @Override
    public boolean execute(MqMessage mqMessage) {
        try {
            // 处理课程发布逻辑
            log.info("开始处理课程发布任务，消息内容：{}", mqMessage);
            Long courseId = Long.parseLong(mqMessage.getBusinessKey1());

            // 生成课程静态化页面并上传至文件系统
            generateCourseHtml(mqMessage, courseId);

            // 添加es索引
            addESIndex(mqMessage,courseId);
            // 添加redis缓存

            // 查询当前MqMessage的状态
            return true;
        } catch (Exception e) {
            // 捕获所有可能的异常
            log.error("处理课程发布任务时发生异常, 消息内容: {}", mqMessage, e);
            // 返回 false，明确告知框架任务处理失败
            return false;
        }
    }

    //生成课程静态化页面并上传至文件系统
    public void generateCourseHtml(MqMessage mqMessage, long courseId) {
        // 任务幂等性
        int stageOne = mqMessageService.getStageOne(mqMessage.getId());
        if (stageOne > 0) {
            log.info("课程发布任务已完成第一阶段，课程ID：{}", courseId);
            return;
        }
        // 执行生成课程静态化页面的逻辑
        File file = coursePublishService.generateCourseHtml(courseId);
        if (file != null){
            coursePublishService.uploadCourseHtml(courseId,file);
        }
        // 标记任务第一阶段完成
        mqMessageService.completedStageOne(mqMessage.getId());
    }

    private void addESIndex(MqMessage mqMessage,Long courseId){
        // 任务幂等性
        int stageTwo = mqMessageService.getStageTwo(mqMessage.getId());
        if (stageTwo > 0) {
            log.info("课程发布任务已完成第二阶段，课程ID：{}", courseId);
            return;
        }

        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
        CourseIndex courseIndex = new CourseIndex();
        BeanUtils.copyProperties(coursePublish,courseIndex);
        Boolean isAdd = searchServiceClient.add(courseIndex);
        if (!isAdd){
            XueChengPlusException.cast("远程调用搜索服务添加索引失败");
        }

        mqMessageService.completedStageTwo(mqMessage.getId());
    }

    @XxlJob("CoursePublishJobHandler")
    public void coursePublishJobHandler() throws Exception {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        log.debug("shardIndex="+shardIndex+",shardTotal="+shardTotal);
        //参数:分片序号、分片总数、消息类型、一次最多取到的任务数量、一次任务调度执行的超时时间
        process(shardIndex,shardTotal,"course_publish",30,60);
    }

}