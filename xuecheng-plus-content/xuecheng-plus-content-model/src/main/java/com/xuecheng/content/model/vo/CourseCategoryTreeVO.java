package com.xuecheng.content.model.vo;

import com.xuecheng.content.model.po.CourseCategory;
import lombok.Data;

import java.util.List;

@Data
public class CourseCategoryTreeVO extends CourseCategory {
    List<CourseCategoryTreeVO> childrenTreeNodes;
}
