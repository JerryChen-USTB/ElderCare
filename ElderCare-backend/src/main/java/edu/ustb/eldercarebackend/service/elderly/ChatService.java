package edu.ustb.eldercarebackend.service.elderly;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

public interface ChatService {
    
    @SystemMessage(fromResource = "SystemMessage.txt")          // 静态系统消息
    public Flux<String> chat(
            @MemoryId String memoryId,
            @UserMessage String message
        //     @V("now") String now
    );
}