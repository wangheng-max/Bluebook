package com.wangheng.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 互动状态查询结果：我是否已赞/已藏 + 最新计数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InteractStatusVO {
    private Boolean liked;      // 我是否已点赞
    private Boolean favorited;  // 我是否已收藏
    private Long likeCount;     // 点赞数（Redis SCARD）
    private Long favoriteCount; // 收藏数
}
