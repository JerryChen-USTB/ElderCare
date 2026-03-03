package edu.ustb.eldercarebackend.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 聊天配置类
 * 配置ChatMemory和EmbeddingStore
 * 注意：ChatService现在由MultiTenantChatService管理，不再是单例Bean
 */
@Configuration
@EnableAsync  // 启用异步支持
public class ChatConfig {
    
    @Autowired
    private ChatMemoryStore redisChatMemoryStore;

    /**
     * ChatMemory提供者
     * 为每个用户会话提供独立的对话记忆
     */
    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return new ChatMemoryProvider() {
            @Override
            public ChatMemory get(Object memoryId) {
                return MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(20)
                        .chatMemoryStore(redisChatMemoryStore)
                        .build();
            }
        };
    }
    
    /**
     * 空的EmbeddingStore Bean
     * 仅用于满足某些依赖注入需求
     * 实际的向量存储由GlobalKnowledgeManager和UserKnowledgeManager管理
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }
}
