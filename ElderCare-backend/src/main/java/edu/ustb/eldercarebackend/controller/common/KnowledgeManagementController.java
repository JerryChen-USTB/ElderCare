package edu.ustb.eldercarebackend.controller.common;

import edu.ustb.eldercarebackend.service.elderly.MultiTenantChatService;
import edu.ustb.eldercarebackend.service.knowledge.GlobalKnowledgeManager;
import edu.ustb.eldercarebackend.service.knowledge.UserKnowledgeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 知识库管理控制器
 * 提供知识库管理和诊断接口
 */
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeManagementController {
    
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeManagementController.class);
    
    @Autowired
    private GlobalKnowledgeManager globalKnowledgeManager;
    
    @Autowired
    private UserKnowledgeManager userKnowledgeManager;
    
    @Autowired
    private MultiTenantChatService multiTenantChatService;
    
    /**
     * 获取知识库状态
     */
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> result = new HashMap<>();
        
        result.put("success", true);
        result.put("globalKnowledge", Map.of(
            "initialized", globalKnowledgeManager.isInitialized(),
            "status", globalKnowledgeManager.isInitialized() ? "已就绪" : "加载中"
        ));
        result.put("cacheStats", multiTenantChatService.getCacheStats());
        result.put("timestamp", System.currentTimeMillis());
        
        logger.info("📊 知识库状态查询");
        
        return result;
    }
    
    /**
     * 清除用户知识库缓存
     * @param userId 用户ID
     */
    @PostMapping("/user/{userId}/clear")
    public Map<String, Object> clearUserCache(@PathVariable String userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            multiTenantChatService.clearUserCache(userId);
            
            result.put("success", true);
            result.put("message", "用户知识库缓存已清除: user_" + userId);
            
            logger.info("🗑️ 清除用户缓存: user_{}", userId);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "清除缓存失败: " + e.getMessage());
            
            logger.error("❌ 清除用户缓存失败: user_{}", userId, e);
        }
        
        return result;
    }
    
    /**
     * 重新加载用户知识库
     * @param userId 用户ID
     */
    @PostMapping("/user/{userId}/reload")
    public Map<String, Object> reloadUserKnowledge(@PathVariable String userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 先清除缓存
            multiTenantChatService.clearUserCache(userId);
            
            // 异步重新加载
            userKnowledgeManager.loadUserKnowledgeAsync(Long.valueOf(userId));
            
            result.put("success", true);
            result.put("message", "用户知识库重新加载已触发: user_" + userId);
            
            logger.info("🔄 重新加载用户知识库: user_{}", userId);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "重新加载失败: " + e.getMessage());
            
            logger.error("❌ 重新加载用户知识库失败: user_{}", userId, e);
        }
        
        return result;
    }
}

