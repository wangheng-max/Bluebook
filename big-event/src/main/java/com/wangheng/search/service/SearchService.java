package com.wangheng.search.service;

import com.wangheng.article.pojo.NoteSearchVO;
import com.wangheng.common.PageBean;
import com.wangheng.search.pojo.SearchUserVO;


public interface SearchService {

    // 搜索公开笔记
    PageBean<NoteSearchVO> searchNotes(String keyword, Integer pageNum, Integer pageSize);

    // 按标签搜索公开笔记
    PageBean<NoteSearchVO> searchNotesByTag(String tag, Integer pageNum, Integer pageSize);

    // 搜索用户
    PageBean<SearchUserVO> searchUsers(String keyword, Integer pageNum, Integer pageSize);
}
