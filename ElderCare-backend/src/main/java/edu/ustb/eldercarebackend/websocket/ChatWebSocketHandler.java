package edu.ustb.eldercarebackend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ustb.eldercarebackend.service.elderly.MultiTenantChatService;
import edu.ustb.eldercarebackend.util.ChatContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler implements WebSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    
    @Autowired
    private MultiTenantChatService multiTenantChatService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 存储活跃的WebSocket会话
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        logger.info("🔌 WebSocket连接建立: {}", sessionId);
        
        // 发送连接成功消息
        sendMessage(session, Map.of(
            "type", "connected", 
            "message", "WebSocket连接成功",
            "sessionId", sessionId
        ));
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String sessionId = session.getId();
        logger.debug("📨 收到WebSocket消息[{}]: {}", sessionId, message.getPayload());
        
        try {
            // 解析客户端消息
            @SuppressWarnings("unchecked")
            Map<String, String> request = objectMapper.readValue(
                message.getPayload().toString(), 
                Map.class
            );
            
            String action = request.get("action");
            String userMessage = request.get("message");
            String memoryId = request.get("memoryId");
            
            if ("chat".equals(action) && userMessage != null) {
                handleChatMessage(session, userMessage, memoryId);
            } else {
                sendMessage(session, Map.of(
                    "type", "error", 
                    "message", "无效的消息格式"
                ));
            }
            
        } catch (Exception e) {
            logger.error("❌ 处理WebSocket消息失败", e);
            sendMessage(session, Map.of(
                "type", "error", 
                "message", "消息处理失败: " + e.getMessage()
            ));
        }
    }
    
    private void handleChatMessage(WebSocketSession session, String message, String memoryId) {
        if (message == null || message.trim().isEmpty()) {
            sendMessage(session, Map.of(
                "type", "error", 
                "message", "消息内容不能为空"
            ));
            return;
        }
        
        // 如果没有提供memoryId，使用默认值
        if (memoryId == null || memoryId.trim().isEmpty()) {
            memoryId = "default-session";
        }
        
        // 从memoryId中提取userId (格式: "user_16")
        String userId = extractUserIdFromMemoryId(memoryId);
        
        logger.info("🚀 开始WebSocket流式聊天 - userId: {}, memoryId: {}", userId, memoryId);
        
        // 发送开始消息
        sendMessage(session, Map.of(
            "type", "start", 
            "message", "开始处理您的消息..."
        ));
        
        // 异步处理聊天请求
        final String finalMemoryId = memoryId;
        final String finalUserId = userId;
        new Thread(() -> {
            // 设置聊天上下文，用于Tool调用时获取用户信息
            ChatContext.setContext(finalMemoryId);
            
            // 调试日志：验证用户ID是否正确解析
            String threadName = Thread.currentThread().getName();
            Integer currentUserId = ChatContext.getCurrentUserId();
            logger.debug("🔍 WebSocket聊天上下文设置完成: 线程名={}", threadName);
            logger.debug("🔍 WebSocket上下文信息: memoryId={}, userId={}", finalMemoryId, currentUserId);
            
            try {
                // 获取当前时间
                LocalDateTime now = LocalDateTime.now();
                String enhancedMessage = message + "\nAdditional Information:\n当前时间：" + 
                                       now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                
                // 调用MultiTenantChatService获取流式响应（支持混合知识库检索）
                multiTenantChatService.chatStreaming(finalUserId, finalMemoryId, enhancedMessage)
                        .doOnNext(chunk -> {
                        // 发送每个数据块
                        logger.debug("📤 发送WebSocket数据块: {}", chunk);
                        sendMessage(session, Map.of(
                            "type", "chunk",
                            "data", chunk
                        ));
                    })
                    .doOnComplete(() -> {
                        // 发送完成消息
                        logger.info("✅ WebSocket聊天完成");
                        sendMessage(session, Map.of(
                            "type", "complete",
                            "message", "聊天完成"
                        ));
                    })
                    .doOnError(error -> {
                        // 发送错误消息
                        logger.error("❌ WebSocket聊天错误", error);
                        sendMessage(session, Map.of(
                            "type", "error",
                            "message", "聊天服务错误: " + error.getMessage()
                        ));
                    })
                    .blockLast(); // 等待流完成
                    
            } catch (Exception e) {
                logger.error("❌ WebSocket聊天处理异常", e);
                sendMessage(session, Map.of(
                    "type", "error",
                    "message", "处理异常: " + e.getMessage()
                ));
            } finally {
                // 在清除前检查上下文状态
                Integer finalUserIdInContext = ChatContext.getCurrentUserId();
                String finalMemoryIdCheck = ChatContext.getCurrentMemoryId();
                logger.debug("🔍 WebSocket处理结束前: userId={}, memoryId={}", finalUserIdInContext, finalMemoryIdCheck);
                
                // 确保在线程结束时清除上下文
                ChatContext.clearContext();
                logger.debug("🔍 WebSocket上下文已清除");
            }
        }).start();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("❌ WebSocket传输错误", exception);
        sessions.remove(session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        logger.info("🔌 WebSocket连接关闭: {}, 状态: {}", sessionId, closeStatus);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
    
    // 发送消息到客户端
    private void sendMessage(WebSocketSession session, Map<String, String> message) {
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            logger.error("❌ 发送WebSocket消息失败", e);
        }
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
