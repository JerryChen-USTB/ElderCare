package edu.ustb.eldercarebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.ustb.eldercarebackend.entity.Schedule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 日程管理数据访问层
 */
@Mapper
public interface ScheduleMapper extends BaseMapper<Schedule> {
}