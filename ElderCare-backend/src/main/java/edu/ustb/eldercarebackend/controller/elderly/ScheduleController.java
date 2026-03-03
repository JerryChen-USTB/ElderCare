package edu.ustb.eldercarebackend.controller.elderly;

import edu.ustb.eldercarebackend.entity.Schedule;
import edu.ustb.eldercarebackend.service.elderly.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 日程管理控制器
 */
@RestController
@RequestMapping("/api/schedule")
@CrossOrigin(origins = "*")
public class ScheduleController {
    
    // 添加构造函数，确认Controller被正确加载

    
    @Autowired
    private ScheduleService scheduleService;
    

    
    /**
     * 获取用户日程列表
     * @param userId 用户ID
     * @param type 日程类型（可选）
     * @return 日程列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getScheduleList(
            @RequestParam Integer userId,
            @RequestParam(required = false) String type) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Schedule> schedules;
            if (type != null && !type.isEmpty() && !"all".equals(type)) {
                schedules = scheduleService.getSchedulesByUserIdAndType(userId, type);
            } else {
                schedules = scheduleService.getSchedulesByUserId(userId);
            }

            
            response.put("success", true);
            response.put("data", schedules);
            response.put("message", "获取日程列表成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取日程列表失败：" + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 创建新日程
     * @param scheduleRequest 日程信息
     * @return 创建结果
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createSchedule(@RequestBody Map<String, Object> scheduleRequest) {

        
        Map<String, Object> response = new HashMap<>();
        
        try {

            
            Schedule schedule = new Schedule();
            schedule.setUserId((Integer) scheduleRequest.get("userId"));
            schedule.setType((String) scheduleRequest.get("type"));
            schedule.setContent((String) scheduleRequest.get("content"));
            schedule.setLocation((String) scheduleRequest.get("location"));
            schedule.setStatus((String) scheduleRequest.get("status"));
            schedule.setRepeatType((String) scheduleRequest.get("repeatType"));
            

            
            // 处理时间字段
            String dateStr = (String) scheduleRequest.get("date");
            String timeStr = (String) scheduleRequest.get("time");

            
            if (dateStr != null && timeStr != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                Date scheduleTime = sdf.parse(dateStr + " " + timeStr);
                schedule.setTime(scheduleTime);

            }
            

            
            boolean success = scheduleService.createSchedule(schedule);

            
            if (success) {
                response.put("success", true);
                response.put("message", "日程创建成功");
                response.put("data", schedule);

            } else {
                response.put("success", false);
                response.put("message", "日程创建失败");

            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {

            e.printStackTrace();
            response.put("success", false);
            response.put("message", "日程创建失败：" + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 更新日程状态
     * @param id 日程ID
     * @param statusRequest 状态信息
     * @return 更新结果
     */
    @PutMapping("/status/{id}")
    public ResponseEntity<Map<String, Object>> updateScheduleStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> statusRequest) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String status = statusRequest.get("status");
            boolean success = scheduleService.updateScheduleStatus(id, status);
            
            if (success) {
                response.put("success", true);
                response.put("message", "状态更新成功");
            } else {
                response.put("success", false);
                response.put("message", "状态更新失败");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "状态更新失败：" + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 更新日程信息
     * @param id 日程ID
     * @param scheduleRequest 日程信息
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateSchedule(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> scheduleRequest) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 首先检查日程是否存在
            Schedule existingSchedule = scheduleService.getScheduleById(id);
            if (existingSchedule == null) {
                response.put("success", false);
                response.put("message", "日程不存在");
                return ResponseEntity.status(404).body(response);
            }
            
            // 创建更新对象，只更新指定字段
            Schedule schedule = new Schedule();
            schedule.setId(id);
            
            // 更新日程类型
            if (scheduleRequest.containsKey("type")) {
                schedule.setType((String) scheduleRequest.get("type"));
            }
            
            // 更新日程内容
            if (scheduleRequest.containsKey("content")) {
                schedule.setContent((String) scheduleRequest.get("content"));
            }
            
            // 更新日程地点
            if (scheduleRequest.containsKey("location")) {
                schedule.setLocation((String) scheduleRequest.get("location"));
            }
            
            // 处理时间字段更新
            if (scheduleRequest.containsKey("date") && scheduleRequest.containsKey("time")) {
                String dateStr = (String) scheduleRequest.get("date");
                String timeStr = (String) scheduleRequest.get("time");
                
                if (dateStr != null && timeStr != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    Date scheduleTime = sdf.parse(dateStr + " " + timeStr);
                    schedule.setTime(scheduleTime);
                }
            }
            
            boolean success = scheduleService.updateSchedule(schedule);
            
            if (success) {
                response.put("success", true);
                response.put("message", "日程更新成功");
                // 返回更新后的完整日程信息
                Schedule updatedSchedule = scheduleService.getScheduleById(id);
                response.put("data", updatedSchedule);
            } else {
                response.put("success", false);
                response.put("message", "日程更新失败");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "日程更新失败：" + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 删除日程（原有接口，保持兼容性）
     * @param id 日程ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteSchedule(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean success = scheduleService.deleteSchedule(id);
            
            if (success) {
                response.put("success", true);
                response.put("message", "日程删除成功");
            } else {
                response.put("success", false);
                response.put("message", "日程删除失败");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "日程删除失败：" + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 仅删除当前日程（重复日程场景）
     * @param id 日程ID
     * @return 删除结果
     */
    @DeleteMapping("/current/{id}")
    public ResponseEntity<Map<String, Object>> deleteCurrentScheduleOnly(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean success = scheduleService.deleteCurrentScheduleOnly(id);
            
            if (success) {
                response.put("success", true);
                response.put("message", "当前日程删除成功");
            } else {
                response.put("success", false);
                response.put("message", "当前日程删除失败");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "当前日程删除失败：" + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 删除所有相关重复日程
     * @param id 日程ID
     * @return 删除结果
     */
    @DeleteMapping("/all/{id}")
    public ResponseEntity<Map<String, Object>> deleteAllRelatedSchedules(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean success = scheduleService.deleteAllRelatedSchedules(id);
            
            if (success) {
                response.put("success", true);
                response.put("message", "所有相关日程删除成功");
            } else {
                response.put("success", false);
                response.put("message", "删除相关日程失败");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "删除相关日程失败：" + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 获取用户未来的日程列表（首页展示用）
     * @param userId 用户ID
     * @param limit 限制返回的数量，默认5
     * @return 未来日程列表
     */
    @GetMapping("/upcoming")
    public ResponseEntity<Map<String, Object>> getUpcomingSchedules(
            @RequestParam Integer userId,
            @RequestParam(defaultValue = "5") Integer limit) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Schedule> upcomingSchedules = scheduleService.getUpcomingSchedulesByUserId(userId, limit);
            
            response.put("success", true);
            response.put("data", upcomingSchedules);
            response.put("message", "获取近期日程成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取近期日程失败：" + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 获取日程详情
     * @param id 日程ID
     * @return 日程详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getScheduleDetail(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Schedule schedule = scheduleService.getScheduleById(id);
            
            if (schedule != null) {
                response.put("success", true);
                response.put("data", schedule);
                response.put("message", "获取日程详情成功");
            } else {
                response.put("success", false);
                response.put("message", "日程不存在");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取日程详情失败：" + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
