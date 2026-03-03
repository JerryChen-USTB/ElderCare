package edu.ustb.eldercarebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.ustb.eldercarebackend.entity.Assistance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 远程协助数据访问层
 */
@Mapper
public interface AssistanceMapper extends BaseMapper<Assistance> {
    
    /**
     * 根据老人ID查找活跃的协助项目（状态不为completed或cancelled）
     * @param elderId 老人ID
     * @return 活跃的协助项目
     */
    @Select("SELECT * FROM assistance WHERE elder_id = #{elderId} AND status NOT IN ('completed', 'cancelled') ORDER BY created_at DESC LIMIT 1")
    Assistance findActiveAssistanceByElderId(Integer elderId);
    
    /**
     * 根据老人ID查找最新的协助记录（不论状态）
     * @param elderId 老人ID
     * @return 最新的协助记录
     */
    @Select("SELECT * FROM assistance WHERE elder_id = #{elderId} ORDER BY created_at DESC LIMIT 1")
    Assistance findLatestAssistanceByElderId(Integer elderId);
    
    /**
     * 根据志愿者ID获取志愿者姓名
     * @param volunteerId 志愿者ID
     * @return 志愿者姓名
     */
    @Select("SELECT name FROM volunteers WHERE id = #{volunteerId}")
    String getVolunteerNameById(Integer volunteerId);
    
    /**
     * 获取所有等待志愿者接单的协助项目
     * @return 等待接单的协助项目列表
     */
    @Select("SELECT * FROM assistance WHERE status = 'waiting_response' ORDER BY apply_time ASC")
    List<Assistance> findWaitingAssistances();
    
    /**
     * 根据老人ID获取老人姓名
     * @param elderId 老人ID
     * @return 老人姓名
     */
    @Select("SELECT name FROM elders WHERE user_id = #{elderId}")
    String getElderNameById(Integer elderId);
    
    /**
     * 志愿者接单 - 更新协助项目的志愿者ID和状态
     * @param assistanceId 协助项目ID
     * @param volunteerId 志愿者ID
     * @return 更新行数
     */
    @Update("UPDATE assistance SET volunteer_id = #{volunteerId}, status = 'waiting_call' WHERE id = #{assistanceId} AND status = 'waiting_response'")
    int acceptAssistance(Integer assistanceId, Integer volunteerId);
    
    /**
     * 查询志愿者当前等待中的协助项目
     * @param volunteerId 志愿者ID
     * @return 等待中的协助项目
     */
    @Select("SELECT * FROM assistance WHERE volunteer_id = #{volunteerId} AND status = 'waiting_call' ORDER BY apply_time DESC LIMIT 1")
    Assistance findWaitingCallByVolunteerId(Integer volunteerId);
}
