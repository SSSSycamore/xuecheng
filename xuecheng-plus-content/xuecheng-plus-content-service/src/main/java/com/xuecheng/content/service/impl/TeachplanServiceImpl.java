package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class TeachplanServiceImpl implements TeachplanService {
    private final TeachplanMapper teachplanMapper;
    private final TeachplanMediaMapper teachplanMediaMapper;

    @Override
    public List<TeachplanDto> selectTreeNodes(Long courseId) {
        return teachplanMapper.selectTreeNodes(courseId);
    }

    @Override
    public void saveTeachplan(SaveTeachplanDto teachplanDto) {
        //课程计划id
        Long id = teachplanDto.getId();
        //修改课程计划
        if(id!=null){
            Teachplan teachplan = teachplanMapper.selectById(id);
            BeanUtils.copyProperties(teachplanDto,teachplan);
            teachplanMapper.updateById(teachplan);
        }else{
            //取出同父同级别的课程计划数量
            int count = teachplanMapper.getMaxOrderBy(teachplanDto.getCourseId(), teachplanDto.getParentid());
            Teachplan teachplanNew = new Teachplan();
            //设置排序号
            teachplanNew.setOrderby(count+1);
            BeanUtils.copyProperties(teachplanDto,teachplanNew);

            teachplanMapper.insert(teachplanNew);
        }

    }

    @Transactional
    @Override
    public void deleteTeachplan(Long id) {
        Teachplan teachplan = teachplanMapper.selectById(id);
        if (teachplan.getGrade().equals(1)){
            LambdaQueryWrapper<Teachplan> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Teachplan::getParentid, id);
            if (teachplanMapper.selectCount(wrapper) > 0) {
                XueChengPlusException.cast("课程计划信息还有子级信息，无法操作");
            } else {
                teachplanMapper.deleteById(id);
            }
        }else{
            // 如果是二级课程计划，直接删除
            teachplanMapper.deleteById(id);
            LambdaQueryWrapper<TeachplanMedia> mediaWrapper = new LambdaQueryWrapper<>();
            mediaWrapper.eq(TeachplanMedia::getTeachplanId, id);
            teachplanMediaMapper.delete(mediaWrapper);
        }
    }

    @Transactional
    @Override
    public void moveTeachplan(String moveType, Long id) {
        Teachplan teachplan = teachplanMapper.selectById(id);
        if (teachplan == null) {
            XueChengPlusException.cast("课程计划不存在");
        }
        if (teachplan.getGrade() == 1) {
            XueChengPlusException.cast("一级课程计划不能进行排序");
        }
        LambdaQueryWrapper<Teachplan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Teachplan::getParentid, teachplan.getParentid())
                .eq(Teachplan::getGrade, teachplan.getGrade())
                .eq(Teachplan::getCourseId, teachplan.getCourseId());
        if ("moveup".equals(moveType)) {
            wrapper.lt(Teachplan::getOrderby, teachplan.getOrderby())
                    .orderByDesc(Teachplan::getOrderby);
        } else if ("movedown".equals(moveType)) {
            // 向下移动
            wrapper.gt(Teachplan::getOrderby, teachplan.getOrderby())
                    .orderByAsc(Teachplan::getOrderby);
        } else {
            XueChengPlusException.cast("排序类型错误");
        }
        List<Teachplan> teachplans = teachplanMapper.selectList(wrapper);
        if (teachplans.isEmpty()) {
            XueChengPlusException.cast("没有可以交换的课程计划");
        }
        Teachplan otherTeachplan = teachplans.get(0);
        // 交换两个课程计划的排序号
        int tempOrderby = teachplan.getOrderby();
        teachplan.setOrderby(otherTeachplan.getOrderby());
        otherTeachplan.setOrderby(tempOrderby);
        // 更新两个课程计划
        teachplanMapper.updateById(teachplan);
        teachplanMapper.updateById(otherTeachplan);
    }

    @Transactional
    @Override
    public TeachplanMedia associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto) {
        //教学计划id
        Long teachplanId = bindTeachplanMediaDto.getTeachplanId();
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        if(teachplan==null){
            XueChengPlusException.cast("教学计划不存在");
        }
        Integer grade = teachplan.getGrade();
        if(grade!=2){
            XueChengPlusException.cast("只允许第二级教学计划绑定媒资文件");
        }
        //课程id
        Long courseId = teachplan.getCourseId();

        //先删除原来该教学计划绑定的媒资
        teachplanMediaMapper.delete(new LambdaQueryWrapper<TeachplanMedia>().eq(TeachplanMedia::getTeachplanId,teachplanId));

        //再添加教学计划与媒资的绑定关系
        TeachplanMedia teachplanMedia = new TeachplanMedia();
        teachplanMedia.setCourseId(courseId);
        teachplanMedia.setTeachplanId(teachplanId);
        teachplanMedia.setMediaFilename(bindTeachplanMediaDto.getFileName());
        teachplanMedia.setMediaId(bindTeachplanMediaDto.getMediaId());
        teachplanMedia.setCreateDate(LocalDateTime.now());
        teachplanMediaMapper.insert(teachplanMedia);
        return teachplanMedia;
    }


}
