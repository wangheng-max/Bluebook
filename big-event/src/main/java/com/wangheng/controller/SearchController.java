package com.wangheng.controller;

import com.wangheng.pojo.NoteSearchVO;
import com.wangheng.pojo.PageBean;
import com.wangheng.pojo.Result;
import com.wangheng.pojo.SearchUserVO;
import com.wangheng.service.SearchService;
import jakarta.validation.constraints.NotEmpty;
import org.hibernate.validator.constraints.Length;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/search")
@Validated
public class SearchController {

    @Autowired
    private SearchService searchService;

    /**
     * 搜索公开笔记（关键词）
     */
    @GetMapping("/notes")
    public Result<PageBean<NoteSearchVO>> searchNotes(
            @RequestParam @NotEmpty @Length(min = 1, max = 50) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "time", required = false) String sort) {
        PageBean<NoteSearchVO> pb = searchService.searchNotes(keyword, page, size);
        return Result.success(pb);
    }

    /**
     * 按标签搜索公开笔记
     */
    @GetMapping("/notes/by-tag")
    public Result<PageBean<NoteSearchVO>> searchNotesByTag(
            @RequestParam @NotEmpty @Length(min = 1, max = 30) String tag,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        PageBean<NoteSearchVO> pb = searchService.searchNotesByTag(tag, page, size);
        return Result.success(pb);
    }

    /**
     * 搜索用户
     */
    @GetMapping("/users")
    public Result<PageBean<SearchUserVO>> searchUsers(
            @RequestParam @NotEmpty @Length(min = 1, max = 30) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        PageBean<SearchUserVO> pb = searchService.searchUsers(keyword, page, size);
        return Result.success(pb);
    }
}
