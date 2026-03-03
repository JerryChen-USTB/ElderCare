package edu.ustb.eldercarebackend.controller.volunteer;

import edu.ustb.eldercarebackend.entity.Assistance;
import edu.ustb.eldercarebackend.mapper.AssistanceMapper;
import edu.ustb.eldercarebackend.service.volunteer.VolunteerAssistanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 志愿者远程协助控制器
 */
@RestController
@RequestMapping("/api/volunteer/assistance")
public class VolunteerAssistanceController {
    
    private static final Logger logger = LoggerFactory.getLogger(VolunteerAssistanceController.class);
    
    @Autowired
    private AssistanceMapper assistanceMapper;
    
    /**
     * 获取等待接单的协助列表
     * @return 待接单的协助列表
     */
    @GetMapping("/waiting")
    public Map<String, Object> getWaitingAssistances() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Assistance> assistances = assistanceMapper.findWaitingAssistances();
            List<Map<String, Object>> assistanceList = new ArrayList<>();
            
            for (Assistance assistance : assistances) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", assistance.getId());
                item.put("appointmentContent", assistance.getAppointmentContent());
                item.put("applyTime", assistance.getApplyTime());
                item.put("elderId", assistance.getElderId());
                
                // 获取老年人姓名
                String elderName = assistanceMapper.getElderNameById(assistance.getElderId());
                item.put("elderName", elderName != null ? elderName : "未知用户");
                
                assistanceList.add(item);
            }
            
            response.put("success", true);
            response.put("data", assistanceList);
            
            logger.info("获取待接单协助列表成功，共{}项", assistanceList.size());
            
        } catch (Exception e) {
            logger.error("获取待接单协助列表失败", e);
            response.put("success", false);
            response.put("message", "获取协助列表失败: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 志愿者接单
     * @param assistanceId 协助项目ID
     * @param volunteerId 志愿者ID
     * @return 接单结果
     */
    @PostMapping("/accept")
    public Map<String, Object> acceptAssistance(@RequestBody Map<String, Object> requestBody) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Integer assistanceId = (Integer) requestBody.get("assistanceId");
            Integer volunteerId = (Integer) requestBody.get("volunteerId");
            
            if (assistanceId == null || volunteerId == null) {
                response.put("success", false);
                response.put("message", "协助ID和志愿者ID不能为空");
                return response;
            }
            
            // 执行接单操作
            int updateCount = assistanceMapper.acceptAssistance(assistanceId, volunteerId);
            
            if (updateCount > 0) {
                response.put("success", true);
                response.put("message", "接单成功");
                
                logger.info("志愿者{}成功接单协助项目{}", volunteerId, assistanceId);
                
                // 返回协助详情
                Assistance assistance = assistanceMapper.selectById(assistanceId);
                if (assistance != null) {
                    Map<String, Object> assistanceInfo = new HashMap<>();
                    assistanceInfo.put("id", assistance.getId());
                    assistanceInfo.put("appointmentContent", assistance.getAppointmentContent());
                    assistanceInfo.put("applyTime", assistance.getApplyTime());
                    assistanceInfo.put("elderId", assistance.getElderId());
                    assistanceInfo.put("status", assistance.getStatus());
                    
                    // 获取老年人姓名
                    String elderName = assistanceMapper.getElderNameById(assistance.getElderId());
                    assistanceInfo.put("elderName", elderName != null ? elderName : "未知用户");
                    
                    response.put("assistanceInfo", assistanceInfo);
                }
                
            } else {
                response.put("success", false);
                response.put("message", "接单失败，该协助可能已被其他志愿者接单");
                
                logger.warn("志愿者{}接单协助项目{}失败，可能已被其他志愿者接单", volunteerId, assistanceId);
            }
            
        } catch (Exception e) {
            logger.error("志愿者接单失败", e);
            response.put("success", false);
            response.put("message", "接单失败: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 获取协助详情
     * @param assistanceId 协助项目ID
     * @return 协助详情
     */
    @GetMapping("/{assistanceId}")
    public Map<String, Object> getAssistanceDetail(@PathVariable Integer assistanceId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Assistance assistance = assistanceMapper.selectById(assistanceId);
            
            if (assistance == null) {
                response.put("success", false);
                response.put("message", "协助项目不存在");
                return response;
            }
            
            Map<String, Object> assistanceInfo = new HashMap<>();
            assistanceInfo.put("id", assistance.getId());
            assistanceInfo.put("appointmentContent", assistance.getAppointmentContent());
            assistanceInfo.put("applyTime", assistance.getApplyTime());
            assistanceInfo.put("elderId", assistance.getElderId());
            assistanceInfo.put("status", assistance.getStatus());
            assistanceInfo.put("startTime", assistance.getStartTime());
            assistanceInfo.put("endTime", assistance.getEndTime());
            
            // 获取老年人姓名
            String elderName = assistanceMapper.getElderNameById(assistance.getElderId());
            assistanceInfo.put("elderName", elderName != null ? elderName : "未知用户");
            
            response.put("success", true);
            response.put("data", assistanceInfo);
            
            logger.info("获取协助详情成功 - ID: {}", assistanceId);
            
        } catch (Exception e) {
            logger.error("获取协助详情失败 - ID: {}", assistanceId, e);
            response.put("success", false);
            response.put("message", "获取协助详情失败: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 检查志愿者是否有等待中的协助项目
     * @param volunteerId 志愿者ID
     * @return 等待中的协助项目信息
     */
    @GetMapping("/volunteer/{volunteerId}/waiting")
    public Map<String, Object> getVolunteerWaitingAssistance(@PathVariable Integer volunteerId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 查询志愿者当前等待中的协助项目
            Assistance waitingAssistance = assistanceMapper.findWaitingCallByVolunteerId(volunteerId);
            
            if (waitingAssistance != null) {
                Map<String, Object> assistanceInfo = new HashMap<>();
                assistanceInfo.put("id", waitingAssistance.getId());
                assistanceInfo.put("appointmentContent", waitingAssistance.getAppointmentContent());
                assistanceInfo.put("applyTime", waitingAssistance.getApplyTime());
                assistanceInfo.put("elderId", waitingAssistance.getElderId());
                assistanceInfo.put("status", waitingAssistance.getStatus());
                assistanceInfo.put("startTime", waitingAssistance.getStartTime());
                assistanceInfo.put("endTime", waitingAssistance.getEndTime());
                
                // 获取老年人姓名
                String elderName = assistanceMapper.getElderNameById(waitingAssistance.getElderId());
                assistanceInfo.put("elderName", elderName != null ? elderName : "未知用户");
                
                response.put("success", true);
                response.put("hasWaiting", true);
                response.put("data", assistanceInfo);
                
                logger.info("志愿者{}有等待中的协助项目 - ID: {}", volunteerId, waitingAssistance.getId());
            } else {
                response.put("success", true);
                response.put("hasWaiting", false);
                response.put("data", null);
                
                logger.info("志愿者{}没有等待中的协助项目", volunteerId);
            }
            
        } catch (Exception e) {
            logger.error("检查志愿者{}等待中协助项目失败", volunteerId, e);
            response.put("success", false);
            response.put("message", "检查等待中协助项目失败: " + e.getMessage());
        }
        
        return response;
    }


    @Autowired
    private VolunteerAssistanceService assistanceService;

    /**
     * 按 userId 查询所有远程协助记录（服务记录用）
     */
    @GetMapping("/records")
    public Map<String, Object> getAssistanceRecords(@RequestParam("userId") Integer userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Assistance> records = assistanceService.getByUserId(userId);
            result.put("success", true);
            result.put("data", records); // 仅返回实体字段，无额外名称
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "查询远程协助记录失败：" + e.getMessage());
        }
        return result;
    }

}
