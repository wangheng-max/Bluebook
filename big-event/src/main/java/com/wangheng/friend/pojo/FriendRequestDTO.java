package com.wangheng.friend.pojo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class FriendRequestDTO {
    @NotNull
    private Integer targetUserId;
}
