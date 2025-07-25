package com.xuecheng.media.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuecheng.media.model.po.MediaProcess;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author itcast
 */
public interface MediaProcessMapper extends BaseMapper<MediaProcess> {
    @Select("select * from media_process as m where m.id % #{shardTotal} = #{shardIndex} " +
            "and (m.status = '1' or m.status = '3') and m.fail_count < 3 limit #{count}")
    public List<MediaProcess> selectListByShardIndex(@Param("shardIndex") int shardIndex,
                                                     @Param("shardTotal") int shardTotal,
                                                     @Param("count") int count);

    @Update("update media_process m set m.status='4' where (m.status='1' or m.status='3') and m.fail_count<3 and m.id=#{id}")
    int startTask(@Param("id") long id);}
