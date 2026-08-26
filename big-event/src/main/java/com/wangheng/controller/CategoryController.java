package com.wangheng.controller;

import com.wangheng.pojo.Category;
import com.wangheng.pojo.Result;
import com.wangheng.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    @PostMapping
    public Result add(@RequestBody @Validated(Category.Add.class) Category category){
        categoryService.add(category);
        return Result.success();
    }

    @GetMapping
    public Result<List<Category>> list(){
        List<Category> cs = categoryService.list();
        return Result.success(cs);
    }

    /**
     * 分类列表（社区扩展：系统内置分类 + 个人分类，Redis 缓存，秒开）
     */
    @GetMapping("/list")
    public Result<List<Category>> listV2(){
        return list();
    }

    @GetMapping("/detail")
    public Result<Category> detail(Integer id){
        Category c = categoryService.findById(id);
        return Result.success(c);
    }

    @PutMapping
    public Result update(@RequestBody @Validated(Category.Update.class) Category category){
        categoryService.update(category);
        return Result.success();
    }

   @DeleteMapping
    public Result delete(Integer id){
        categoryService.deleteById(id);
        return Result.success();
   }
}
