package com.wangheng.article.mapper;

import com.wangheng.article.pojo.Category;






import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CategoryMapper {
    //新增（用户自建分类，is_system 走数据库默认 0）
    @Insert("insert into category(category_name,category_alias,create_user,is_system,create_time,update_time) " +
            "values(#{categoryName},#{categoryAlias},#{createUser},0,#{createTime},#{updateTime})")
    void add(Category category);

    //查询分类列表：系统内置分类（is_system=1） + 当前用户自建分类
    @Select("SELECT * FROM category WHERE is_system = 1 OR create_user = #{userId}")
    List<Category> list(Integer userId);

    //根据id查询
    @Select("select * from category where id =#{Id}")
    Category findById(Integer id);

    //校验是否为系统内置分类
    @Select("select count(*) from category where id = #{id} and is_system = 1")
    int countSystemById(Integer id);

    //校验分类对当前用户是否可用（系统分类 或 自己创建的分类）
    @Select("select count(*) from category where id = #{id} and (is_system = 1 or create_user = #{userId})")
    int countValidForUser(Integer id, Integer userId);

    //更新
    @Update("update category set category_name=#{categoryName},category_alias=#{categoryAlias},update_time=#{updateTime} where id=#{id}")
    void update(Category category);

    //根据id删除
    @Delete("delete from category where id=#{id}")
    void deleteById(Integer id);
}
