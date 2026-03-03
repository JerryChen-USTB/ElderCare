package edu.ustb.eldercarebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.ustb.eldercarebackend.entity.HealthInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 健康信息数据访问层
 */
@Mapper
public interface HealthInfoMapper extends BaseMapper<HealthInfo> {
}