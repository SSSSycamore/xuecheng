package com.xuecheng.content.service.impl;

import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.service.TeachplanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class TeachplanServiceImpl implements TeachplanService {
    private final TeachplanMapper teachplanMapper;

    @Override
    public List<TeachplanDto> selectTreeNodes(Long courseId) {
        return teachplanMapper.selectTreeNodes(courseId);
    }
}
