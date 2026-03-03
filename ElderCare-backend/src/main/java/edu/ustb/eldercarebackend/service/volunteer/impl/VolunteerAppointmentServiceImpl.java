package edu.ustb.eldercarebackend.service.volunteer.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.ustb.eldercarebackend.entity.Appointment;
import edu.ustb.eldercarebackend.mapper.AppointmentMapper;
import edu.ustb.eldercarebackend.service.volunteer.VolunteerAppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class VolunteerAppointmentServiceImpl extends ServiceImpl<AppointmentMapper, Appointment>
        implements VolunteerAppointmentService {

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Override
    public List<Appointment> getByUserIdAndDate(Integer userId, Date targetDate) {
        return appointmentMapper.selectByUserIdAndDate(userId, targetDate);
    }

    @Override
    public boolean cancelAppointment(Integer appointmentId, Integer userId) {
        // 1. 查询预约记录（验证预约存在性）
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            throw new IllegalArgumentException("预约不存在");
        }

        // 2. 验证权限（确保是当前用户的预约）
        if (!userId.equals(appointment.getVolunteerId())) {
            throw new SecurityException("没有权限取消此预约");
        }

        // 3. 检查是否过期（使用开始时间判断）
        Date now = new Date();
        if (appointment.getStartTime() == null) {
            throw new IllegalArgumentException("预约开始时间不存在");
        }
        if (appointment.getStartTime().before(now)) {
            throw new IllegalArgumentException("不能取消已过期的预约");
        }

        // 4. 调用自定义的Mapper方法，强制更新volunteer_id为NULL
        int affectedRows = appointmentMapper.cancelAppointment(appointmentId, userId);

        return affectedRows > 0;
    }

    @Override
    public List<Appointment> getPendingAppointments() {
        return appointmentMapper.selectPendingAppointments();
    }


    // 事务注解：确保更新操作要么成功、要么回滚（防数据不一致）
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void acceptAppointment(Integer appointmentId, Integer volunteerUserId) {
        // 1. 参数校验（防非法请求）
        if (appointmentId == null || appointmentId <= 0) {
            throw new IllegalArgumentException("预约ID无效");
        }
        if (volunteerUserId == null || volunteerUserId <= 0) {
            throw new IllegalArgumentException("志愿者用户ID无效");
        }

        // 2. 调用Mapper执行更新，返回“受影响的行数”
        int affectedRows = appointmentMapper.acceptAppointment(appointmentId, volunteerUserId);

        // 3. 判断更新结果（受影响行数为0说明更新失败）
        if (affectedRows == 0) {
            // 可能原因：1. 预约ID不存在 2. 预约已被他人接单（status≠pending或volunteer_id≠null）3. 预约已取消
            throw new RuntimeException("接单失败：预约不存在或已被他人接单");
        }
    }

    /**
     * 按 userId 查询所有普通预约记录（无需新增 Mapper 方法）
     */
    @Override
    public List<Appointment> getByUserId(Integer userId) {
        // 使用 QueryWrapper 构建查询条件
        QueryWrapper<Appointment> queryWrapper = new QueryWrapper<>();
        // 条件：volunteer_id = userId（关联志愿者ID）
        queryWrapper.eq("volunteer_id", userId)
                // 按创建时间倒序（最新的记录在前）
                .orderByDesc("created_at");

        // 调用 MyBatis-Plus 自带的 selectList 方法执行查询
        return baseMapper.selectList(queryWrapper);
    }

}
