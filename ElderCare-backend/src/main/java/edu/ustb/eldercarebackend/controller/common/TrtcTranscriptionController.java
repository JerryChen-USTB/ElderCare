package edu.ustb.eldercarebackend.controller.common;

import edu.ustb.eldercarebackend.service.TrtcTranscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * TRTC转录控制器
 * 处理语音转文字转录任务的启动和停止
 */
@RestController
@RequestMapping("/api/trtc/transcription")
public class TrtcTranscriptionController {
    
    private static final Logger logger = LoggerFactory.getLogger(TrtcTranscriptionController.class);
    
    @Autowired
    private TrtcTranscriptionService transcriptionService;
    
    
    /**
     * 启动转录任务
     * @param roomId 房间ID
     * @param requestBody 包含robotUserSig、transcriptFileName、elderId的请求体
     */
    @PostMapping("/start/{roomId}")
    public Map<String, Object> startTranscription(
            @PathVariable String roomId, 
            @RequestBody Map<String, Object> requestBody) {
        Map<String, Object> result = new HashMap<>();
        
        logger.info("收到启动转录请求 - RoomId: {}", roomId);
        
        try {
            // 从前端获取robotUserSig
            String robotUserSig = (String) requestBody.get("robotUserSig");
            if (robotUserSig == null || robotUserSig.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "robotUserSig参数缺失");
                return result;
            }
            
            // 获取转录文件名和用户ID
            String transcriptFileName = (String) requestBody.get("transcriptFileName");
            Object elderIdObj = requestBody.get("elderId");
            String elderId = elderIdObj != null ? elderIdObj.toString() : null;
            
            logger.info("使用前端提供的robotUserSig启动转录任务，文件名: {}, 用户ID: {}", 
                       transcriptFileName, elderId);
            
            // 启动转录任务
            String taskId = transcriptionService.startTranscription(roomId, robotUserSig, transcriptFileName, elderId);
            
            if (taskId != null) {
                result.put("success", true);
                result.put("message", "转录任务启动成功");
                result.put("taskId", taskId);
                result.put("roomId", roomId);
                
                logger.info("转录任务启动成功 - RoomId: {}, TaskId: {}", roomId, taskId);
            } else {
                result.put("success", false);
                result.put("message", "转录任务启动失败");
                
                logger.error("转录任务启动失败 - RoomId: {}", roomId);
            }
            
        } catch (Exception e) {
            logger.error("启动转录任务异常 - RoomId: {}, Error: {}", roomId, e.getMessage(), e);
            result.put("success", false);
            result.put("message", "启动转录任务异常: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 停止转录任务
     * @param roomId 房间ID
     */
    @PostMapping("/stop/{roomId}")
    public Map<String, Object> stopTranscription(@PathVariable String roomId) {
        Map<String, Object> result = new HashMap<>();
        
        logger.info("收到停止转录请求 - RoomId: {}", roomId);
        
        try {
            boolean success = transcriptionService.stopTranscription(roomId);
            
            if (success) {
                result.put("success", true);
                result.put("message", "转录任务停止成功");
                result.put("roomId", roomId);
                
                logger.info("转录任务停止成功 - RoomId: {}", roomId);
            } else {
                result.put("success", false);
                result.put("message", "转录任务停止失败或任务不存在");
                
                logger.warn("转录任务停止失败 - RoomId: {}", roomId);
            }
            
        } catch (Exception e) {
            logger.error("停止转录任务异常 - RoomId: {}, Error: {}", roomId, e.getMessage(), e);
            result.put("success", false);
            result.put("message", "停止转录任务异常: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 通过TaskId停止转录任务
     * @param taskId 任务ID
     */
    @PostMapping("/stop-by-taskid/{taskId}")
    public Map<String, Object> stopTranscriptionByTaskId(@PathVariable String taskId) {
        Map<String, Object> result = new HashMap<>();
        
        logger.info("收到通过TaskId停止转录请求 - TaskId: {}", taskId);
        
        try {
            boolean success = transcriptionService.stopTranscriptionByTaskId(taskId, "unknown");
            
            if (success) {
                result.put("success", true);
                result.put("message", "转录任务停止成功");
                result.put("taskId", taskId);
            } else {
                result.put("success", false);
                result.put("message", "转录任务停止失败");
            }
            
        } catch (Exception e) {
            logger.error("停止转录任务异常 - TaskId: {}, Error: {}", taskId, e.getMessage(), e);
            result.put("success", false);
            result.put("message", "停止转录任务异常: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取转录状态信息
     */
    @GetMapping("/status")
    public Map<String, Object> getTranscriptionStatus() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            int activeTaskCount = transcriptionService.getActiveTaskCount();
            
            result.put("success", true);
            result.put("activeTaskCount", activeTaskCount);
            result.put("message", "获取状态成功");
            
        } catch (Exception e) {
            logger.error("获取转录状态异常: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "获取状态失败: " + e.getMessage());
        }
        
        return result;
    }
}
