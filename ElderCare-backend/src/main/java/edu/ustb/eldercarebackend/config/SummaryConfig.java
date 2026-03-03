package edu.ustb.eldercarebackend.config;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import edu.ustb.eldercarebackend.service.elderly.SummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI总结服务配置类
 */
@Configuration
public class SummaryConfig {
    
    @Autowired
    private OpenAiChatModel openAiChatModel;
    
    /**
     * 创建SummaryService实例
     * 使用简单的AI服务，不需要RAG和工具调用
     */
    @Bean
    public SummaryService summaryService() {
        return AiServices.builder(SummaryService.class)
                .chatModel(openAiChatModel)  // 指定聊天模型
                .build();
    }
}
