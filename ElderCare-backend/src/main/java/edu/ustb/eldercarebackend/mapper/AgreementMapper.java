package edu.ustb.eldercarebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.ustb.eldercarebackend.entity.Agreement;
import org.apache.ibatis.annotations.Mapper;

/**
 * 协议管理数据访问层
 */
@Mapper
public interface AgreementMapper extends BaseMapper<Agreement> {
}