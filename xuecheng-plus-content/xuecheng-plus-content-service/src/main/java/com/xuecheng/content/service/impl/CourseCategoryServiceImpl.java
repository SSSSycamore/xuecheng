package com.xuecheng.content.service.impl;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.vo.CourseCategoryTreeVO;
import com.xuecheng.content.service.CourseCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CourseCategoryServiceImpl implements CourseCategoryService {
    private final CourseCategoryMapper courseCategoryMapper;

    @Override
    public List<CourseCategoryTreeVO> queryTreeNodes(String id) {
        // 数据库查询结果
        List<CourseCategoryTreeVO> courseCategoryTreeVOS = courseCategoryMapper.selectTreeNodes(id);
        // 将List集合转成Map，其中key为id，value为分类对象本身
        Map<String, CourseCategoryTreeVO> idMap = courseCategoryTreeVOS.stream().filter(item -> !item.getId().equals(id)).collect(Collectors.toMap(key -> key.getId(), value -> value, (key1, key2) -> key2));
        // 遍历courseCategoryTreeVOS，设置children属性
        List<CourseCategoryTreeVO> result = new ArrayList<>();
        courseCategoryTreeVOS.stream().filter(item -> !item.getId().equals(id)).forEach(item -> {
            if (item.getParentid().equals(id)) {
                result.add(item);
            }
            CourseCategoryTreeVO parentTreeVO = idMap.get(item.getParentid());
            if (parentTreeVO != null){
                if(parentTreeVO.getChildrenTreeNodes() == null){
                    parentTreeVO.setChildrenTreeNodes(new ArrayList<>());
                }
                parentTreeVO.getChildrenTreeNodes().add(item);
            }
        });
        return result;
    }
}
