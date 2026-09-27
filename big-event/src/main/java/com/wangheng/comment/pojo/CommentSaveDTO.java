package com.wangheng.comment.pojo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;


/**
 * 发布评论/回复请求体。
 * parentId 为空或不传表示顶级评论；有值表示回复该评论（回复的回复也直接传被回复的评论ID）。
 */
@Data
public class CommentSaveDTO {
    @NotBlank
    @Pattern(regexp = "^(article|product|rating)$", message = "评论目标类型不合法")
    private String targetType;//article/product/rating（rating=商家评价的评论楼）

    @NotNull(message = "评论目标ID不能为空")
    private Integer targetId;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容不能超过1000字")
    private String content;

    private Integer parentId;//父评论ID，空=顶级评论

    // ===== 商品评论扩展（targetType=product 的顶级评论生效）=====
    @Min(value = 1, message = "评分最低 1 星")
    @Max(value = 5, message = "评分最高 5 星")
    private Integer score;//商品评分 1-5

    @Size(max = 4, message = "评论图片最多 4 张")
    private List<String> images;//评论图片 URL

    private Integer orderId;//关联购买订单（晒单，展示所购规格快照）
}
