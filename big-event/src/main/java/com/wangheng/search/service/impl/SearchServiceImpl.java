package com.wangheng.search.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.wangheng.article.mapper.ArticleMapper;
import com.wangheng.article.pojo.Article;
import com.wangheng.article.pojo.NoteSearchVO;
import com.wangheng.article.util.NoteVOConverter;
import com.wangheng.common.PageBean;
import com.wangheng.friend.mapper.FriendMapper;
import com.wangheng.search.pojo.SearchUserVO;
import com.wangheng.search.service.SearchCacheService;
import com.wangheng.search.service.SearchService;
import com.wangheng.user.mapper.UserMapper;
import com.wangheng.user.pojo.User;
import com.wangheng.utils.ThreadLocalUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private FriendMapper friendMapper;

    @Autowired
    private SearchCacheService searchCacheService;

    @Autowired
    private NoteVOConverter noteVOConverter;

    @Override
    public PageBean<NoteSearchVO> searchNotes(String keyword, Integer pageNum, Integer pageSize) {
        // 先查缓存：公开笔记搜索是热点数据，缓存5分钟
        PageBean<NoteSearchVO> cached = searchCacheService.getNotesByKeyword(keyword, pageNum, pageSize);
        if (cached != null) {
            return cached;
        }

        PageBean<NoteSearchVO> pb = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);

        List<Article> articles = articleMapper.searchPublicNotes(keyword);
        Page<Article> p = (Page<Article>) articles;

        List<NoteSearchVO> voList = new ArrayList<>();
        for (Article a : p.getResult()) {
            NoteSearchVO vo = noteVOConverter.convert(a);
            voList.add(vo);
        }

        pb.setTotal(p.getTotal());
        pb.setItems(voList);

        // 写入缓存
        searchCacheService.putNotesByKeyword(keyword, pageNum, pageSize, pb);
        return pb;
    }

    @Override
    public PageBean<NoteSearchVO> searchNotesByTag(String tag, Integer pageNum, Integer pageSize) {
        // 先查缓存
        PageBean<NoteSearchVO> cached = searchCacheService.getNotesByTag(tag, pageNum, pageSize);
        if (cached != null) {
            return cached;
        }

        PageBean<NoteSearchVO> pb = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);

        List<Article> articles = articleMapper.searchPublicNotesByTag(tag);
        Page<Article> p = (Page<Article>) articles;

        List<NoteSearchVO> voList = new ArrayList<>();
        for (Article a : p.getResult()) {
            NoteSearchVO vo = noteVOConverter.convert(a);
            voList.add(vo);
        }

        pb.setTotal(p.getTotal());
        pb.setItems(voList);

        // 写入缓存
        searchCacheService.putNotesByTag(tag, pageNum, pageSize, pb);
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

}
