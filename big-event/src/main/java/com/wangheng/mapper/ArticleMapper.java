package com.wangheng.mapper;

import com.wangheng.pojo.Article;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ArticleMapper {
    //新增
    @Insert("insert into article(title,content,cover_img,state,category_id,create_user,create_time,update_time,tags) " +
            "values(#{title},#{content},#{coverImg},#{state},#{categoryId},#{createUser},#{createTime},#{updateTime},#{tags})")
    void add(Article article);


    List<Article> list(Integer userId, Integer categoryId, String state);

    //搜索公开笔记（关键词全文检索）
    List<Article> searchPublicNotes(String keyword);

    //按标签搜索公开笔记
    List<Article> searchPublicNotesByTag(String tag);
}
