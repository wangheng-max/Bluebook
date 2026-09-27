package com.wangheng.article.pojo;

import com.wangheng.product.pojo.ProductBriefVO;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;










/**
 * 公开笔记搜索结果展示对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteSearchVO {
    private Integer noteId;       // 笔记ID
    private String title;         // 标题
    private String content;       // 完整正文（仅详情接口填充，列表为 null）
    private String summary;       // 正文摘要（前150字）
    private String coverImage;    // 封面图URL
    private Integer authorId;     // 作者用户ID
    private String authorName;    // 作者用户名
    private String authorAvatar;  // 作者头像URL
    private Integer likeCount;    // 点赞数
    private Boolean liked;        // 当前用户是否已点赞（匿名恒 false，信息流卡片可直接点赞）
    private Integer commentCount; // 评论数
    private Integer viewCount;    // 浏览量
    private Integer favoriteCount;// 收藏数
    private Integer forwardCount; // 转发数
    private LocalDateTime createTime; // 创建时间

    // ---- 标签与带货（内容电商打通） ----
    private java.util.List<String> tags;             // 文章标签
    private java.util.List<ProductBriefVO> products; // 带货商品（点击跳商品页）
    private Integer shopUserId;                      // 作者认证店铺（认证商家才有，跳店铺页）
    private String shopName;                         // 店铺名称
}
