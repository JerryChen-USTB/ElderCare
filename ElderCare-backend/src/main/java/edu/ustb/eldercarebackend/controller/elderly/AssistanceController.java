package edu.ustb.eldercarebackend.controller.elderly;

import edu.ustb.eldercarebackend.entity.Assistance;
import edu.ustb.eldercarebackend.service.elderly.AssistanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 远程协助控制器
 */
@RestController
@RequestMapping("/api/assistance")
@CrossOrigin(origins = "*")
public class AssistanceController {

    @Autowired
    private AssistanceService assistanceService;

    /**
     * 获取用户当前的活跃协助项目
     */
    @GetMapping("/active/{elderId}")
    public Map<String, Object> getActiveAssistance(@PathVariable Integer elderId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            AssistanceService.AssistanceDetailVO detail = assistanceService.getAssistanceDetailByElderId(elderId);
            
            if (detail != null) {
                result.put("success", true);
                result.put("data", detail);
            } else {
                // 没有活跃项目，返回默认的waiting_apply状态
                result.put("success", true);
                Map<String, Object> defaultData = new HashMap<>();
                defaultData.put("status", "waiting_apply");
                defaultData.put("appointmentContent", "");
                defaultData.put("location", "无法获取定位");
                result.put("data", defaultData);
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取协助信息失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 提交远程协助申请
     */
    @PostMapping("/submit")
    public Map<String, Object> submitAssistanceRequest(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Integer elderId = (Integer) request.get("elderId");
            String content = (String) request.get("content");
            
            if (elderId == null || content == null || content.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "请填写完整的申请信息");
                return result;
            }

            Assistance assistance = assistanceService.submitAssistanceRequest(elderId, content.trim());
            
            if (assistance != null) {
                result.put("success", true);
                result.put("message", "申请提交成功");
                result.put("data", assistance);
            } else {
                result.put("success", false);
                result.put("message", "申请提交失败");
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        
        return result;
    }

    /**
     * 开始呼叫
     */
    @PostMapping("/call/start/{assistanceId}")
    public Map<String, Object> startCall(@PathVariable Integer assistanceId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Assistance assistance = assistanceService.startCall(assistanceId);
            
            if (assistance != null) {
                result.put("success", true);
                result.put("message", "开始呼叫...");
                result.put("data", assistance);
            } else {
                result.put("success", false);
                result.put("message", "发起呼叫失败");
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        
        return result;
    }

    /**
     * 呼叫失败
     */
    @PostMapping("/call/failed/{assistanceId}")
    public Map<String, Object> callFailed(@PathVariable Integer assistanceId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Assistance assistance = assistanceService.callFailed(assistanceId);
            
            if (assistance != null) {
                result.put("success", true);
                result.put("message", "呼叫失败，已返回等待状态");
                result.put("data", assistance);
            } else {
                result.put("success", false);
                result.put("message", "处理呼叫失败失败");
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        
        return result;
    }

    /**
     * 呼叫失败，返回指定状态
     */
    @PostMapping("/call/failed/{assistanceId}/{targetStatus}")
    public Map<String, Object> callFailedWithStatus(@PathVariable Integer assistanceId, @PathVariable String targetStatus) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Assistance assistance = assistanceService.callFailedWithStatus(assistanceId, targetStatus);
            
            if (assistance != null) {
                result.put("success", true);
                result.put("message", "呼叫失败，已返回到状态: " + targetStatus);
                result.put("data", assistance);
            } else {
                result.put("success", false);
                result.put("message", "处理呼叫失败失败");
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        
        return result;
    }

    /**
     * 志愿者接通呼叫，开始协助
     */
    @PostMapping("/start/{assistanceId}")
    public Map<String, Object> startAssistance(@PathVariable Integer assistanceId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Assistance assistance = assistanceService.startAssistance(assistanceId);
            
            if (assistance != null) {
                result.put("success", true);
                result.put("message", "协助开始");
                result.put("data", assistance);
            } else {
                result.put("success", false);
                result.put("message", "开始协助失败");
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        
        return result;
    }

    /**
     * 结束协助
     */
    @PostMapping("/end/{assistanceId}")
    public Map<String, Object> endAssistance(@PathVariable Integer assistanceId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Assistance assistance = assistanceService.endAssistance(assistanceId);
            
            if (assistance != null) {
                result.put("success", true);
                result.put("message", "协助已结束");
                result.put("data", assistance);
            } else {
                result.put("success", false);
                result.put("message", "结束协助失败");
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        
        return result;
    }

    /**
     * 取消协助
     */
    @PostMapping("/cancel/{assistanceId}")
    public Map<String, Object> cancelAssistance(@PathVariable Integer assistanceId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Assistance assistance = assistanceService.cancelAssistance(assistanceId);
            
            if (assistance != null) {
                result.put("success", true);
                result.put("message", "协助已取消");
                result.put("data", assistance);
            } else {
                result.put("success", false);
                result.put("message", "取消协助失败");
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        
        return result;
    }

    /**
     * 更新状态为 waiting_call_again（通话结束后可再次呼叫）
     */
    @PostMapping("/waiting-call-again/{assistanceId}")
    public Map<String, Object> updateToWaitingCallAgain(@PathVariable Integer assistanceId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Assistance assistance = assistanceService.updateToWaitingCallAgain(assistanceId);
            
            if (assistance != null) {
                result.put("success", true);
                result.put("message", "可以再次进行呼叫或完成协助");
                result.put("data", assistance);
            } else {
                result.put("success", false);
                result.put("message", "状态更新失败");
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        
        return result;
    }

    /**
     * 更新协助内容
     */
    @PostMapping("/update-content")
    public Map<String, Object> updateAssistanceContent(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Integer elderId = (Integer) request.get("elderId");
            String content = (String) request.get("content");
            
            if (elderId == null || content == null || content.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "请填写协助内容");
                return result;
            }

            // 通过Service更新协助内容
            boolean updated = assistanceService.updateAssistanceContent(elderId, content);
            
            if (updated) {
                result.put("success", true);
                result.put("message", "协助内容更新成功");
            } else {
                result.put("success", false);
                result.put("message", "协助内容更新失败");
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "更新协助内容失败: " + e.getMessage());
        }
        
        return result;
    }
}
