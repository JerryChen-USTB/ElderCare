package edu.ustb.eldercarebackend.config;

import edu.ustb.eldercarebackend.websocket.ChatWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/*  
    配置WebSocket，支持跨域
 */

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Bean
    public ChatWebSocketHandler chatWebSocketHandler() {
        return new ChatWebSocketHandler();
    }
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册WebSocket处理器，支持跨域
        registry.addHandler(chatWebSocketHandler(), "/ws/chat")
                .setAllowedOrigins("*"); // 允许跨域，生产环境应该设置具体域名
    }
}
