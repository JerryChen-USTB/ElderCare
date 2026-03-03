package edu.ustb.eldercarebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.ustb.eldercarebackend.entity.AppInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 应用信息数据访问层
 */
@Mapper
public interface AppInfoMapper extends BaseMapper<AppInfo> {
}