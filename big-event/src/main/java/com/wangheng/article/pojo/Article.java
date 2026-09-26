package com.wangheng.article.pojo;


import com.wangheng.anno.State;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;
@Data
public class Article {
    private Integer id;//主键ID
    @NotEmpty
    @Pattern(regexp = "^\\S{1,10}$")
    private String title;//文章标题
    @NotEmpty
    private String content;//文章内容

    @URL
    private String coverImg;//封面图像

    @State
    private String state;//发布状态 已发布|草稿
    @NotNull
    private Integer categoryId;//文章分类id
    private String tags;//标签，逗号分隔
    private String productIds;//带货商品ID，逗号分隔（发布时选商品，详情/卡片可跳转商品页）
    private Integer createUser;//创建人ID
    private LocalDateTime createTime;//创建时间
    private LocalDateTime updateTime;//更新时间

    //社区扩展：计数字段（缓存定时落库）
    private Integer viewCount;//浏览量
    private Integer likeCount;//点赞数
    private Integer favoriteCount;//收藏数
    private Integer forwardCount;//转发数
}
