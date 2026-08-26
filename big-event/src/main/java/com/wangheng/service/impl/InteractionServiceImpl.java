package com.wangheng.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangheng.mapper.ArticleMapper;
import com.wangheng.mapper.FriendMapper;
import com.wangheng.mapper.InteractionMapper;
import com.wangheng.pojo.Article;
import com.wangheng.pojo.InteractStatusVO;
import com.wangheng.pojo.Message;
import com.wangheng.service.InteractionService;
import com.wangheng.service.MessageService;
import com.wangheng.websocket.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class InteractionServiceImpl implements InteractionService {

    /** 热点 ZSET：成员=文章ID，score=热度分 */
    private static final String HOT_KEY = "hot:articles";

    /** 热度加权：view×1 + like×2 + favorite×3 + forward×2 */
    private static final double HOT_LIKE = 2;
    private static final double HOT_FAVORITE = 3;
    private static final double HOT_FORWARD = 2;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private FriendMapper friendMapper;

    @Autowired
    private InteractionMapper interactionMapper;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Long like(Integer articleId, Integer userId) {
        checkArticle(articleId);
        Long added = stringRedisTemplate.opsForSet().add(likeKey(articleId), String.valueOf(userId));
        // 首次点赞才加分（幂等）
        if (added != null && added > 0) {
            stringRedisTemplate.opsForZSet().incrementScore(HOT_KEY, String.valueOf(articleId), HOT_LIKE);
        }
        return stringRedisTemplate.opsForSet().size(likeKey(articleId));
    }

    @Override
    public Long unlike(Integer articleId, Integer userId) {
        Long removed = stringRedisTemplate.opsForSet().remove(likeKey(articleId), String.valueOf(userId));
        if (removed != null && removed > 0) {
            stringRedisTemplate.opsForZSet().incrementScore(HOT_KEY, String.valueOf(articleId), -HOT_LIKE);
        }
        return stringRedisTemplate.opsForSet().size(likeKey(articleId));
    }

    @Override
    public Long favorite(Integer articleId, Integer userId) {
        checkArticle(articleId);
        Long added = stringRedisTemplate.opsForSet().add(favKey(articleId), String.valueOf(userId));
        if (added != null && added > 0) {
            stringRedisTemplate.opsForZSet().incrementScore(HOT_KEY, String.valueOf(articleId), HOT_FAVORITE);
        }
        return stringRedisTemplate.opsForSet().size(favKey(articleId));
    }

    @Override
    public Long unfavorite(Integer articleId, Integer userId) {
        Long removed = stringRedisTemplate.opsForSet().remove(favKey(articleId), String.valueOf(userId));
        if (removed != null && removed > 0) {
            stringRedisTemplate.opsForZSet().incrementScore(HOT_KEY, String.valueOf(articleId), -HOT_FAVORITE);
        }
        return stringRedisTemplate.opsForSet().size(favKey(articleId));
    }

    @Override
    public InteractStatusVO status(Integer articleId, Integer userId) {
        Boolean liked = stringRedisTemplate.opsForSet().isMember(likeKey(articleId), String.valueOf(userId));
        Boolean favorited = stringRedisTemplate.opsForSet().isMember(favKey(articleId), String.valueOf(userId));
        Long likeCount = stringRedisTemplate.opsForSet().size(likeKey(articleId));
        Long favoriteCount = stringRedisTemplate.opsForSet().size(favKey(articleId));
        return new InteractStatusVO(
                Boolean.TRUE.equals(liked),
                Boolean.TRUE.equals(favorited),
                likeCount == null ? 0 : likeCount,
                favoriteCount == null ? 0 : favoriteCount);
    }

    @Override
    public void forward(Integer articleId, Integer fromUserId, Integer toUserId) {
        Article article = articleMapper.findById(articleId);
        if (article == null || !"已发布".equals(article.getState())) {
            throw new RuntimeException("文章不存在或未发布");
        }
        if (fromUserId.equals(toUserId)) {
            throw new RuntimeException("不能转发给自己");
        }
        // 转发目标必须是好友
        if (friendMapper.isFriend(fromUserId, toUserId) <= 0) {
            throw new RuntimeException("只能转发给好友");
        }

        // 记录转发明细（唯一索引幂等）+ 计数 + 热点加分
        interactionMapper.insertForward(articleId, fromUserId, toUserId);
        interactionMapper.incrForwardCount(articleId);
        stringRedisTemplate.opsForZSet().incrementScore(HOT_KEY, String.valueOf(articleId), HOT_FORWARD);

        // 生成 msg_type=1 的消息卡片（content 存标题，卡片图走联表）
        Message message = messageService.save(fromUserId, toUserId, 1, article.getTitle(), articleId);

        // 好友在线 → 实时推送
        try {
            chatWebSocketHandler.sendToUser(toUserId, objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            // 推送失败不阻断，离线用户走未读拉取
        }
    }

    private void checkArticle(Integer articleId) {
        Article article = articleMapper.findById(articleId);
        if (article == null || !"已发布".equals(article.getState())) {
            throw new RuntimeException("文章不存在或未发布");
        }
    }

    private String likeKey(Integer articleId) {
        return "article:like:" + articleId;
    }

    private String favKey(Integer articleId) {
        return "article:fav:" + articleId;
    }
}
