package edu.ustb.eldercarebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.ustb.eldercarebackend.entity.Relation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 关系管理数据访问层
 */
@Mapper
public interface RelationMapper extends BaseMapper<Relation> {
    /**
     * 根据监护人ID查询所有关联的老人关系记录
     * @param guardianUserId 监护人用户ID（对应relations表的guardian_id字段）
     * @return 该监护人关联的所有关系记录列表
     */
    List<Relation> selectByGuardianUserId(@Param("guardianUserId") Integer guardianUserId);
    
    /**
     * 根据老年人ID查询所有关联的监护人关系记录
     * @param elderlyId 老年人用户ID（对应relations表的elderly_id字段）
     * @return 该老年人关联的所有监护人关系记录列表
     */
    List<Relation> selectByElderlyId(@Param("elderlyId") Integer elderlyId);
    
    /**
     * 检查监护关系是否存在
     * @param guardianId 监护人ID
     * @param elderlyId 老年人ID
     * @return 关系记录，不存在则返回null
     */
    Relation selectByGuardianAndElderly(@Param("guardianId") Integer guardianId, 
                                        @Param("elderlyId") Integer elderlyId);
}