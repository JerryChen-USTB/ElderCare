package edu.ustb.eldercarebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.ustb.eldercarebackend.entity.Service;
import org.apache.ibatis.annotations.Mapper;

/**
 * 服务管理数据访问层
 */
@Mapper
public interface ServiceMapper extends BaseMapper<Service> {
}