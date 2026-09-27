package com.wangheng.comment.pojo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;



/**
 * 评论展示 VO：联表带出评论人昵称/头像，回复额外带被回复人昵称；
 * liked 为当前登录用户的点赞状态（匿名访问时为 false）。
 */
@Data
public class CommentVO {
    private Integer id;
    private String targetType;
    private Integer targetId;
    private Integer userId;//评论人ID（前端据此判断是否可删除）
    private String content;
    private Integer parentId;
    private Integer rootId;
    private Integer replyUserId;
    private String nickname;//评论人昵称
    private String userPic;//评论人头像
    private String replyNickname;//被回复人昵称（回复才有）
    private Integer likeCount;
    private Integer score;//商品评分（仅商品顶级评论）
    private Integer orderId;//关联购买订单ID（商品晒单）
    private List<String> images;//评论图片
    private String imagesJson;//图片URL串（DB 原样，服务层解析到 images 后置空）
    private String purchasedText;//已购快照文本（如：已购「商品名」规格：xx）
    private Boolean liked;//当前用户是否已点赞
    private LocalDateTime createTime;
    private Integer replyCount;//该顶级评论下的回复总数
    private List<CommentVO> replies;//顶级评论的回复列表（列表接口只带前几条预览）
}
