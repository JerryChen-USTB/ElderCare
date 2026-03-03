package edu.ustb.eldercarebackend.controller.volunteer;

import edu.ustb.eldercarebackend.entity.Appointment;
import edu.ustb.eldercarebackend.service.volunteer.VolunteerAppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;


@RestController
@RequestMapping("/api/volunteer/appointment")


public class VolunteerAppointmentController {

    @Autowired
    private VolunteerAppointmentService appointmentService;

    // 日期格式化（复用）
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");

    @GetMapping("/list")
    public Map<String, Object> getAppointments(
            @RequestParam("userId") Integer userId,
            @RequestParam("targetDate") @DateTimeFormat(pattern = "yyyy-MM-dd") Date targetDate) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Appointment> appointments = appointmentService.getByUserIdAndDate(userId, targetDate);
            List<Map<String, Object>> events = new ArrayList<>();

            for (Appointment appt : appointments) {
                Map<String, Object> event = new HashMap<>();
                event.put("id", appt.getId());
                event.put("title", appt.getAppointmentContent()); // 标题用预约内容
                event.put("date", DATE_FORMAT.format(appt.getStartTime())); // 日期用开始时间
                // 1. 替换：用数据库的start_time/end_time，不再计算
                event.put("startTime", TIME_FORMAT.format(appt.getStartTime()));
                event.put("endTime", TIME_FORMAT.format(appt.getEndTime()));
                // 2. 替换：用数据库的location，不再固定值
                event.put("location", appt.getLocation() != null ? appt.getLocation() : "未填写地点");
                event.put("description", appt.getAppointmentContent()); // 描述用预约内容
                event.put("type", convertType(appt.getAppointmentType())); // 类型转换不变
                events.add(event);
            }

            result.put("success", true);
            result.put("events", events);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "查询失败：" + e.getMessage());
        }

        return result;
    }
    /**
     * 获取所有未接单预约（供首页推荐）
     */
    @GetMapping("/pending-list")
    public Map<String, Object> getPendingAppointments() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Appointment> pendingApps = appointmentService.getPendingAppointments();
            List<Map<String, Object>> recommendedTasks = new ArrayList<>();

            for (Appointment appt : pendingApps) {
                Map<String, Object> task = new HashMap<>();
                task.put("id", appt.getId());
                task.put("title", appt.getAppointmentContent());
                task.put("status", "待分配");
                task.put("date", DATE_FORMAT.format(appt.getStartTime()));
                task.put("location", appt.getLocation() != null ? appt.getLocation() : "未填写地点");
                task.put("reward", getServiceDuration(appt.getStartTime(), appt.getEndTime()));
                recommendedTasks.add(task);
            }

            result.put("success", true);
            result.put("recommendedTasks", recommendedTasks);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取推荐预约失败：" + e.getMessage());
        }
        return result;
    }

    @PostMapping("/cancel/{appointmentId}")
    public Map<String, Object> cancelAppointment(
            @PathVariable("appointmentId") Integer appointmentId,
            @RequestParam("userId") Integer userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            boolean success = appointmentService.cancelAppointment(appointmentId, userId);
            if (success) {
                result.put("success", true);
                result.put("message", "预约已成功取消");
            } else {
                result.put("success", false);
                result.put("message", "取消预约失败");
            }
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        } catch (SecurityException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            result.put("code", 403); // 权限错误码
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "服务器错误：" + e.getMessage());
        }

        return result;
    }

    /**
     * 志愿者接单接口
     * @param appointmentId 要接的预约ID（从URL路径获取）
     * @return 统一响应格式（成功/失败信息）
     */
    @PostMapping("/accept/{appointmentId}")
    public Map<String, Object> acceptAppointment(
            @PathVariable Integer appointmentId,
            @RequestParam("userId") Integer userId) {

        // 响应结果命名与其他接口一致（用result而非response）
        Map<String, Object> result = new HashMap<>();
        try {
            // 1. 先校验userId有效性（参考cancel接口的参数校验逻辑）
            if (userId == null || userId <= 0) {
                throw new IllegalArgumentException("用户ID无效");
            }

            // 2. 调用Service执行接单逻辑（传入动态userId，删除静态值15）
            appointmentService.acceptAppointment(appointmentId, userId);

            // 3. 成功响应（格式与其他接口完全一致）
            result.put("success", true);
            result.put("message", "接单成功");
        } catch (IllegalArgumentException e) {
            // 参数错误处理（与cancel接口异常类型对齐）
            result.put("success", false);
            result.put("message", e.getMessage());
        } catch (RuntimeException e) {
            // 业务错误处理（如预约已被接单，保持原有逻辑）
            result.put("success", false);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            // 系统异常处理（与其他接口的错误提示文案对齐）
            result.put("success", false);
            result.put("message", "服务器错误：" + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 计算服务时长
     */
    private String getServiceDuration(Date startTime, Date endTime) {
        long durationMs = endTime.getTime() - startTime.getTime();
        double durationHours = durationMs / (1000.0 * 60 * 60);
        if (durationHours == Math.floor(durationHours)) {
            return (int) durationHours + "小时服务时长";
        } else {
            return String.format("%.1f小时服务时长", durationHours);
        }
    }

    @GetMapping("/records")
    public Map<String, Object> getAppointmentRecords(@RequestParam("userId") Integer userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Appointment> records = appointmentService.getByUserId(userId);
            result.put("success", true);
            result.put("data", records); // 仅返回实体字段，无额外名称
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "查询普通预约记录失败：" + e.getMessage());
        }
        return result;
    }


    // 类型转换方法不变
    private Integer convertType(String type) {
        return switch (type) {
            case "doctor" -> 1;
            case "nurse" -> 2;
            case "rehab" -> 3;
            case "therapy" -> 4;
            default -> 5;
        };
    }
}