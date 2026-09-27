package com.wangheng.article.pojo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * 转发文章请求体
 */
@Data
public class ForwardDTO {
    @NotNull(message = "目标好友不能为空")
    private Integer toUserId;
}
