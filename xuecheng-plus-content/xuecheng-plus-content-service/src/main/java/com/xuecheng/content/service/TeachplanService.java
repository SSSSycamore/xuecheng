package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;

import java.util.List;

public interface TeachplanService {
    List<TeachplanDto> selectTreeNodes(Long courseId);

    void saveTeachplan(SaveTeachplanDto teachplanDto);

    void deleteTeachplan(Long id);

    void moveTeachplan(String moveType, Long id);
}
