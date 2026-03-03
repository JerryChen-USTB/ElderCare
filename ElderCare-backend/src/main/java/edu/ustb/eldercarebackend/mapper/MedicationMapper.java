package edu.ustb.eldercarebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.ustb.eldercarebackend.entity.Medication;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用药管理数据访问层
 */
@Mapper
public interface MedicationMapper extends BaseMapper<Medication> {
}