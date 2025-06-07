package com.xuecheng.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.dto.TeachplanDto;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 课程计划 Mapper 接口
 * </p>
 *
 * @author itcast
 */
public interface TeachplanMapper extends BaseMapper<Teachplan> {

    List<TeachplanDto> selectTreeNodes(Long courseId);

    int getMaxOrderBy(@Param("courseId") Long courseId,@Param("parentId") Long parentId);
}
