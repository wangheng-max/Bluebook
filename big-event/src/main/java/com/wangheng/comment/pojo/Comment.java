package com.wangheng.comment.pojo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论实体（文章/商品通用，target_type 区分目标）。
 * 两级展示模型：parent_id=0 为顶级评论；回复的 root_id 指向所属顶级评论，
 * parent_id 指向被回复的评论（回复的回复在 UI 上仍平铺在根评论楼层内）。
 */
@Data
public class Comment {
    private Integer id;//评论ID
    private String targetType;//目标类型：article/product
    private Integer targetId;//目标ID
    private Integer userId;//评论人ID
    private String content;//评论内容
    private Integer parentId;//父评论ID，0=顶级评论
    private Integer rootId;//根评论ID，顶级评论为0
    private Integer replyUserId;//被回复用户ID
    private Integer likeCount;//点赞数
    private LocalDateTime createTime;//创建时间
    private LocalDateTime updateTime;//更新时间
}
