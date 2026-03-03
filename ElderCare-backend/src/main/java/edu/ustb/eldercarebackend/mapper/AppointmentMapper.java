package edu.ustb.eldercarebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.ustb.eldercarebackend.entity.Appointment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

/**
 * 预约信息数据访问层
 */
@Mapper
public interface AppointmentMapper extends BaseMapper<Appointment> {
    @Select("""
        SELECT a.* 
        FROM appointments a
        JOIN users u ON a.volunteer_id = u.id  -- 直接关联 users 表
        WHERE u.id = #{userId}                -- 通过 users.user_id 筛选
          AND DATE(a.start_time) = DATE(#{targetDate})
        ORDER BY a.start_time ASC
        """)
    List<Appointment> selectByUserIdAndDate(
            @Param("userId") Integer userId,
            @Param("targetDate") Date targetDate);


    /**
     * 查询所有未接单预约（状态为pending且未关联志愿者）
     */
    @Select("""
        SELECT id, elder_id, volunteer_id, appointment_type, 
               start_time, end_time, location,
               appointment_content, status, created_at, updated_at
        FROM appointments
        WHERE status = 'pending'
          AND volunteer_id IS NULL
        ORDER BY start_time ASC
        """)
    List<Appointment> selectPendingAppointments();


    @Update("""
        UPDATE appointments
        SET status = 'confirmed',
            volunteer_id = #{volunteerUserId},  -- 志愿者的 user_id（关联 users.id）
            updated_at = NOW()  -- 更新时间戳（数据库自动维护也可，这里显式更新更规范）
        WHERE id = #{appointmentId}
          AND status = 'pending'  -- 防重复接单：仅待接单状态可更新
          AND volunteer_id IS NULL  -- 防抢占：仅未分配志愿者的可更新
        """)
    int acceptAppointment(
            @Param("appointmentId") Integer appointmentId,  // 要接的预约ID
            @Param("volunteerUserId") Integer volunteerUserId);  //

    @Update("""
        UPDATE appointments
        SET volunteer_id = NULL,
            status = 'pending',
            updated_at = NOW()
        WHERE id = #{appointmentId}
          AND volunteer_id = #{userId}  -- 确保只能取消自己的预约
        """)
    int cancelAppointment(
            @Param("appointmentId") Integer appointmentId,
            @Param("userId") Integer userId);
}