package edu.ustb.eldercarebackend.service.elderly.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import edu.ustb.eldercarebackend.entity.Appointment;
import edu.ustb.eldercarebackend.entity.Schedule;
import edu.ustb.eldercarebackend.mapper.AppointmentMapper;
import edu.ustb.eldercarebackend.service.elderly.AppointmentService;
import edu.ustb.eldercarebackend.service.elderly.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 预约服务实现类
 */
@Service
public class AppointmentServiceImpl implements AppointmentService {

    @Autowired
    private AppointmentMapper appointmentMapper;
    
    @Autowired
    private ScheduleService scheduleService;

    @Override
    public Appointment createAppointment(Appointment appointment) {
        try {
            int result = appointmentMapper.insert(appointment);
            if (result > 0) {
                return appointment;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<Appointment> getAppointmentsByElderId(Integer elderId) {
        QueryWrapper<Appointment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("elder_id", elderId);
        queryWrapper.orderByDesc("created_at");
        return appointmentMapper.selectList(queryWrapper);
    }

    @Override
    public Appointment getAppointmentById(Integer appointmentId) {
        return appointmentMapper.selectById(appointmentId);
    }

    @Override
    public boolean cancelAppointment(Integer appointmentId) {
        try {
            UpdateWrapper<Appointment> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", appointmentId);
            updateWrapper.set("status", "canceled");
            updateWrapper.set("updated_at", new Date());
            
            int result = appointmentMapper.update(null, updateWrapper);
            return result > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<Appointment> getPendingAppointments() {
        QueryWrapper<Appointment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", "pending");
        queryWrapper.orderByAsc("start_time");
        return appointmentMapper.selectList(queryWrapper);
    }

    @Override
    public String validateAppointment(Appointment appointment) {
        // 验证必填字段
        if (appointment.getElderId() == null) {
            return "用户ID不能为空";
        }
        
        if (appointment.getAppointmentType() == null || appointment.getAppointmentType().trim().isEmpty()) {
            return "预约类型不能为空";
        }
        
        if (appointment.getAppointmentContent() == null || appointment.getAppointmentContent().trim().isEmpty()) {
            return "服务内容不能为空";
        }
        
        if (appointment.getStartTime() == null) {
            return "开始时间不能为空";
        }
        
        // 验证预约类型是否合法
        String[] validTypes = {"doctor", "nurse", "rehab", "therapy", "other"};
        boolean isValidType = false;
        for (String type : validTypes) {
            if (type.equals(appointment.getAppointmentType())) {
                isValidType = true;
                break;
            }
        }
        if (!isValidType) {
            return "无效的预约类型";
        }
        
        // 验证预约时间不能是过去时间
        if (appointment.getStartTime().before(new Date())) {
            return "预约时间不能早于当前时间";
        }
        
        // 如果填写了结束时间，需要验证时间逻辑
        if (appointment.getEndTime() != null) {
            if (appointment.getEndTime().before(appointment.getStartTime()) || 
                appointment.getEndTime().equals(appointment.getStartTime())) {
                return "结束时间必须晚于开始时间";
            }
            
            // 验证服务时长不超过8小时
            long timeDiff = appointment.getEndTime().getTime() - appointment.getStartTime().getTime();
            long hoursDiff = timeDiff / (1000 * 60 * 60); // 转换为小时
            if (hoursDiff > 8) {
                return "单次服务时长不能超过8小时";
            }
        }
        
        // 验证服务内容长度
        if (appointment.getAppointmentContent().length() > 500) {
            return "服务内容不能超过500个字符";
        }
        
        // 验证地点信息长度（可选字段）
        if (appointment.getLocation() != null && appointment.getLocation().length() > 255) {
            return "地点描述不能超过255个字符";
        }
        
        // 备注信息已经合并到appointmentContent中，不需要单独验证
        
        return null; // 验证通过
    }

    @Override
    public boolean acceptAppointment(Integer appointmentId, Integer volunteerId) {
        try {
            UpdateWrapper<Appointment> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", appointmentId);
            updateWrapper.eq("status", "pending"); // 只有待接单状态才能被接单
            updateWrapper.set("volunteer_id", volunteerId);
            updateWrapper.set("status", "confirmed");
            updateWrapper.set("updated_at", new Date());
            
            int result = appointmentMapper.update(null, updateWrapper);
            return result > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean updateAppointmentStatus(Integer appointmentId, String status) {
        try {
            // 验证状态是否合法
            String[] validStatuses = {"pending", "confirmed", "completed", "canceled", "time_out", "no_show"};
            boolean isValidStatus = false;
            for (String validStatus : validStatuses) {
                if (validStatus.equals(status)) {
                    isValidStatus = true;
                    break;
                }
            }
            if (!isValidStatus) {
                return false;
            }
            
            UpdateWrapper<Appointment> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", appointmentId);
            updateWrapper.set("status", status);
            updateWrapper.set("updated_at", new Date());
            
            int result = appointmentMapper.update(null, updateWrapper);
            return result > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean syncAppointmentToSchedule(Appointment appointment) {
        try {
            if (appointment == null || appointment.getId() == null) {
                System.err.println("❌ 同步失败：预约信息为空或缺少ID");
                return false;
            }

            System.out.println("🔗 开始同步预约到日程管理，预约ID: " + appointment.getId());
            
            // 创建对应的日程记录
            Schedule schedule = new Schedule();
            
            // 基本信息
            schedule.setUserId(appointment.getElderId()); // 老人用户ID
            schedule.setAppointmentId(appointment.getId()); // 关联预约ID
            
            // 日程类型和内容
            schedule.setType("appointment"); // 标记为预约类型的日程
            schedule.setContent(appointment.getAppointmentContent()); // 构建日程内容
            
            // 时间设置
            schedule.setTime(appointment.getStartTime()); // 日程时间使用预约开始时间
            schedule.setReminderTime(calculateReminderTime(appointment.getStartTime())); // 提前30分钟提醒
            
            // 地点信息（如果用户提供了地点，使用用户的地点，否则使用默认值）
            String scheduleLocation = (appointment.getLocation() != null && !appointment.getLocation().trim().isEmpty()) 
                                     ? appointment.getLocation() 
                                     : "待志愿者确认";
            schedule.setLocation(scheduleLocation);
            
            // 状态设置 - 志愿者服务预约的状态始终为null，需要时从appointments表中查询
            schedule.setStatus(null);
            
            // 重复类型（预约通常不重复）
            schedule.setRepeatType("none");
            schedule.setParentScheduleId(null); // 一次性日程
            
            // 创建和更新时间
            schedule.setCreatedAt(new Date());
            schedule.setUpdatedAt(new Date());
            
            // 保存到数据库
            boolean success = scheduleService.createSchedule(schedule);
            
            if (success) {
                System.out.println("✅ 预约同步到日程成功，预约ID: " + appointment.getId() + "，日程ID: " + schedule.getId());
                return true;
            } else {
                System.err.println("❌ 预约同步到日程失败，预约ID: " + appointment.getId());
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ 同步预约到日程时发生异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 计算提醒时间（提前30分钟）
     */
    private Date calculateReminderTime(Date appointmentTime) {
        if (appointmentTime == null) return null;
        
        long reminderMillis = appointmentTime.getTime() - (30 * 60 * 1000); // 提前30分钟
        return new Date(reminderMillis);
    }

    @Override
    public boolean updateScheduleByAppointmentId(Integer appointmentId, String newStatus) {
        try {
            System.out.println("🔄 预约状态更新通知，预约ID: " + appointmentId + ", 新状态: " + newStatus);
            
            // 志愿者服务预约在日程管理中状态始终为null，不需要同步更新日程状态
            // 需要获取状态时应该直接从appointments表中查询
            // 这里只是记录日志，不实际更新日程状态
            
            System.out.println("📝 注意：志愿者服务预约的日程状态始终为null，实际状态请从appointments表查询");
            System.out.println("✅ 预约状态变更已记录");
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ 处理预约状态变更时发生异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<Appointment> findTimeoutAppointments() {
        try {
            // 计算当前时间，用于确定哪些预约应该被标记为超时
            Date currentTime = new Date();
            
            // 创建查询条件
            QueryWrapper<Appointment> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status", "pending"); // 只查找状态为pending的预约
            
            // 获取所有pending状态的预约
            List<Appointment> pendingAppointments = appointmentMapper.selectList(queryWrapper);
            
            // 过滤出需要标记为超时的预约
            return pendingAppointments.stream()
                    .filter(appointment -> isAppointmentTimeout(appointment, currentTime))
                    .collect(java.util.stream.Collectors.toList());
            
        } catch (Exception e) {
            System.err.println("❌ 查找超时预约时发生异常: " + e.getMessage());
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    @Override
    public int updateAppointmentsToTimeout(List<Appointment> appointments) {
        if (appointments == null || appointments.isEmpty()) {
            return 0;
        }
        
        int updateCount = 0;
        Date updateTime = new Date();
        
        try {
            for (Appointment appointment : appointments) {
                UpdateWrapper<Appointment> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("id", appointment.getId());
                updateWrapper.eq("status", "pending"); // 只更新状态仍为pending的记录
                updateWrapper.set("status", "time_out");
                updateWrapper.set("updated_at", updateTime);
                
                int result = appointmentMapper.update(null, updateWrapper);
                if (result > 0) {
                    updateCount++;
                    System.out.println("⏰ 预约ID " + appointment.getId() + " 已标记为超时");
                    
                    // 同步更新相关的日程状态
                    try {
                        updateScheduleByAppointmentId(appointment.getId(), "time_out");
                    } catch (Exception e) {
                        System.err.println("⚠️ 同步更新日程状态失败，预约ID: " + appointment.getId());
                    }
                }
            }
            
            if (updateCount > 0) {
                System.out.println("✅ 已将 " + updateCount + " 个预约标记为超时");
            }
            
        } catch (Exception e) {
            System.err.println("❌ 批量更新预约状态为超时时发生异常: " + e.getMessage());
            e.printStackTrace();
        }
        
        return updateCount;
    }

    /**
     * 判断预约是否应该被标记为超时
     * 规则：如果当前时间已经超过预约开始时间前一天的晚上10点，且状态仍为pending，则应该被标记为超时
     * 
     * @param appointment 预约对象
     * @param currentTime 当前时间
     * @return 是否应该标记为超时
     */
    private boolean isAppointmentTimeout(Appointment appointment, Date currentTime) {
        if (appointment.getStartTime() == null) {
            return false;
        }
        
        try {
            // 计算预约开始时间前一天晚上10点
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(appointment.getStartTime());
            
            // 减去一天
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            
            // 设置为晚上10点
            calendar.set(Calendar.HOUR_OF_DAY, 22);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            
            Date timeoutDeadline = calendar.getTime();
            
            // 如果当前时间已经超过截止时间，则应该标记为超时
            boolean shouldTimeout = currentTime.after(timeoutDeadline);
            
            if (shouldTimeout) {
                System.out.println("🔍 发现超时预约：ID=" + appointment.getId() + 
                                  ", 开始时间=" + appointment.getStartTime() + 
                                  ", 截止时间=" + timeoutDeadline + 
                                  ", 当前时间=" + currentTime);
            }
            
            return shouldTimeout;
            
        } catch (Exception e) {
            System.err.println("❌ 判断预约是否超时时发生异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
