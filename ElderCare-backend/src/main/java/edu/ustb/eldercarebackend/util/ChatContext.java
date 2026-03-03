package edu.ustb.eldercarebackend.util;

import org.springframework.stereotype.Component;

/**
 * 聊天上下文管理器
 * 用于在Tool调用过程中传递用户会话信息
 */
@Component
public class ChatContext {
    
    private static final InheritableThreadLocal<String> memoryIdThreadLocal = new InheritableThreadLocal<>();
    private static final InheritableThreadLocal<Integer> userIdThreadLocal = new InheritableThreadLocal<>();
    
    /**
     * 设置当前线程的会话上下文
     * @param memoryId 会话ID
     */
    public static void setContext(String memoryId) {
        memoryIdThreadLocal.set(memoryId);
        // 从memoryId中提取用户ID
        Integer userId = extractUserIdFromMemoryId(memoryId);
        userIdThreadLocal.set(userId);
    }
    
    /**
     * 获取当前线程的会话ID
     * @return 会话ID
     */
    public static String getCurrentMemoryId() {
        return memoryIdThreadLocal.get();
    }
    
    /**
     * 获取当前线程的用户ID
     * @return 用户ID
     */
    public static Integer getCurrentUserId() {
        return userIdThreadLocal.get();
    }
    
    /**
     * 清除当前线程的上下文信息
     */
    public static void clearContext() {
        memoryIdThreadLocal.remove();
        userIdThreadLocal.remove();
    }
    
    /**
     * 从memoryId中提取用户ID
     * memoryId格式：
     * - chat_{userId}_persistent (持久会话)
     * - chat_{userId}_{timestamp}_{random} (临时会话)
     * 
     * @param memoryId 会话ID
     * @return 用户ID，如果解析失败返回null
     */
    private static Integer extractUserIdFromMemoryId(String memoryId) {
        if (memoryId == null || memoryId.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 移除前缀 "chat_"
            if (memoryId.startsWith("chat_")) {
                String withoutPrefix = memoryId.substring(5); // 移除 "chat_"
                
                // 查找第一个下划线的位置
                int firstUnderscoreIndex = withoutPrefix.indexOf('_');
                if (firstUnderscoreIndex > 0) {
                    // 提取用户ID部分
                    String userIdStr = withoutPrefix.substring(0, firstUnderscoreIndex);
                    return Integer.parseInt(userIdStr);
                } else {
                    // 如果没有下划线，整个部分就是用户ID（这种情况不太可能，但作为容错）
                    return Integer.parseInt(withoutPrefix);
                }
            }
            
            return null;
        } catch (NumberFormatException e) {
            System.err.println("❌ 从memoryId中解析用户ID失败: " + memoryId + ", 错误: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 检查当前是否有有效的用户上下文
     * @return 是否有有效的用户上下文
     */
    public static boolean hasValidUserContext() {
        return getCurrentUserId() != null && getCurrentUserId() > 0;
    }
}
