package edu.ustb.eldercarebackend.controller.common;

import edu.ustb.eldercarebackend.dto.TrtcCallbackDto;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import edu.ustb.eldercarebackend.service.TrtcTranscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 腾讯云TRTC语音转文字回调控制器（简化版）
 * 专注处理语音转文字回调，输出识别的句子到控制台
 */
@RestController
@RequestMapping("/trtc")
public class TrtcCallbackController {
    
    private static final Logger logger = LoggerFactory.getLogger(TrtcCallbackController.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private TrtcTranscriptionService transcriptionService;
    
    @Value("${app.user-data.root-path}")
    private String userDataRootPath;
    
    @Value("${app.user-data.users-dir}")
    private String usersDir;
    
    @Value("${tencent.cloud.trtc.sdk-app-id}")
    private Long sdkAppId;
    
    @Value("${tencent.cloud.trtc.secret-key}")
    private String secretKey;
    
    
    /**
     * 接收TRTC回调
     * 快速响应，避免腾讯云重试机制
     */
    @PostMapping("/callback")
    public ResponseEntity<Map<String, Object>> receiveCallback(@RequestBody String requestBody) {
        // 立即构建响应，确保5秒内返回
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        
        try {
            // 异步处理回调内容
            processCallback(requestBody);
        } catch (Exception e) {
            logger.error("处理回调时发生异常", e);
        }
        
        // 无论是否有异常，都返回200状态码
        return ResponseEntity.ok(response);
    }
    
    /**
     * 处理回调内容
     */
    private void processCallback(String requestBody) {
        new Thread(() -> {
            try {
                TrtcCallbackDto callback = objectMapper.readValue(requestBody, TrtcCallbackDto.class);
                
                System.out.println("=== TRTC回调 [" + LocalDateTime.now() + "] ===");
                System.out.println("事件类型: " + callback.getEventType() + " (" + getEventTypeName(callback.getEventType()) + ")");
                System.out.println("房间ID: " + (callback.getEventInfo() != null ? callback.getEventInfo().getRoomId() : "未知"));
                
                // 专门处理语音转文字回调(EventType 903)
                if (callback.getEventType() != null && callback.getEventType() == 903) {
                    handleSpeechToText(callback, requestBody);
                }
                
                System.out.println("========================================");
                
            } catch (Exception e) {
                logger.error("解析回调数据失败", e);
                // 如果解析失败，至少输出原始内容
                System.err.println("回调解析失败，原始内容: " + requestBody);
            }
        }).start();
    }
    
    /**
     * 处理语音转文字回调 - 输出识别的句子和原始回调内容
     */
    private void handleSpeechToText(TrtcCallbackDto callback, String rawRequestBody) {
        try {
            TrtcCallbackDto.EventInfo eventInfo = callback.getEventInfo();
            if (eventInfo != null && eventInfo.getPayload() != null) {
                
                // 解析Payload中的Text字段
                JsonNode payloadNode = objectMapper.valueToTree(eventInfo.getPayload());
                String text = payloadNode.get("Text") != null ? payloadNode.get("Text").asText() : null;
                String userId = payloadNode.get("UserId") != null ? payloadNode.get("UserId").asText() : "未知用户";
                
                if (text != null && !text.trim().isEmpty()) {
                    System.out.println("🗣️ 语音转文字结果:");
                    System.out.println("用户: " + userId);
                    System.out.println("识别内容: " + text);
                    
                    // 根据房间信息保存到对应的用户文件
                    String roomId = eventInfo.getRoomId() != null ? eventInfo.getRoomId().toString() : null;
                    saveSpeechRecord(roomId, userId, text);
                    
                    // 输出原始回调内容
                    System.out.println("--- 原始回调内容 ---");
                    System.out.println(rawRequestBody);
                    System.out.println("--- 原始内容结束 ---");
                    
                    // 记录到日志
                    logger.info("语音转文字 - 用户: {}, 内容: {}", userId, text);
                } else {
                    System.out.println("⚠️ 语音转文字回调中未找到文字内容");
                    // 即使没有文字内容，也输出原始回调便于调试
                    System.out.println("--- 原始回调内容 ---");
                    System.out.println(rawRequestBody);
                    System.out.println("--- 原始内容结束 ---");
                }
            }
        } catch (Exception e) {
            logger.error("处理语音转文字回调失败", e);
            System.err.println("处理语音转文字回调失败: " + e.getMessage());
            // 发生异常时也输出原始内容便于调试
            System.err.println("--- 原始回调内容 ---");
            System.err.println(rawRequestBody);
            System.err.println("--- 原始内容结束 ---");
        }
    }
    
    /**
     * 获取事件类型名称
     */
    private String getEventTypeName(Integer eventType) {
        if (eventType == null) return "未知";
        
        switch (eventType) {
            case 901: return "AI任务开始";
            case 902: return "AI任务结束";
            case 903: return "语音转文字";
            case 904: return "用户事件";
            case 906: return "性能指标";
            default: return "其他事件";
        }
    }
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");
        result.put("message", "TRTC回调服务运行正常");
        result.put("timestamp", System.currentTimeMillis());
        
        System.out.println("健康检查 - " + LocalDateTime.now());
        
        return result;
    }
    
    /**
     * 获取TRTC配置信息
     * 供前端获取SDK配置，避免敏感信息硬编码在前端
     */
    @GetMapping("/config")
    public Map<String, Object> getTrtcConfig() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("sdkAppId", sdkAppId);
        result.put("secretKey", secretKey);
        
        logger.info("前端请求TRTC配置");
        
        return result;
    }
    
    /**
     * 确保用户转录目录存在
     */
    private void ensureUserTranscriptDirectory(String elderId) {
        try {
            String userDir = userDataRootPath + File.separator + usersDir + File.separator + "user_" + elderId;
            String transcriptDir = userDir + File.separator + "transcripts";
            
            File directory = new File(transcriptDir);
            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                if (created) {
                    System.out.println("创建用户转录目录: " + transcriptDir);
                    logger.info("创建用户转录目录: {}", transcriptDir);
                } else {
                    System.err.println("无法创建用户转录目录: " + transcriptDir);
                    logger.error("无法创建用户转录目录: {}", transcriptDir);
                }
            }
        } catch (Exception e) {
            System.err.println("创建用户转录目录时发生异常: " + e.getMessage());
            logger.error("创建用户转录目录异常", e);
        }
    }
    
    /**
     * 保存语音转文字记录到用户特定文件
     * 格式：UserId: Text
     * 每次回调追加一行到指定的转录文件
     */
    private void saveSpeechRecord(String roomId, String userId, String text) {
        try {
            // 通过房间ID获取转录文件信息
            TrtcTranscriptionService.TranscriptFileInfo fileInfo = null;
            if (roomId != null) {
                fileInfo = transcriptionService.getFileInfoByRoomId(roomId);
            }
            
            if (fileInfo == null) {
                System.err.println("⚠️ 无法获取转录文件信息，房间ID: " + roomId);
                logger.warn("无法获取转录文件信息，房间ID: {}", roomId);
                return;
            }
            
            String elderId = fileInfo.getElderId();
            String fileName = fileInfo.getFileName();
            
            // 确保用户转录目录存在
            ensureUserTranscriptDirectory(elderId);
            
            // 构建文件路径
            String userDir = userDataRootPath + File.separator + usersDir + File.separator + "user_" + elderId;
            String transcriptDir = userDir + File.separator + "transcripts";
            File recordFile = new File(transcriptDir, fileName);
            
            // 判断说话者角色：发起通话的是老年人，接通的是志愿者
            String speakerRole = getSpeakerRole(userId, elderId);
            
            // 使用FileWriter追加模式写入文件
            try (FileWriter writer = new FileWriter(recordFile, StandardCharsets.UTF_8, true)) {
                String record = speakerRole + ": " + text + System.lineSeparator();
                writer.write(record);
                writer.flush();
                
                System.out.println("📝 语音记录已保存到: " + recordFile.getAbsolutePath());
                logger.info("语音记录已保存 - 用户: {} ({}), 房间: {}, 文件: {}", userId, speakerRole, roomId, recordFile.getAbsolutePath());
            }
            
        } catch (IOException e) {
            System.err.println("保存语音记录时发生异常: " + e.getMessage());
            logger.error("保存语音记录异常 - 房间: {}, 用户: {}, 内容: {}", roomId, userId, text, e);
        }
    }
    
    /**
     * 根据用户ID判断说话者角色
     * @param userId 当前说话的用户ID
     * @param elderId 发起通话的老年人用户ID  
     * @return 角色标识：老年人或志愿者
     */
    private String getSpeakerRole(String userId, String elderId) {
        if (userId != null && elderId != null && userId.equals(elderId)) {
            return "老年人";
        } else {
            return "志愿者";
        }
    }
}
