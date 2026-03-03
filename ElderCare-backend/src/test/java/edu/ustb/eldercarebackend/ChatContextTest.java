package edu.ustb.eldercarebackend;

import edu.ustb.eldercarebackend.util.ChatContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 聊天上下文管理器测试
 */
public class ChatContextTest {

    @AfterEach
    public void cleanup() {
        // 每个测试后清理上下文
        ChatContext.clearContext();
    }

    @Test
    public void testExtractUserIdFromPersistentMemoryId() {
        // 测试持久会话ID格式: chat_{userId}_persistent
        String memoryId = "chat_123_persistent";
        ChatContext.setContext(memoryId);
        
        Integer userId = ChatContext.getCurrentUserId();
        String retrievedMemoryId = ChatContext.getCurrentMemoryId();
        
        assertEquals(123, userId);
        assertEquals(memoryId, retrievedMemoryId);
    }

    @Test
    public void testExtractUserIdFromTemporaryMemoryId() {
        // 测试临时会话ID格式: chat_{userId}_{timestamp}_{random}
        String memoryId = "chat_456_1640995200000_abc123def";
        ChatContext.setContext(memoryId);
        
        Integer userId = ChatContext.getCurrentUserId();
        String retrievedMemoryId = ChatContext.getCurrentMemoryId();
        
        assertEquals(456, userId);
        assertEquals(memoryId, retrievedMemoryId);
    }

    @Test
    public void testInvalidMemoryIdFormat() {
        // 测试无效的memoryId格式
        String memoryId = "invalid_format";
        ChatContext.setContext(memoryId);
        
        Integer userId = ChatContext.getCurrentUserId();
        
        assertNull(userId);
    }

    @Test
    public void testEmptyMemoryId() {
        // 测试空的memoryId
        String memoryId = "";
        ChatContext.setContext(memoryId);
        
        Integer userId = ChatContext.getCurrentUserId();
        
        assertNull(userId);
    }

    @Test
    public void testNullMemoryId() {
        // 测试null的memoryId
        String memoryId = null;
        ChatContext.setContext(memoryId);
        
        Integer userId = ChatContext.getCurrentUserId();
        
        assertNull(userId);
    }

    @Test
    public void testHasValidUserContext() {
        // 测试有效用户上下文检查
        ChatContext.setContext("chat_789_persistent");
        
        assertTrue(ChatContext.hasValidUserContext());
        
        // 清理后应该无效
        ChatContext.clearContext();
        
        assertFalse(ChatContext.hasValidUserContext());
    }

    @Test
    public void testThreadLocalIsolation() {
        // 测试不同线程的上下文隔离
        ChatContext.setContext("chat_111_persistent");
        assertEquals(111, ChatContext.getCurrentUserId());
        
        // 创建新线程测试隔离性
        Thread newThread = new Thread(() -> {
            // 新线程中应该没有上下文
            assertNull(ChatContext.getCurrentUserId());
            assertFalse(ChatContext.hasValidUserContext());
            
            // 在新线程中设置不同的上下文
            ChatContext.setContext("chat_222_persistent");
            assertEquals(222, ChatContext.getCurrentUserId());
        });
        
        try {
            newThread.start();
            newThread.join();
            
            // 主线程的上下文应该保持不变
            assertEquals(111, ChatContext.getCurrentUserId());
        } catch (InterruptedException e) {
            fail("Thread was interrupted");
        }
    }
}
