package edu.ustb.eldercarebackend.controller.elderly;

import edu.ustb.eldercarebackend.service.elderly.MultiTenantChatService;
import edu.ustb.eldercarebackend.service.knowledge.UserKnowledgeManager;
import edu.ustb.eldercarebackend.util.ChatContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.data.message.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    @Autowired
    private MultiTenantChatService multiTenantChatService;
    
    @Autowired
    private UserKnowledgeManager userKnowledgeManager;
    
    @Autowired
    private ChatMemoryStore redisChatMemoryStore;
    
    /**
     * 初始化用户聊天环境
     * 用于用户进入聊天页面时预加载知识库
     * @param requestData 包含userId的请求数据
     * @return 标准JSON响应
     */
    @PostMapping("/initialize")
    public Map<String, Object> initializeChatEnvironment(@RequestBody Map<String, Object> requestData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Object userIdObj = requestData.get("userId");
            if (userIdObj == null) {
                response.put("success", false);
                response.put("message", "用户ID不能为空");
                return response;
            }
            
            String userId = userIdObj.toString();
            logger.info("🔄 用户进入聊天页面，开始初始化知识库: user_{}", userId);
            
            // 异步加载用户私有知识库
            userKnowledgeManager.loadUserKnowledgeAsync(Long.valueOf(userId));
            
            response.put("success", true);
            response.put("message", "知识库初始化已触发");
            response.put("userId", userId);
            
        } catch (Exception e) {
            logger.error("❌ 初始化聊天环境失败", e);
            response.put("success", false);
            response.put("message", "初始化失败: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 普通聊天接口（非流式）
     * @param requestData 包含message和memoryId的请求数据
     * @return 标准JSON响应
     */
    @PostMapping("/message")
    public Map<String, Object> chatMessage(@RequestBody Map<String, String> requestData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String message = requestData.get("message");
            String memoryId = requestData.get("memoryId");
            
            if (message == null || message.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "消息内容不能为空");
                return response;
            }
            
            // 如果没有提供memoryId，使用默认值
            if (memoryId == null || memoryId.trim().isEmpty()) {
                memoryId = "default-session";
            }
            
            // 从memoryId中提取userId (格式: "user_16")
            String userId = extractUserIdFromMemoryId(memoryId);
            logger.info("普通聊天请求: userId={}, memoryId={}", userId, memoryId);
            
            // 设置聊天上下文，用于Tool调用时获取用户信息
            ChatContext.setContext(memoryId);
            
            String fullResponse;
            try {
                // 使用多租户聊天服务（支持混合知识库检索）
                fullResponse = multiTenantChatService.chat(userId, memoryId, message);
            } finally {
                // 确保在方法结束时清除上下文
                ChatContext.clearContext();
            }
                    
            response.put("success", true);
            response.put("reply", fullResponse);
            response.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            System.err.println("聊天处理错误: " + e.getMessage());
            response.put("success", false);
            response.put("message", "聊天服务暂时不可用，请稍后再试");
        }
        
        return response;
    }

    /**
     * 清理用户会话记录接口
     * @param userId 用户ID
     * @return 操作结果
     */
    @DeleteMapping("/memory/user/{userId}")
    public Map<String, Object> clearUserChatMemory(@PathVariable String userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 构造用户固定的会话ID，格式为 chat_用户ID_persistent
            String userMemoryId = "chat_" + userId + "_persistent";
            
            // 删除用户的固定会话记录
            redisChatMemoryStore.deleteMessages(userMemoryId);
            
            response.put("success", true);
            response.put("message", "用户会话记录清理成功");
            
        } catch (Exception e) {
            System.err.println("清理用户会话记录错误: " + e.getMessage());
            response.put("success", false);
            response.put("message", "清理会话记录失败");
        }
        
        return response;
    }

    /**
     * 清理指定会话记录接口
     * @param requestData 包含memoryId的请求数据
     * @return 操作结果
     */
    @PostMapping("/memory/clear")
    public Map<String, Object> clearChatMemory(@RequestBody Map<String, String> requestData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String memoryId = requestData.get("memoryId");
            
            if (memoryId == null || memoryId.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "会话ID不能为空");
                return response;
            }
            
            // 删除指定的会话记录
            redisChatMemoryStore.deleteMessages(memoryId);
            
            response.put("success", true);
            response.put("message", "会话记录清理成功");
            
        } catch (Exception e) {
            System.err.println("清理会话记录错误: " + e.getMessage());
            response.put("success", false);
            response.put("message", "清理会话记录失败");
        }
        
        return response;
    }

    /**
     * 获取会话历史记录接口
     * @param requestData 包含memoryId的请求数据
     * @return 会话历史记录
     */
    @PostMapping("/memory/history")
    public Map<String, Object> getChatHistory(@RequestBody Map<String, String> requestData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String memoryId = requestData.get("memoryId");
            
            if (memoryId == null || memoryId.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "会话ID不能为空");
                return response;
            }
            
            // 获取会话历史记录
            List<ChatMessage> messages = redisChatMemoryStore.getMessages(memoryId);
            
            // 将ChatMessage转换为可序列化的Map格式
            List<Map<String, Object>> convertedMessages = convertChatMessagesToMaps(messages);

            // 剔除convertedMessages中元素的text字段为空的元素（工具调用时，text字段为空）
            convertedMessages.removeIf(map -> map.get("text") == null || map.get("text").toString().isEmpty());
            
            response.put("success", true);
            response.put("messages", convertedMessages);
            response.put("count", convertedMessages.size());
            
        } catch (Exception e) {
            System.err.println("获取会话历史记录错误: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "获取会话历史记录失败");
            response.put("messages", List.of());
            response.put("count", 0);
        }
        
        return response;
    }
    
    /**
     * 将ChatMessage列表转换为可序列化的Map列表
     * @param messages ChatMessage列表
     * @return 转换后的Map列表
     */
    private List<Map<String, Object>> convertChatMessagesToMaps(List<ChatMessage> messages) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        if (messages == null || messages.isEmpty()) {
            return result;
        }
        
        for (ChatMessage message : messages) {
            Map<String, Object> messageMap = new HashMap<>();
            
            try {
                // 获取消息类型
                String messageType = message.type().name();
                messageMap.put("type", messageType);
                
                // 获取消息内容
                String content = "";
                if (message instanceof dev.langchain4j.data.message.UserMessage) {
                    content = ((dev.langchain4j.data.message.UserMessage) message).singleText();
                } else if (message instanceof dev.langchain4j.data.message.AiMessage) {
                    content = ((dev.langchain4j.data.message.AiMessage) message).text();
                } else if (message instanceof dev.langchain4j.data.message.SystemMessage) {
                    content = ((dev.langchain4j.data.message.SystemMessage) message).text();
                } else {
                    content = message.toString();
                }
                
                messageMap.put("content", content);
                messageMap.put("text", content); // 兼容前端的两种字段名
                
                result.add(messageMap);
                
            } catch (Exception e) {
                System.err.println("转换消息失败: " + e.getMessage());
                // 如果转换失败，添加一个空的消息记录
                Map<String, Object> errorMessage = new HashMap<>();
                errorMessage.put("type", "UNKNOWN");
                errorMessage.put("content", "");
                errorMessage.put("text", "");
                result.add(errorMessage);
            }
        }
        
        return result;
    }
    
    /**
     * 从memoryId中提取userId
     * 支持多种格式：
     * - "chat_16_persistent" → "16"
     * - "chat_16_1728389520_abc123" → "16"
     * - "user_16" → "16"
     * - "default-session" → "default"
     */
    private String extractUserIdFromMemoryId(String memoryId) {
        if (memoryId == null || memoryId.isEmpty()) {
            return "default";
        }
        
        // 格式1: "chat_XXX_persistent" 或 "chat_XXX_timestamp_random"
        if (memoryId.startsWith("chat_")) {
            String[] parts = memoryId.split("_");
            if (parts.length >= 2) {
                return parts[1]; // 返回 userId 部分
            }
        }
        
        // 格式2: "user_XXX"
        if (memoryId.startsWith("user_")) {
            return memoryId.substring(5); // 去掉 "user_" 前缀
        }
        
        // 其他格式返回default
        return "default";
    }
}