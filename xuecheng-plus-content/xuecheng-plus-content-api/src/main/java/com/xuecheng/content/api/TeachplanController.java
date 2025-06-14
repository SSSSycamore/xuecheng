package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.service.TeachplanService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @description 课程计划编辑接口
 * @author Mr.M
 * @date 2022/9/6 11:29
 * @version 1.0
 */
 @Api(value = "课程计划编辑接口",tags = "课程计划编辑接口")
 @RestController
 @RequiredArgsConstructor
public class TeachplanController {
     private final TeachplanService teachplanService;

    @ApiOperation("查询课程计划树形结构")
    @ApiImplicitParam(value = "courseId",name = "课程Id",required = true,dataType = "Long",paramType = "path")
    @GetMapping("/teachplan/{courseId}/tree-nodes")
    public List<TeachplanDto> getTreeNodes(@PathVariable Long courseId){
        List<TeachplanDto> teachplanDtos = teachplanService.selectTreeNodes(courseId);
        return teachplanDtos;
    }

    @ApiOperation("课程计划创建或修改")
    @PostMapping("/teachplan")
    public void saveTeachplan(@RequestBody SaveTeachplanDto teachplanDto){
        // 调用服务层方法保存课程计划
        teachplanService.saveTeachplan(teachplanDto);
    }

    @ApiOperation("课程计划删除")
    @DeleteMapping("/teachplan/{id}")
    public void deleteTeachplan(@PathVariable Long id) {
        // 调用服务层方法删除课程计划
        teachplanService.deleteTeachplan(id);
    }

    @ApiOperation("课程计划排序")
    @PostMapping("/teachplan/{moveType}/{id}")
    public void moveTeachplan(@PathVariable String moveType, @PathVariable Long id) {
        // 调用服务层方法进行课程计划排序
        teachplanService.moveTeachplan(moveType, id);
    }

    @ApiOperation(value = "课程计划和媒资信息绑定")
    @PostMapping("/teachplan/association/media")
    public void associationMedia(@RequestBody BindTeachplanMediaDto bindTeachplanMediaDto){
        teachplanService.associationMedia(bindTeachplanMediaDto);
    }

}