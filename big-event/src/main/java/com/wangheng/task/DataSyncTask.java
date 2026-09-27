package com.wangheng.task;

import com.wangheng.article.mapper.ArticleMapper;
import com.wangheng.article.mapper.InteractionMapper;
import com.wangheng.article.pojo.Article;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;













/**
 * 缓存一致性定时任务（文档第7.2/7.3节）：
 * 1. 浏览计数：Redis INCR 增量批量落库；
 * 2. 点赞/收藏：Redis Set 与明细表全量对账，计数回写 article；
 * 3. 热度重算：score = view×1 + like×2 + favorite×3 + forward×2，按 log2(age+2) 时间衰减重建 ZSET。
 */
@Component
public class DataSyncTask {

    private static final String HOT_KEY = "hot:articles";
    private static final String VIEW_KEY_PREFIX = "article:view:";
    private static final String LIKE_KEY_PREFIX = com.wangheng.article.util.InteractionRedisKeys.LIKE_KEY_PREFIX;
    private static final String FAV_KEY_PREFIX = com.wangheng.article.util.InteractionRedisKeys.FAV_KEY_PREFIX;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private InteractionMapper interactionMapper;

    @Autowired
    private ArticleMapper articleMapper;

    /**
     * 浏览计数落库：每5分钟
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void syncViewCounts() {
        Set<String> keys = stringRedisTemplate.keys(VIEW_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }
        for (String key : keys) {
            try {
                String id = key.substring(VIEW_KEY_PREFIX.length());
                String v = stringRedisTemplate.opsForValue().get(key);
                if (v == null) {
                    continue;
                }
                int delta = Integer.parseInt(v);
                if (delta <= 0) {
                    continue;
                }
                interactionMapper.incrViewCount(Integer.parseInt(id), delta);
                stringRedisTemplate.delete(key);
            } catch (Exception e) {
                // 单条失败跳过，下次继续
            }
        }
    }

    /**
     * 点赞/收藏 Set 对账落库：每5分钟
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void syncInteractions() {
        // 点赞对账
        Set<String> likeKeys = stringRedisTemplate.keys(LIKE_KEY_PREFIX + "*");
        if (likeKeys != null) {
            for (String key : likeKeys) {
                try {
                    int articleId = Integer.parseInt(key.substring(LIKE_KEY_PREFIX.length()));
                    reconcile(articleId, LIKE_KEY_PREFIX + articleId,
                            (aid, userIds) -> {
                                if (userIds.isEmpty()) {
                                    interactionMapper.deleteLikeAll(aid);
                                } else {
                                    for (Integer uid : userIds) {
                                        interactionMapper.insertLike(aid, uid);
                                    }
                                    interactionMapper.deleteLikeNotIn(aid, userIds);
                                }
                            });
                } catch (Exception e) {
                    // 单条失败跳过
                }
            }
        }

        // 收藏对账
        Set<String> favKeys = stringRedisTemplate.keys(FAV_KEY_PREFIX + "*");
        if (favKeys != null) {
            for (String key : favKeys) {
                try {
                    int articleId = Integer.parseInt(key.substring(FAV_KEY_PREFIX.length()));
                    reconcile(articleId, FAV_KEY_PREFIX + articleId,
                            (aid, userIds) -> {
                                if (userIds.isEmpty()) {
                                    interactionMapper.deleteFavAll(aid);
                                } else {
                                    for (Integer uid : userIds) {
                                        interactionMapper.insertFavorite(aid, uid, null); // 对账回写归默认收藏夹
                                    }
                                    interactionMapper.deleteFavNotIn(aid, userIds);
                                }
                            });
                } catch (Exception e) {
                    // 单条失败跳过
                }
            }
        }

        // 计数回写：以 Redis SCARD 为准更新 article.like_count / favorite_count
        if (likeKeys != null) {
            for (String key : likeKeys) {
                try {
                    int articleId = Integer.parseInt(key.substring(LIKE_KEY_PREFIX.length()));
                    Long likeSize = stringRedisTemplate.opsForSet().size(LIKE_KEY_PREFIX + articleId);
                    Long favSize = stringRedisTemplate.opsForSet().size(FAV_KEY_PREFIX + articleId);
                    interactionMapper.syncCounts(articleId,
                            likeSize == null ? 0 : likeSize.intValue(),
                            favSize == null ? 0 : favSize.intValue());
                } catch (Exception e) {
                    // 单条失败跳过
                }
            }
        }
    }

    /**
     * 热度重算：每10分钟，全量重建 hot:articles ZSET（数据量小，直接重建）
     * score = view×1 + like×2 + favorite×3 + forward×2，再除以 log2(age+2) 时间衰减
     */
    @Scheduled(fixedDelay = 10 * 60 * 1000)
    public void rebuildHotScore() {
        List<Article> articles = articleMapper.findAllPublished();
        if (articles == null || articles.isEmpty()) {
            return;
        }
        stringRedisTemplate.delete(HOT_KEY);
        for (Article a : articles) {
            double score = (a.getViewCount() == null ? 0 : a.getViewCount()) * 1
                    + (a.getLikeCount() == null ? 0 : a.getLikeCount()) * 2
                    + (a.getFavoriteCount() == null ? 0 : a.getFavoriteCount()) * 3
                    + (a.getForwardCount() == null ? 0 : a.getForwardCount()) * 2;

            // 时间衰减：log2(ageDays+2)，新文章不衰减，越老权重越低
            long ageDays = a.getCreateTime() == null
                    ? 0 : Duration.between(a.getCreateTime(), LocalDateTime.now()).toDays();
            double decay = Math.log(ageDays + 2) / Math.log(2);
            if (decay < 1) {
                decay = 1;
            }
            stringRedisTemplate.opsForZSet().add(HOT_KEY, String.valueOf(a.getId()), score / decay);
        }
    }

    private interface ReconcileAction {
        void apply(int articleId, List<Integer> userIds);
    }

    private void reconcile(int articleId, String key, ReconcileAction action) {
        Set<String> members = stringRedisTemplate.opsForSet().members(key);
        if (members == null) {
            return;
        }
        List<Integer> userIds = members.stream().map(Integer::valueOf).toList();
        action.apply(articleId, userIds);
    }
}
