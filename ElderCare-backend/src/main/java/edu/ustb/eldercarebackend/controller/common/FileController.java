package edu.ustb.eldercarebackend.controller.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件操作控制器
 * 处理转录文件、总结文件的读取和管理
 */
@RestController
@RequestMapping("/api/files")
public class FileController {
    
    // 添加一个测试接口确认控制器工作正常
    @GetMapping("/test")
    public Map<String, Object> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "FileController is working!");
        logger.info("FileController test endpoint called");
        return response;
    }
    
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);
    
    @Value("${app.user-data.root-path}")
    private String userDataRootPath;
    
    @Value("${app.user-data.users-dir}")
    private String usersDir;
    
    /**
     * 获取转录文件内容
     * @param userId 用户ID
     * @param fileName 文件名
     * @return 文件内容
     */
    @GetMapping("/transcript/{userId}/{fileName:.+}")
    public Map<String, Object> getTranscriptFile(@PathVariable String userId, @PathVariable String fileName) {
        Map<String, Object> response = new HashMap<>();
        
        logger.info("收到获取转录文件请求 - 用户ID: {}, 文件名: {}", userId, fileName);
        
        try {
            String userDir = userDataRootPath + File.separator + usersDir + File.separator + "user_" + userId;
            String transcriptDir = userDir + File.separator + "transcripts";
            Path filePath = Paths.get(transcriptDir, fileName);
            
            logger.info("构建的文件路径: {}", filePath);
            
            if (!Files.exists(filePath)) {
                logger.warn("转录文件不存在: {}", filePath);
                response.put("success", false);
                response.put("message", "转录文件不存在: " + filePath);
                return response;
            }
            
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            logger.info("成功读取转录文件内容，长度: {}", content.length());
            
            response.put("success", true);
            response.put("content", content);
            response.put("fileName", fileName);
            
            logger.info("成功获取转录文件 - 用户: {}, 文件: {}", userId, fileName);
            
        } catch (Exception e) {
            logger.error("获取转录文件失败 - 用户: {}, 文件: {}", userId, fileName, e);
            response.put("success", false);
            response.put("message", "获取转录文件失败: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 获取总结文件内容
     * @param userId 用户ID
     * @param fileName 原转录文件名（将自动转换为总结文件名）
     * @return 总结文件内容
     */
    @GetMapping("/summary/{userId}/{fileName:.+}")
    public Map<String, Object> getSummaryFile(@PathVariable String userId, @PathVariable String fileName) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 将转录文件名转换为总结文件名
            String summaryFileName = fileName.replace("record_", "summary_");
            
            String userDir = userDataRootPath + File.separator + usersDir + File.separator + "user_" + userId;
            String summaryDir = userDir + File.separator + "remote_assistance_AI_summary";
            Path filePath = Paths.get(summaryDir, summaryFileName);
            
            if (!Files.exists(filePath)) {
                response.put("success", false);
                response.put("message", "总结文件不存在，请先生成AI总结");
                response.put("needGenerate", true);
                return response;
            }
            
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            
            // 跳过表头，从第6行开始显示AI总结内容
            String actualSummary = extractSummaryContent(content);
            
            response.put("success", true);
            response.put("content", actualSummary);
            response.put("fileName", summaryFileName);
            
            logger.info("成功获取总结文件 - 用户: {}, 文件: {}", userId, summaryFileName);
            
        } catch (Exception e) {
            logger.error("获取总结文件失败 - 用户: {}, 文件: {}", userId, fileName, e);
            response.put("success", false);
            response.put("message", "获取总结文件失败: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 将总结文件复制到知识库目录
     * @param userId 用户ID
     * @param fileName 原转录文件名
     * @return 操作结果
     */
    @PostMapping("/copy-to-rag/{userId}/{fileName:.+}")
    public Map<String, Object> copyToRagSources(@PathVariable String userId, @PathVariable String fileName) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 构建源文件路径（总结文件）
            String summaryFileName = fileName.replace("record_", "summary_");
            String userDir = userDataRootPath + File.separator + usersDir + File.separator + "user_" + userId;
            String summaryDir = userDir + File.separator + "remote_assistance_AI_summary";
            Path sourcePath = Paths.get(summaryDir, summaryFileName);
            
            if (!Files.exists(sourcePath)) {
                response.put("success", false);
                response.put("message", "总结文件不存在，无法加入知识库");
                return response;
            }
            
            // 构建目标目录和文件路径
            String ragDir = userDir + File.separator + "rag_sources";
            File ragDirectory = new File(ragDir);
            if (!ragDirectory.exists()) {
                boolean created = ragDirectory.mkdirs();
                if (!created) {
                    response.put("success", false);
                    response.put("message", "无法创建知识库目录");
                    return response;
                }
                logger.info("创建知识库目录: {}", ragDir);
            }
            
            Path targetPath = Paths.get(ragDir, summaryFileName);
            
            // 复制文件到知识库目录
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            response.put("success", true);
            response.put("message", "已成功将协助记录加入个人知识库");
            response.put("targetFile", targetPath.toString());
            
            logger.info("成功复制总结文件到知识库 - 用户: {}, 源文件: {}, 目标文件: {}", 
                       userId, sourcePath, targetPath);
            
        } catch (Exception e) {
            logger.error("复制总结文件到知识库失败 - 用户: {}, 文件: {}", userId, fileName, e);
            response.put("success", false);
            response.put("message", "加入知识库失败: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 提取总结文件的实际内容，跳过表头
     * @param fullContent 完整的文件内容
     * @return 实际的AI总结内容
     */
    private String extractSummaryContent(String fullContent) {
        try {
            String[] lines = fullContent.split("\n");
            
            // 总结文件的表头格式：
            // 第1行: === 远程协助对话总结 ===
            // 第2行: 总结时间: ...
            // 第3行: 原文件: ...
            // 第4行: =====================================
            // 第5行: (空行)
            // 第6行开始: 实际的AI总结内容
            
            if (lines.length <= 5) {
                // 如果行数不足，返回原内容
                return fullContent;
            }
            
            StringBuilder actualContent = new StringBuilder();
            for (int i = 5; i < lines.length; i++) { // 从第6行开始（索引5）
                if (i > 5) { // 第一行后添加换行符
                    actualContent.append("\n");
                }
                actualContent.append(lines[i]);
            }
            
            String result = actualContent.toString().trim();
            logger.info("成功提取AI总结内容，原长度: {}, 提取后长度: {}", fullContent.length(), result.length());
            
            return result;
            
        } catch (Exception e) {
            logger.warn("提取总结内容失败，返回原内容: {}", e.getMessage());
            return fullContent;
        }
    }
}
