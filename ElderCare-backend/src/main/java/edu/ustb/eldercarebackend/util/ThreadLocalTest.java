package edu.ustb.eldercarebackend.util;

import org.springframework.stereotype.Component;

/**
 * 线程本地变量测试工具
 * 用于验证InheritableThreadLocal在不同线程中的表现
 */
@Component
public class ThreadLocalTest {
    
    /**
     * 测试InheritableThreadLocal在父子线程中的传递情况
     */
    public static void testInheritableThreadLocal() {
        // 在主线程中设置上下文
        String testMemoryId = "chat_999_test";
        ChatContext.setContext(testMemoryId);
        
        System.out.println("==== InheritableThreadLocal 测试开始 ====");
        System.out.println("主线程: " + Thread.currentThread().getName());
        System.out.println("主线程设置: memoryId=" + testMemoryId);
        System.out.println("主线程获取: userId=" + ChatContext.getCurrentUserId() + ", memoryId=" + ChatContext.getCurrentMemoryId());
        
        // 测试子线程继承
        Thread childThread = new Thread(() -> {
            System.out.println("子线程: " + Thread.currentThread().getName());
            System.out.println("子线程获取: userId=" + ChatContext.getCurrentUserId() + ", memoryId=" + ChatContext.getCurrentMemoryId());
            
            // 在子线程中修改值
            ChatContext.setContext("chat_888_child");
            System.out.println("子线程修改后: userId=" + ChatContext.getCurrentUserId() + ", memoryId=" + ChatContext.getCurrentMemoryId());
        });
        
        childThread.start();
        try {
            childThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // 检查主线程的值是否受影响
        System.out.println("主线程最终: userId=" + ChatContext.getCurrentUserId() + ", memoryId=" + ChatContext.getCurrentMemoryId());
        
        // 测试线程池情况
        System.out.println("\n==== 测试线程池情况 ====");
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        
        for (int i = 0; i < 3; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("线程池任务" + taskId + ": " + Thread.currentThread().getName());
                System.out.println("线程池任务" + taskId + "获取: userId=" + ChatContext.getCurrentUserId() + ", memoryId=" + ChatContext.getCurrentMemoryId());
            });
        }
        
        executor.shutdown();
        try {
            executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        ChatContext.clearContext();
        System.out.println("==== InheritableThreadLocal 测试结束 ====\n");
    }
    
    /**
     * 测试当前线程的上下文状态
     */
    public static void printCurrentContext(String label) {
        System.out.println("=== " + label + " ===");
        System.out.println("线程: " + Thread.currentThread().getName());
        System.out.println("userId: " + ChatContext.getCurrentUserId());
        System.out.println("memoryId: " + ChatContext.getCurrentMemoryId());
        System.out.println("hasValidContext: " + ChatContext.hasValidUserContext());
        System.out.println("==================\n");
    }
}
