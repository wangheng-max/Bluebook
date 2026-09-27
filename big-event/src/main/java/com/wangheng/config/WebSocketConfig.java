package com.wangheng.config;

import com.wangheng.utils.JwtUtil;
import com.wangheng.websocket.ChatWebSocketHandler;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;



/**
 * WebSocket 配置：注册 /ws/chat，握手时从 query 参数 token 解析 userId（复用 JWT 登录态）。
 * 前端连接：ws://host/ws/chat?token={jwt}
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOrigins("*")
                .addInterceptors(new JwtHandshakeInterceptor());
    }

    /** 握手拦截器：校验 JWT 并把 userId 放入 session attributes */
    static class JwtHandshakeInterceptor implements HandshakeInterceptor {

        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Map<String, Object> attributes) {
            String query = request.getURI().getQuery();
            if (query == null) {
                return false;
            }
            String token = null;
            for (String pair : query.split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2 && "token".equals(kv[0])) {
                    token = kv[1];
                    break;
                }
            }
            if (token == null || token.isEmpty()) {
                return false;
            }
            try {
                Map<String, Object> claims = JwtUtil.parseToken(token);
                Object id = claims.get("id");
                if (id == null) {
                    return false;
                }
                attributes.put("userId", Integer.valueOf(String.valueOf(id)));
                return true;
            } catch (Exception e) {
                // JWT 校验失败拒绝握手
                return false;
            }
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Exception exception) {
        }
    }
}
