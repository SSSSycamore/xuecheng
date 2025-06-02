package com.xuecheng.content.service;

import com.xuecheng.content.model.vo.CourseCategoryTreeVO;

import java.util.List;

public interface CourseCategoryService {
    List<CourseCategoryTreeVO> queryTreeNodes(String id);
}
