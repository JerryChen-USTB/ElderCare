package edu.ustb.eldercarebackend.service.elderly;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import edu.ustb.eldercarebackend.service.knowledge.GlobalKnowledgeManager;
import edu.ustb.eldercarebackend.service.knowledge.HybridContentRetriever;
import edu.ustb.eldercarebackend.service.knowledge.UserKnowledgeManager;
import edu.ustb.eldercarebackend.util.AppointmentTool;
import edu.ustb.eldercarebackend.util.HealthEducationTool;
import edu.ustb.eldercarebackend.util.ScheduleTool;
import edu.ustb.eldercarebackend.util.WeatherTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多租户聊天服务
 * 为每个用户提供独立的ChatService实例，支持混合知识库检索
 */
@Service
public class MultiTenantChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(MultiTenantChatService.class);
    
    @Autowired
    private OpenAiChatModel openAiChatModel;
    
    @Autowired
    private OpenAiStreamingChatModel openAiStreamingChatModel;
    
    @Autowired
    private ChatMemoryProvider chatMemoryProvider;
    
    @Autowired
    private GlobalKnowledgeManager globalKnowledgeManager;
    
    @Autowired
    private UserKnowledgeManager userKnowledgeManager;
    
    @Autowired
    private ScheduleTool scheduleTool;
    
    @Autowired
    private WeatherTool weatherTool;
    
    @Autowired
    private HealthEducationTool healthEducationTool;
    
    @Autowired
    private AppointmentTool appointmentTool;
    
    // 用户ChatService缓存：userId -> ChatService
    private final Map<String, ChatService> chatServiceCache = new ConcurrentHashMap<>();
    
    /**
     * 获取用户专属的ChatService
     */
    private ChatService getUserChatService(String userId) {
        return chatServiceCache.computeIfAbsent(userId, uid -> {
            logger.info("🆕 为用户创建专属ChatService: user_{}", uid);
            
            // 获取全局检索器
            ContentRetriever globalRetriever = globalKnowledgeManager.getGlobalRetriever();
            
            // 获取用户私有检索器
            ContentRetriever userRetriever = userKnowledgeManager.getUserRetriever(uid);
            
            // 创建混合检索器
            HybridContentRetriever hybridRetriever = new HybridContentRetriever(
                globalRetriever, 
                userRetriever
            );
            
            // 构建用户专属的ChatService
            ChatService chatService = AiServices.builder(ChatService.class)
                .chatModel(openAiChatModel)
                .streamingChatModel(openAiStreamingChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .contentRetriever(hybridRetriever)  // 使用混合检索器
                .tools(scheduleTool, weatherTool, healthEducationTool, appointmentTool)
                .build();
            
            logger.info("✅ ChatService创建完成: user_{}", uid);
            
            return chatService;
        });
    }
    
    /**
     * 流式聊天（对外接口）
     * @param userId 用户ID
     * @param memoryId 会话ID
     * @param userMessage 用户消息
     * @return Flux<String>
     */
    public Flux<String> chatStreaming(String userId, String memoryId, String userMessage) {
        logger.info("💬 用户 {} 发起流式聊天, memoryId: {}", userId, memoryId);
        
        // 获取用户专属的ChatService
        ChatService userChatService = getUserChatService(userId);
        
        // 调用聊天（返回Flux）
        return userChatService.chat(memoryId, userMessage);
    }
    
    /**
     * 普通聊天（对外接口）
     * @param userId 用户ID
     * @param memoryId 会话ID
     * @param userMessage 用户消息
     * @return AI回复（完整文本）
     */
    public String chat(String userId, String memoryId, String userMessage) {
        logger.info("💬 用户 {} 发起聊天, memoryId: {}", userId, memoryId);
        
        // 获取用户专属的ChatService
        ChatService userChatService = getUserChatService(userId);
        
        // 调用聊天并收集为完整字符串
        return userChatService.chat(memoryId, userMessage)
                .collectList()
                .map(list -> String.join("", list))
                .block();
    }
    
    /**
     * 清除用户ChatService缓存
     * 当用户上传新文档后调用，强制重建ChatService
     */
    public void clearUserCache(String userId) {
        chatServiceCache.remove(userId);
        userKnowledgeManager.clearUserCache(userId);
        logger.info("🗑️ 清除用户ChatService缓存: user_{}", userId);
    }
    
    /**
     * 获取缓存统计
     */
    public String getCacheStats() {
        return String.format("ChatService缓存: %d 个用户; %s", 
                           chatServiceCache.size(),
                           userKnowledgeManager.getCacheStats());
    }
}

