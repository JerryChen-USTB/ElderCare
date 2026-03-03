package edu.ustb.eldercarebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.ustb.eldercarebackend.entity.ContactInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 联系信息数据访问层
 */
@Mapper
public interface ContactInfoMapper extends BaseMapper<ContactInfo> {
}