package com.wangheng.service;

import com.wangheng.pojo.NoteSearchVO;
import com.wangheng.pojo.PageBean;
import com.wangheng.pojo.SearchUserVO;

public interface SearchService {

    // 搜索公开笔记
    PageBean<NoteSearchVO> searchNotes(String keyword, Integer pageNum, Integer pageSize);

    // 按标签搜索公开笔记
    PageBean<NoteSearchVO> searchNotesByTag(String tag, Integer pageNum, Integer pageSize);

    // 搜索用户
    PageBean<SearchUserVO> searchUsers(String keyword, Integer pageNum, Integer pageSize);
}
