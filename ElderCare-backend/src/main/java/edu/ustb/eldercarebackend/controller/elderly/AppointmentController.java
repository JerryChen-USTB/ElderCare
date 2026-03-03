package edu.ustb.eldercarebackend.controller.elderly;

import edu.ustb.eldercarebackend.entity.Appointment;
import edu.ustb.eldercarebackend.service.elderly.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 预约服务控制器
 */
@RestController
@RequestMapping("/api/appointment")
@CrossOrigin(origins = "*")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    /**
     * 创建预约
     */
    @PostMapping("/create")
    public Map<String, Object> createAppointment(@RequestBody Appointment appointment) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 设置创建时间和默认状态
            appointment.setCreatedAt(new Date());
            appointment.setUpdatedAt(new Date());
            if (appointment.getStatus() == null) {
                appointment.setStatus("pending");
            }

            // 验证预约数据
            String validationResult = appointmentService.validateAppointment(appointment);
            if (validationResult != null) {
                result.put("success", false);
                result.put("message", validationResult);
                return result;
            }

            // 创建预约
            Appointment createdAppointment = appointmentService.createAppointment(appointment);
            if (createdAppointment != null) {
                // 预约创建成功后，同步到日程管理
                try {
                    boolean syncSuccess = appointmentService.syncAppointmentToSchedule(createdAppointment);
                    if (!syncSuccess) {
                        System.err.println("⚠️ 预约创建成功，但同步到日程失败，预约ID: " + createdAppointment.getId());
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ 预约同步到日程时发生异常: " + e.getMessage());
                    e.printStackTrace();
                    // 即使同步失败，预约已经创建成功，不影响返回结果
                }
                
                result.put("success", true);
                result.put("message", "预约创建成功");
                result.put("data", createdAppointment);
            } else {
                result.put("success", false);
                result.put("message", "预约创建失败");
            }

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "系统错误：" + e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取用户的预约列表
     */
    @GetMapping("/list/{elderId}")
    public Map<String, Object> getAppointmentsByElderId(@PathVariable Integer elderId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<Appointment> appointments = appointmentService.getAppointmentsByElderId(elderId);
            result.put("success", true);
            result.put("message", "获取预约列表成功");
            result.put("data", appointments);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取预约列表失败：" + e.getMessage());
        }
        
        return result;
    }

    /**
     * 根据ID获取预约详情
     */
    @GetMapping("/{appointmentId}")
    public Map<String, Object> getAppointmentById(@PathVariable Integer appointmentId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Appointment appointment = appointmentService.getAppointmentById(appointmentId);
            if (appointment != null) {
                result.put("success", true);
                result.put("message", "获取预约详情成功");
                result.put("data", appointment);
            } else {
                result.put("success", false);
                result.put("message", "预约不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取预约详情失败：" + e.getMessage());
        }
        
        return result;
    }

    /**
     * 取消预约
     */
    @PutMapping("/cancel/{appointmentId}")
    public Map<String, Object> cancelAppointment(@PathVariable Integer appointmentId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            boolean success = appointmentService.cancelAppointment(appointmentId);
            if (success) {
                result.put("success", true);
                result.put("message", "预约已取消");
            } else {
                result.put("success", false);
                result.put("message", "取消预约失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "取消预约失败：" + e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取待接单的预约列表（供志愿者查看）
     */
    @GetMapping("/pending")
    public Map<String, Object> getPendingAppointments() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<Appointment> appointments = appointmentService.getPendingAppointments();
            result.put("success", true);
            result.put("message", "获取待接单预约列表成功");
            result.put("data", appointments);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取待接单预约列表失败：" + e.getMessage());
        }
        
        return result;
    }
}
