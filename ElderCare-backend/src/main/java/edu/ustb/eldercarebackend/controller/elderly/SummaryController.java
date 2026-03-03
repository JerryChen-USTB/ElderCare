package edu.ustb.eldercarebackend.controller.elderly;

import edu.ustb.eldercarebackend.service.elderly.SummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * AI总结控制器
 * 处理远程协助对话内容总结
 */
@RestController
@RequestMapping("/api/summary")
public class SummaryController {
    
    private static final Logger logger = LoggerFactory.getLogger(SummaryController.class);
    
    @Autowired
    private SummaryService summaryService;
    
    @Value("${app.user-data.root-path}")
    private String userDataRootPath;
    
    @Value("${app.user-data.users-dir}")
    private String usersDir;
    
    /**
     * 总结远程协助对话内容
     * @param requestData 包含用户ID和转录文件名的请求数据
     * @return 总结结果
     */
    @PostMapping("/assistance")
    public Map<String, Object> summarizeAssistance(@RequestBody Map<String, String> requestData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String userId = requestData.get("userId");
            String transcriptFileName = requestData.get("transcriptFileName");
            
            if (userId == null || userId.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "用户ID不能为空");
                return response;
            }
            
            if (transcriptFileName == null || transcriptFileName.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "转录文件名不能为空");
                return response;
            }
            
            logger.info("开始总结远程协助对话 - 用户ID: {}, 文件名: {}", userId, transcriptFileName);
            
            // 读取转录文件内容
            String transcriptContent = readTranscriptFile(userId, transcriptFileName);
            if (transcriptContent == null) {
                response.put("success", false);
                response.put("message", "无法读取转录文件");
                return response;
            }
            
            // 调用AI进行总结
            String summary = summaryService.summarizeConversation(transcriptContent);
            
            // 保存总结结果
            boolean saveSuccess = saveSummaryResult(userId, transcriptFileName, summary);
            if (!saveSuccess) {
                response.put("success", false);
                response.put("message", "保存总结结果失败");
                return response;
            }
            
            response.put("success", true);
            response.put("message", "对话总结完成");
            response.put("summary", summary);
            
            logger.info("远程协助对话总结完成 - 用户ID: {}, 文件名: {}", userId, transcriptFileName);
            
        } catch (Exception e) {
            logger.error("总结远程协助对话失败", e);
            response.put("success", false);
            response.put("message", "总结服务暂时不可用：" + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 读取转录文件内容
     * @param userId 用户ID
     * @param transcriptFileName 转录文件名
     * @return 文件内容，失败返回null
     */
    private String readTranscriptFile(String userId, String transcriptFileName) {
        try {
            // 构建转录文件路径
            String userDir = userDataRootPath + File.separator + usersDir + File.separator + "user_" + userId;
            String transcriptDir = userDir + File.separator + "transcripts";
            Path filePath = Paths.get(transcriptDir, transcriptFileName);
            
            if (!Files.exists(filePath)) {
                logger.error("转录文件不存在: {}", filePath);
                return null;
            }
            
            // 读取文件内容
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            logger.info("成功读取转录文件: {}, 内容长度: {}", filePath, content.length());
            
            return content;
            
        } catch (IOException e) {
            logger.error("读取转录文件失败 - 用户ID: {}, 文件名: {}", userId, transcriptFileName, e);
            return null;
        }
    }
    
    /**
     * 保存总结结果到文件
     * @param userId 用户ID
     * @param transcriptFileName 原转录文件名
     * @param summary 总结内容
     * @return 是否保存成功
     */
    private boolean saveSummaryResult(String userId, String transcriptFileName, String summary) {
        try {
            // 构建用户目录路径
            String userDir = userDataRootPath + File.separator + usersDir + File.separator + "user_" + userId;
            String summaryDir = userDir + File.separator + "remote_assistance_AI_summary";
            
            // 确保总结目录存在
            File summaryDirectory = new File(summaryDir);
            if (!summaryDirectory.exists()) {
                boolean created = summaryDirectory.mkdirs();
                if (created) {
                    logger.info("创建AI总结目录: {}", summaryDir);
                } else {
                    logger.error("无法创建AI总结目录: {}", summaryDir);
                    return false;
                }
            }
            
            // 生成总结文件名（将record替换为summary）
            String summaryFileName = transcriptFileName.replace("record_", "summary_");
            File summaryFile = new File(summaryDir, summaryFileName);
            
            // 写入总结内容
            try (FileWriter writer = new FileWriter(summaryFile, StandardCharsets.UTF_8)) {
                writer.write("=== 远程协助对话总结 ===\n");
                writer.write("总结时间: " + java.time.LocalDateTime.now() + "\n");
                writer.write("原文件: " + transcriptFileName + "\n");
                writer.write("=====================================\n\n");
                writer.write(summary);
                writer.flush();
                
                logger.info("AI总结结果已保存到: {}", summaryFile.getAbsolutePath());
                System.out.println("📝 AI总结结果已保存到: " + summaryFile.getAbsolutePath());
                
                return true;
            }
            
        } catch (IOException e) {
            logger.error("保存总结结果失败 - 用户ID: {}, 文件名: {}", userId, transcriptFileName, e);
            return false;
        }
    }
}
