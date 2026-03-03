package edu.ustb.eldercarebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.ustb.eldercarebackend.entity.Guardian;
import org.apache.ibatis.annotations.Mapper;

/**
 * 监护者数据访问层
 */
@Mapper
public interface GuardianMapper extends BaseMapper<Guardian> {
}