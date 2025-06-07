package com.xuecheng.content.service;

import com.xuecheng.content.model.po.CourseTeacher;
import lombok.RequiredArgsConstructor;

import java.util.List;


public interface CourseTeacherService {
    /**
     * 查询课程教师列表
     * @param courseId 课程ID
     * @return 教师列表
     */
    List<CourseTeacher> queryTeacherList(Long courseId);

    /**
     * 添加课程教师
     * @param courseTeacher 教师信息
     * @return 添加后的教师信息
     */
    CourseTeacher createTeacher(CourseTeacher courseTeacher);

    /**
     * 修改课程教师
     * @param courseTeacher 教师信息
     * @return 修改后的教师信息
     */
    CourseTeacher updateTeacher(CourseTeacher courseTeacher);

    /**
     * 删除课程教师
     * @param courseId 课程ID
     * @param teacherId 教师ID
     */
    void deleteTeacher(Long courseId, Long teacherId);
}
