package com.wangheng.article.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收藏夹展示 VO（默认收藏夹 id 为 null，前端以 null 语义处理）。
 */
@Data
public class FavoriteFolderVO {
    private Integer id;//null=默认收藏夹
    private String name;
    private Integer articleCount;//夹内文章数
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
