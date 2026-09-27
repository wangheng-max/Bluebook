package com.wangheng.follow.pojo;

import java.time.LocalDateTime;
import lombok.Data;



/**
 * 关注实体（博主/店铺统一一张表，follow_type 区分）。
 */
@Data
public class Follow {
    private Integer id;
    private Integer followerId;//关注者用户ID
    private Integer followeeId;//被关注者ID（博主用户ID 或 商家用户ID）
    private Integer followType;//1=关注博主 2=关注店铺
    private LocalDateTime createTime;
}
