package com.xuecheng.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.CourseIndex;
import com.xuecheng.content.feignclient.MediaServiceClient;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.mapper.CoursePublishPreMapper;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.*;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.content.service.CourseTeacherService;
import com.xuecheng.content.service.TeachplanService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/9/16 15:37
 */
@Service
@Slf4j
public class CoursePublishServiceImpl implements CoursePublishService {
    @Autowired
    CourseBaseMapper courseBaseMapper;

    @Autowired
    CourseBaseInfoService courseBaseInfoService;

    @Autowired
    TeachplanService teachplanService;

    @Autowired
    CourseMarketMapper courseMarketMapper;

    @Autowired
    CourseTeacherService courseTeacherService;

    @Autowired
    CoursePublishPreMapper coursePublishPreMapper;

    @Autowired
    CoursePublishMapper coursePublishMapper;

    @Autowired
    MqMessageService mqMessageService;

    @Autowired
    MediaServiceClient mediaServiceClient;

    @Autowired
    RestHighLevelClient client;

    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId) {

        //课程基本信息、营销信息
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);

        //课程计划信息
        List<TeachplanDto> teachplanTree = teachplanService.selectTreeNodes(courseId);

        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        coursePreviewDto.setCourseBase(courseBaseInfo);
        coursePreviewDto.setTeachplans(teachplanTree);
        return coursePreviewDto;
    }

    @Override
    @Transactional
    public void commitAudit(Long companyId, Long courseId) {
        // 校验工作
        // 校验课程是否存在
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (courseBase == null) {
            XueChengPlusException.cast("课程不存在");
        }
        // 校验课程图片是否上传
        if (courseBase.getPic() == null || courseBase.getPic().isEmpty()) {
            XueChengPlusException.cast("请先上传课程图片");
        }
        // 校验课程审核状态
        String auditStatus = courseBase.getAuditStatus();
        if ("202003".equals(auditStatus)) {
            XueChengPlusException.cast("课程已提交审核，请勿重复提交");
        }
        // 本机构只能提交本机构的课程
        if (!companyId.equals(courseBase.getCompanyId())) {
            XueChengPlusException.cast("只能提交本机构的课程");
        }
        CoursePublishPre coursePublishPre = new CoursePublishPre();
        //查询课程发布信息
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        //设置课程发布信息
        BeanUtils.copyProperties(courseBaseInfo, coursePublishPre);
        //获取课程营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        if (courseMarket == null) {
            XueChengPlusException.cast("请先添加课程营销信息");
        }
        //设置课程营销信息
        String marketJson = JSON.toJSONString(courseMarket);
        coursePublishPre.setMarket(marketJson);
        //查询课程计划信息
        List<TeachplanDto> teachplanTree = teachplanService.selectTreeNodes(courseId);
        if (teachplanTree == null || teachplanTree.isEmpty()) {
            XueChengPlusException.cast("请先添加课程计划信息");
        }
        //设置课程计划信息
        String teachPlanJson = JSON.toJSONString(teachplanTree);
        coursePublishPre.setTeachplan(teachPlanJson);
        //查询课程教师信息
        List<CourseTeacher> courseTeachers = courseTeacherService.queryTeacherList(courseId);
        if (courseTeachers == null || courseTeachers.isEmpty()) {
            XueChengPlusException.cast("请先添加课程教师信息");
        }
        //设置课程教师信息
        String teacherJson = JSON.toJSONString(courseTeachers);
        coursePublishPre.setTeachers(teacherJson);

        // 更新课程状态为已提交
        coursePublishPre.setStatus("202003"); // 设置为已提交审核状态
        coursePublishPre.setAuditDate(LocalDateTime.now());

        //查找是否已有预发布信息
        CoursePublishPre existingCoursePublishPre = coursePublishPreMapper.selectById(courseId);
        if (existingCoursePublishPre != null) {
            // 如果已存在预发布信息，则更新它
            coursePublishPreMapper.updateById(coursePublishPre);
        } else {
            // 如果不存在，则插入新的预发布信息
            coursePublishPreMapper.insert(coursePublishPre);
        }

        // 更新课程基本信息
        courseBase.setAuditStatus("202003"); // 设置为已提交审核状态
        courseBase.setChangeDate(LocalDateTime.now());
        courseBaseMapper.updateById(courseBase);
    }

    @Override
    @Transactional
    public void publish(Long companyId, Long courseId) {
        // 查询课程预发布信息
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPre == null) {
            XueChengPlusException.cast("课程预发布信息不存在，请先提交审核");
        }
        // 校验工作
        if (!"202004".equals(coursePublishPre.getStatus())) {
            XueChengPlusException.cast("课程未通过审核，不能发布");
        }
        if (!companyId.equals(coursePublishPre.getCompanyId())) {
            XueChengPlusException.cast("只能发布本机构的课程");
        }

        CoursePublish coursePublish = new CoursePublish();
        BeanUtils.copyProperties(coursePublishPre, coursePublish);
        // 设置课程发布状态
        coursePublish.setStatus("203002"); // 设置为已发布状态
        // 写入课程发布表
        CoursePublish existingCoursePublish =  coursePublishMapper.selectById(courseId);
        if (existingCoursePublish != null) {
            // 如果已存在发布信息，则更新它
            coursePublishMapper.updateById(coursePublish);
        } else {
            // 如果不存在，则插入新的发布信息
            coursePublishMapper.insert(coursePublish);
        }
        // 更新课程基本信息状态为已发布
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (courseBase == null) {
            XueChengPlusException.cast("课程不存在");
        }
        courseBase.setStatus("203002");
        courseBase.setChangeDate(LocalDateTime.now());
        courseBaseMapper.updateById(courseBase);

        //写入消息表
        saveCoursePublish(courseId);

        // 删除预发布信息
        coursePublishPreMapper.deleteById(courseId);
    }

    private void saveCoursePublish(Long courseId) {
        MqMessage mqMessage = mqMessageService.addMessage("course_publish", String.valueOf(courseId), null, null);
        if (mqMessage == null) {
            XueChengPlusException.cast("添加消息失败");
        }
    }

    @Override
    public File generateCourseHtml(Long courseId) {
        File htmlFile = null;
        try {
            Configuration configuration = new Configuration(Configuration.getVersion());
            String classpath = this.getClass().getResource("/").getPath();
            configuration.setDirectoryForTemplateLoading(new File(classpath+"/templates"));
            configuration.setDefaultEncoding("utf-8");
            Template template = configuration.getTemplate("course_template.ftl");
            CoursePreviewDto coursePreviewInfo = this.getCoursePreviewInfo(courseId);

            Map<String, Object> map = new HashMap<>();
            map.put("model",coursePreviewInfo);

            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template,map);
            log.info("html字符串生成，课程id:{}",courseId);

            InputStream inputStream = IOUtils.toInputStream(html, "utf-8");
            htmlFile = File.createTempFile("coursepublish",".html");
            FileOutputStream fileOutputStream = new FileOutputStream(htmlFile);
            IOUtils.copy(inputStream,fileOutputStream);
            log.info("课程html文件生成，课程id:{}",courseId);
        } catch (Exception e) {
            log.error("课程静态化异常:{}",e.toString());
            XueChengPlusException.cast("上传静态文件异常");
        }
        return htmlFile;
    }

    @Override
    public void uploadCourseHtml(Long courseId, File file) {
        try {
            MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
            String course = mediaServiceClient.upload(multipartFile,null, "course/"+courseId+".html");
            if(course==null){
                XueChengPlusException.cast("上传静态文件异常");
            }
        } catch (IOException e) {
            XueChengPlusException.cast("上传静态文件异常");
        }
    }

    @Override
    public void addEs() throws IOException {
        int pageNo = 1;
        int pageSize = 500;
        while(true){
            BulkRequest bulkRequest = new BulkRequest();
            Page<CoursePublish> coursePublishPage = coursePublishMapper.selectPage(new Page<>(pageNo, pageSize), null);
            List<CoursePublish> courseLists = coursePublishPage.getRecords();
            if (courseLists == null || courseLists.isEmpty()){
                return;
            }
            for(CoursePublish coursePublish : courseLists){
                CourseIndex courseIndex = new CourseIndex();
                BeanUtils.copyProperties(coursePublish,courseIndex);
                bulkRequest.add(new IndexRequest("course-publish").id(coursePublish.getId().toString())
                        .source(JSON.toJSONString(courseIndex), XContentType.JSON));
            }
            client.bulk(bulkRequest, RequestOptions.DEFAULT);
            pageNo++;
        }
    }


    /**
     * 根据课程 id查询课程发布信息
     * @param courseId
     * @return
     */
    public CoursePublish getCoursePublish(Long courseId){
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
        return coursePublish;
    }
}