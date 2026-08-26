package com.wangheng.utils;

import com.wangheng.mapper.UserMapper;
import com.wangheng.pojo.Article;
import com.wangheng.pojo.NoteSearchVO;
import com.wangheng.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Article → NoteSearchVO 共享转换器。
 * 搜索、社区热点、分类浏览三处共用，避免重复代码。
 */
@Component
public class NoteVOConverter {

    @Autowired
    private UserMapper userMapper;

    public NoteSearchVO convert(Article article) {
        NoteSearchVO vo = new NoteSearchVO();
        vo.setNoteId(article.getId());
        vo.setTitle(article.getTitle());

        // 生成摘要：去除HTML/纯文本后截取前150字
        String content = article.getContent();
        if (content != null) {
            String plainText = content.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
            if (plainText.length() > 150) {
                plainText = plainText.substring(0, 150) + "…";
            }
            vo.setSummary(plainText);
        }

        vo.setCoverImage(article.getCoverImg());
        vo.setAuthorId(article.getCreateUser());
        vo.setCreateTime(article.getCreateTime());
        vo.setViewCount(article.getViewCount() != null ? article.getViewCount() : 0);
        vo.setLikeCount(article.getLikeCount() != null ? article.getLikeCount() : 0);
        vo.setFavoriteCount(article.getFavoriteCount() != null ? article.getFavoriteCount() : 0);
        vo.setForwardCount(article.getForwardCount() != null ? article.getForwardCount() : 0);
        vo.setCommentCount(0);

        // 查询作者信息
        if (article.getCreateUser() != null) {
            User author = userMapper.findById(article.getCreateUser());
            if (author != null) {
                vo.setAuthorName(author.getUsername());
                vo.setAuthorAvatar(author.getUserPic());
            }
        }
        return vo;
    }
}
