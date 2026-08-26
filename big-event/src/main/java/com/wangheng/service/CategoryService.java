package com.wangheng.service;

import com.wangheng.pojo.Category;

import java.util.List;

public interface CategoryService {
    //新增分类
    void add(Category category);

    //列表查询（系统内置分类 + 当前用户自建分类）
    List<Category> list();

    //根据id查询分类信息
    Category findById(Integer id);

    //校验是否为系统内置分类
    boolean isSystemCategory(Integer id);

    //校验分类对当前用户是否可用（系统分类 或 自己创建的分类）
    boolean isValidForUser(Integer id);

    //更新分类
    void update(Category category);


   // void delete(Integer id);

    //删除分类
    void deleteById(Integer id);
}
