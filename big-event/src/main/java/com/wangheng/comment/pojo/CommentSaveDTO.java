package com.wangheng.comment.pojo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发布评论/回复请求体。
 * parentId 为空或不传表示顶级评论；有值表示回复该评论（回复的回复也直接传被回复的评论ID）。
 */
@Data
public class CommentSaveDTO {
    @NotBlank
    @Pattern(regexp = "^(article|product)$", message = "评论目标类型不合法")
    private String targetType;//article/product

    @NotNull(message = "评论目标ID不能为空")
    private Integer targetId;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容不能超过1000字")
    private String content;

    private Integer parentId;//父评论ID，空=顶级评论
}
