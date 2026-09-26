package com.wangheng.product.mapper;

import com.wangheng.product.pojo.ProductCategory;






import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 商品分类表 Mapper（管理员维护）
 */
@Mapper
public interface ProductCategoryMapper {

    @Insert("insert into product_category(name,parent_id,icon,sort_order,is_system,create_time,update_time) " +
            "values(#{name},#{parentId},#{icon},#{sortOrder},1,now(),now())")
    void insert(ProductCategory category);

    @Select("select * from product_category where id=#{id}")
    ProductCategory findById(Integer id);

    @Select("select * from product_category order by sort_order asc, id asc")
    List<ProductCategory> findAll();

    @Update("update product_category set name=#{name},parent_id=#{parentId},icon=#{icon}," +
            "sort_order=#{sortOrder} where id=#{id}")
    void update(ProductCategory category);

    @Delete("delete from product_category where id=#{id}")
    void deleteById(Integer id);

    /** 子分类数量（有子分类不允许删除） */
    @Select("select count(*) from product_category where parent_id=#{id}")
    int countChildren(Integer id);

    /** 分类下商品数量（有商品不允许删除） */
    @Select("select count(*) from product where category_id=#{id}")
    int countProducts(Integer id);
}
