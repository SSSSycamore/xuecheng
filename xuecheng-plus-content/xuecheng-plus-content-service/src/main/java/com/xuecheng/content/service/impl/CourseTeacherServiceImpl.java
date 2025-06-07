package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseTeacherMapper;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseTeacherServiceImpl implements CourseTeacherService {
    private final CourseTeacherMapper courseTeacherMapper;
    private final CourseBaseMapper courseBaseMapper;

    @Override
    public List<CourseTeacher> queryTeacherList(Long courseId) {
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId, courseId);
        List<CourseTeacher> courseTeachers = courseTeacherMapper.selectList(queryWrapper);
        return courseTeachers != null ? courseTeachers : Collections.emptyList();
    }

    @Override
    public CourseTeacher createTeacher(CourseTeacher courseTeacher) {
        CourseBase courseBase = courseBaseMapper.selectById(courseTeacher.getCourseId());
        if (courseBase.getCompanyId()!=1232141425L){
            XueChengPlusException.cast("只能为本机构的课程添加教师");
        }
        // 插入数据库，createDate字段由MyBatis-Plus自动填充
        courseTeacherMapper.insert(courseTeacher);
        // 返回新增后的教师信息
        return courseTeacher;
    }

    @Override
    public CourseTeacher updateTeacher(CourseTeacher courseTeacher) {
        CourseBase courseBase = courseBaseMapper.selectById(courseTeacher.getCourseId());
        if (courseBase.getCompanyId()!=1232141425L){
            XueChengPlusException.cast("只能修改本机构的课程教师信息");
        }
        // 更新教师信息
        courseTeacherMapper.updateById(courseTeacher);
        // 返回更新后的教师信息
        return courseTeacher;
    }

    @Override
    public void deleteTeacher(Long courseId, Long teacherId) {
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId, courseId)
                .eq(CourseTeacher::getId, teacherId);
        // 删除教师记录
        courseTeacherMapper.delete(queryWrapper);
    }
}
