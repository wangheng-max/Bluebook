package com.wangheng.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 公开笔记搜索结果展示对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteSearchVO {
    private Integer noteId;       // 笔记ID
    private String title;         // 标题
    private String summary;       // 正文摘要（前150字）
    private String coverImage;    // 封面图URL
    private Integer authorId;     // 作者用户ID
    private String authorName;    // 作者用户名
    private String authorAvatar;  // 作者头像URL
    private Integer likeCount;    // 点赞数
    private Integer commentCount; // 评论数
    private LocalDateTime createTime; // 创建时间
}
