package com.wangheng.article.pojo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收藏夹实体（用户自定义；「默认收藏夹」为虚拟夹，article_favorite.folder_id 为 NULL）。
 */
@Data
public class FavoriteFolder {
    private Integer id;
    private Integer userId;
    private String name;
    private LocalDateTime createTime;
}
