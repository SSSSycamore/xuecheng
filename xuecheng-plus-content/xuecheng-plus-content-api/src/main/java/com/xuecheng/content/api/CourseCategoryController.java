package com.xuecheng.content.api;

import com.xuecheng.content.model.vo.CourseCategoryTreeVO;
import com.xuecheng.content.service.CourseCategoryService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@Api(value = "课程分类接口", tags = "课程分类接口")
public class CourseCategoryController {
    private final CourseCategoryService courseCategoryService;

    @GetMapping("/course-category/tree-nodes")
    public List<CourseCategoryTreeVO> queryTreeNodes(){
        return courseCategoryService.queryTreeNodes("1");
    }
}
