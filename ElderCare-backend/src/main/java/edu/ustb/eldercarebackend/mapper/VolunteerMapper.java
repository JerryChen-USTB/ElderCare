package edu.ustb.eldercarebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.ustb.eldercarebackend.entity.Volunteer;
import org.apache.ibatis.annotations.Mapper;

/**
 * 志愿者数据访问层
 */
@Mapper
public interface VolunteerMapper extends BaseMapper<Volunteer> {
}