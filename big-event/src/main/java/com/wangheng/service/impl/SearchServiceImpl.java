package com.wangheng.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.wangheng.mapper.ArticleMapper;
import com.wangheng.mapper.FriendMapper;
import com.wangheng.mapper.UserMapper;
import com.wangheng.pojo.*;
import com.wangheng.service.SearchService;
import com.wangheng.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private FriendMapper friendMapper;

    @Override
    public PageBean<NoteSearchVO> searchNotes(String keyword, Integer pageNum, Integer pageSize) {
        PageBean<NoteSearchVO> pb = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);

        List<Article> articles = articleMapper.searchPublicNotes(keyword);
        Page<Article> p = (Page<Article>) articles;

        List<NoteSearchVO> voList = new ArrayList<>();
        for (Article a : p.getResult()) {
            NoteSearchVO vo = convertToNoteSearchVO(a);
            voList.add(vo);
        }

        pb.setTotal(p.getTotal());
        pb.setItems(voList);
        return pb;
    }

    @Override
    public PageBean<NoteSearchVO> searchNotesByTag(String tag, Integer pageNum, Integer pageSize) {
        PageBean<NoteSearchVO> pb = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);

        List<Article> articles = articleMapper.searchPublicNotesByTag(tag);
        Page<Article> p = (Page<Article>) articles;

        List<NoteSearchVO> voList = new ArrayList<>();
        for (Article a : p.getResult()) {
            NoteSearchVO vo = convertToNoteSearchVO(a);
            voList.add(vo);
        }

        pb.setTotal(p.getTotal());
        pb.setItems(voList);
        return pb;
    }

    @Override
    public PageBean<SearchUserVO> searchUsers(String keyword, Integer pageNum, Integer pageSize) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer currentUserId = (Integer) claims.get("id");

        PageBean<SearchUserVO> pb = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);

        List<User> users = userMapper.searchUsers(keyword, currentUserId);
        Page<User> p = (Page<User>) users;

        List<SearchUserVO> voList = new ArrayList<>();
        for (User u : p.getResult()) {
            SearchUserVO vo = new SearchUserVO();
            vo.setUserId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setNickname(u.getNickname());
            vo.setAvatar(u.getUserPic());

            // 检查是否为好友
            int friendCount = friendMapper.isFriend(currentUserId, u.getId());
            vo.setIsFriend(friendCount > 0);

            // 检查是否有待处理的好友请求
            int pendingCount = friendMapper.hasPendingRequest(currentUserId, u.getId());
            vo.setHasRequested(pendingCount > 0);

            voList.add(vo);
        }

        pb.setTotal(p.getTotal());
        pb.setItems(voList);
        return pb;
    }

    /**
     * 将Article转换为NoteSearchVO
     */
    private NoteSearchVO convertToNoteSearchVO(Article article) {
        NoteSearchVO vo = new NoteSearchVO();
        vo.setNoteId(article.getId());
        vo.setTitle(article.getTitle());

        // 生成摘要：去除HTML/纯文本后截取前150字
        String content = article.getContent();
        if (content != null) {
            // 简单的HTML标签去除
            String plainText = content.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
            if (plainText.length() > 150) {
                plainText = plainText.substring(0, 150) + "…";
            }
            vo.setSummary(plainText);
        }

        vo.setCoverImage(article.getCoverImg());
        vo.setAuthorId(article.getCreateUser());
        vo.setCreateTime(article.getCreateTime());
        vo.setLikeCount(0);
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
