package com.wangheng.article.util;

/**
 * 文章互动数据在 Redis 中的键名约定（点赞/收藏 Set，全网统一，勿在调用处手写字符串）。
 */
public final class InteractionRedisKeys {

    /** 文章点赞 Set：成员为用户ID */
    public static final String LIKE_KEY_PREFIX = "article:like:";

    /** 文章收藏 Set：成员为用户ID */
    public static final String FAV_KEY_PREFIX = "article:fav:";

    private InteractionRedisKeys() {
    }

    public static String likeKey(Integer articleId) {
        return LIKE_KEY_PREFIX + articleId;
    }

    public static String favKey(Integer articleId) {
        return FAV_KEY_PREFIX + articleId;
    }
}
