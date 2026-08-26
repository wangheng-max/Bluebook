package com.wangheng.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangheng.pojo.Message;
import com.wangheng.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 好友实时聊天 WebSocket 处理器。
 * 会话以 userId 为 key 管理（同一用户单连接覆盖旧连接）；
 * 发送消息：先落库保证不丢 → 判断在线 → 在线实时推送，离线走未读拉取。
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    /** 在线会话表：userId -> WebSocketSession */
    private static final ConcurrentHashMap<Integer, WebSocketSession> SESSIONS = new ConcurrentHashMap<>();

    @Autowired
    private MessageService messageService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 推送消息给指定用户（在线则实时推送，离线忽略，未读消息由 /message/offline 拉取）
     */
    public boolean sendToUser(Integer toUserId, String payload) {
        WebSocketSession session = SESSIONS.get(toUserId);
        if (session != null && session.isOpen()) {
            try {
                synchronized (session) {
                    session.sendMessage(new TextMessage(payload));
                }
                return true;
            } catch (IOException e) {
                SESSIONS.remove(toUserId);
            }
        }
        return false;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 握手阶段已把 userId 放入 session attributes
        Integer userId = (Integer) session.getAttributes().get("userId");
        if (userId == null) {
            try {
                session.close(CloseStatus.POLICY_VIOLATION);
            } catch (IOException ignored) {
            }
            return;
        }
        SESSIONS.put(userId, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        Integer fromUserId = (Integer) session.getAttributes().get("userId");
        if (fromUserId == null) {
            return;
        }

        // 协议：{"toUserId": 2, "content": "你好"} 或转发消息 {"toUserId":2,"msgType":1,"articleId":3,"content":"看这篇"}
        JsonNode node = objectMapper.readTree(textMessage.getPayload());
        int toUserId = node.path("toUserId").asInt();
        int msgType = node.path("msgType").asInt(0);
        String content = node.path("content").asText(null);
        Integer articleId = node.has("articleId") ? node.get("articleId").asInt() : null;

        // 落库（保证不丢）
        Message saved = messageService.save(fromUserId, toUserId, msgType, content, articleId);
        if (saved == null) {
            return;
        }
        // 带上发送者信息后推送
        String payload = objectMapper.writeValueAsString(saved);
        sendToUser(toUserId, payload);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Integer userId = (Integer) session.getAttributes().get("userId");
        if (userId != null) {
            SESSIONS.remove(userId, session);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Integer userId = (Integer) session.getAttributes().get("userId");
        if (userId != null) {
            SESSIONS.remove(userId, session);
        }
        try {
            session.close(CloseStatus.SERVER_ERROR);
        } catch (IOException ignored) {
        }
    }

    /** 当前在线用户数（调试/管理用） */
    public int onlineCount() {
        return SESSIONS.size();
    }
}
