package edu.ustb.eldercarebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.ustb.eldercarebackend.entity.Emergency;
import org.apache.ibatis.annotations.Mapper;

/**
 * 紧急情况数据访问层
 */
@Mapper
public interface EmergencyMapper extends BaseMapper<Emergency> {
}