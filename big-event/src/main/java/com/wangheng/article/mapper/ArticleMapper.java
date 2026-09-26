package com.wangheng.article.mapper;

import com.wangheng.article.pojo.Article;






import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ArticleMapper {
    //新增
    @Insert("insert into article(title,content,cover_img,state,category_id,create_user,create_time,update_time,tags,product_ids) " +
            "values(#{title},#{content},#{coverImg},#{state},#{categoryId},#{createUser},#{createTime},#{updateTime},#{tags},#{productIds})")
    void add(Article article);


    List<Article> list(Integer userId, Integer categoryId, String state);

    //搜索公开笔记（关键词全文检索）
    List<Article> searchPublicNotes(String keyword);

    //按标签搜索公开笔记
    List<Article> searchPublicNotesByTag(String tag);

    //根据ID查询文章（社区详情）
    @Select("select * from article where id = #{id}")
    Article findById(Integer id);

    //按分类分页查询已发布文章（社区浏览）
    List<Article> listByCategory(Integer categoryId);

    //批量按ID查询文章（热点组装，保持传入顺序）
    List<Article> findByIds(@Param("ids") List<Integer> ids);

    //DB兜底：按浏览量取热点TopN（Redis ZSET 冷启动时使用）
    @Select("select * from article where state = '已发布' order by view_count desc, create_time desc limit #{limit}")
    List<Article> findHotTop(int limit);

    //全部已发布文章（热度定时重算用）
    @Select("select * from article where state = '已发布'")
    List<Article> findAllPublished();

    //近期已发布文章的标签列（热门标签统计用，轻量查询）
    @Select("select tags from article where state = '已发布' and tags is not null order by create_time desc limit 1000")
    List<String> findRecentTags();
}
