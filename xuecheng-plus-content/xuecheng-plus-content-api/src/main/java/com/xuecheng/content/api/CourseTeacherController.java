package com.xuecheng.content.api;

import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@Api(value = "课程教师接口", tags = "课程教师接口")
@RequestMapping("/courseTeacher")
@RequiredArgsConstructor
public class CourseTeacherController {
    private final CourseTeacherService courseTeacherService;

    @GetMapping("/list/{courseId}")
    @ApiOperation("查询课程教师列表")
    public List<CourseTeacher> queryTeacherList(@PathVariable Long courseId) {
        // 调用服务层方法查询课程教师列表
        List<CourseTeacher> courseTeachers = courseTeacherService.queryTeacherList(courseId);
        return courseTeachers;
    }

    @PostMapping
    @ApiOperation("添加或修改课程教师")
    public CourseTeacher saveTeacher(@RequestBody CourseTeacher courseTeacher) {
        // 根据id是否存在判断是添加还是修改
        if (courseTeacher.getId() == null) {
            // id为空，执行添加操作
            return courseTeacherService.createTeacher(courseTeacher);
        } else {
            // id不为空，执行修改操作
            return courseTeacherService.updateTeacher(courseTeacher);
        }
    }

    @DeleteMapping("/course/{courseId}/{teacherId}")
    @ApiOperation("删除课程教师")
    public void deleteTeacher(@PathVariable Long courseId, @PathVariable Long teacherId) {
        // 调用服务层方法删除课程教师
        courseTeacherService.deleteTeacher(courseId, teacherId);
    }
}
