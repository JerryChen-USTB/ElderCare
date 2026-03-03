package edu.ustb.eldercarebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.ustb.eldercarebackend.entity.Elder;
import edu.ustb.eldercarebackend.entity.Relation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 老人数据访问层
 */
@Mapper
public interface ElderMapper extends BaseMapper<Elder> {
    /**
     * 通过users.id查询老人信息
     */
    Elder selectByUserId(@Param("userId") Integer userId);
}