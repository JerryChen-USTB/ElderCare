package edu.ustb.eldercarebackend.task;

import edu.ustb.eldercarebackend.entity.Appointment;
import edu.ustb.eldercarebackend.service.elderly.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 预约超时处理定时任务
 * 负责定期检查并更新超时的预约状态
 */
@Component
public class AppointmentTimeoutTask {

    @Autowired
    private AppointmentService appointmentService;

    /**
     * 定时检查并更新超时预约的状态
     * 每天晚上10:05执行一次（在10:00截止时间之后5分钟）
     * cron表达式：秒 分 时 日 月 周
     * 0 5 22 * * ? 表示：每天22:05:00执行
     */
    @Scheduled(cron = "0 5 22 * * ?")
    public void checkAndUpdateTimeoutAppointments() {
        try {
            System.out.println("🕰️ 开始执行预约超时检查任务...");
            
            // 查找所有需要标记为超时的预约
            List<Appointment> timeoutAppointments = appointmentService.findTimeoutAppointments();
            
            if (timeoutAppointments.isEmpty()) {
                System.out.println("✅ 没有发现需要标记为超时的预约");
                return;
            }
            
            System.out.println("🔍 发现 " + timeoutAppointments.size() + " 个需要标记为超时的预约");
            
            // 批量更新预约状态为超时
            int updatedCount = appointmentService.updateAppointmentsToTimeout(timeoutAppointments);
            
            System.out.println("🎯 预约超时检查任务完成，成功更新 " + updatedCount + " 条记录");
            
        } catch (Exception e) {
            System.err.println("❌ 预约超时检查任务执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 定时检查并更新超时预约的状态（测试用，每5分钟执行一次）
     * 可以用于测试和调试，生产环境建议禁用或删除
     */
    // @Scheduled(fixedRate = 5 * 60 * 1000) // 每5分钟执行一次（仅用于测试）
    public void checkAndUpdateTimeoutAppointmentsForTesting() {
        try {
            System.out.println("🧪 [测试] 开始执行预约超时检查任务...");
            
            // 查找所有需要标记为超时的预约
            List<Appointment> timeoutAppointments = appointmentService.findTimeoutAppointments();
            
            if (timeoutAppointments.isEmpty()) {
                System.out.println("✅ [测试] 没有发现需要标记为超时的预约");
                return;
            }
            
            System.out.println("🔍 [测试] 发现 " + timeoutAppointments.size() + " 个需要标记为超时的预约");
            
            // 批量更新预约状态为超时
            int updatedCount = appointmentService.updateAppointmentsToTimeout(timeoutAppointments);
            
            System.out.println("🎯 [测试] 预约超时检查任务完成，成功更新 " + updatedCount + " 条记录");
            
        } catch (Exception e) {
            System.err.println("❌ [测试] 预约超时检查任务执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
