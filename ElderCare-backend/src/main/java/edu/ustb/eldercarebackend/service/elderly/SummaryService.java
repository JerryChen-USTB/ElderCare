package edu.ustb.eldercarebackend.service.elderly;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * AI总结服务接口
 * 用于总结远程协助的对话内容
 */
public interface SummaryService {
    
    /**
     * 总结对话内容
     * @param transcriptContent 转录的对话内容
     * @return AI总结结果
     */
    @SystemMessage(fromResource = "SummarySystemMessage.txt")
    String summarizeConversation(@UserMessage String transcriptContent);
}
