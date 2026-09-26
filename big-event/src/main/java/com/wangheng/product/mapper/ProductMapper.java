package com.wangheng.product.mapper;

import com.wangheng.product.pojo.Product;






import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品表 Mapper。
 * 动态查询（list/listOwn/search）在 ProductMapper.xml；
 * 简单 CRUD 使用注解。
 */
@Mapper
public interface ProductMapper {

    /** 商品列表（分类/状态筛选，公开） */
    List<Product> list(@Param("categoryId") Integer categoryId, @Param("status") Integer status);

    /** 商家自己的商品列表（数据隔离） */
    List<Product> listOwn(@Param("userId") Integer userId, @Param("status") Integer status);

    /** 商品搜索（仅上架，关键词/分类/价格区间） */
    List<Product> search(@Param("keyword") String keyword,
                         @Param("categoryId") Integer categoryId,
                         @Param("minPrice") BigDecimal minPrice,
                         @Param("maxPrice") BigDecimal maxPrice);

    @Select("select * from product where id=#{id}")
    Product findById(Integer id);

    /** 批量按 ID 查商品（文章带货卡片组装，保持传入顺序） */
    List<Product> findByIds(@Param("ids") List<Integer> ids);

    /** 店铺在架商品（店铺页公开列表，按创建时间倒序；PageHelper 分页） */
    List<Product> listShopApproved(@Param("merchantUserId") Integer merchantUserId);

    /** 店铺在架商品数（店铺页汇总） */
    @Select("select count(*) from product where create_user_id=#{merchantUserId} and status=1")
    int countShopApproved(@Param("merchantUserId") Integer merchantUserId);

    @Insert("insert into product(name,category_id,cover_img,images,video_url,description,price,market_price," +
            "status,stock,sales_count,view_count,create_user_id,create_time,update_time) " +
            "values(#{name},#{categoryId},#{coverImg},#{images},#{videoUrl},#{description},#{price},#{marketPrice}," +
            "#{status},#{stock},0,0,#{createUserId},now(),now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Product product);

    /** 修改商品：WHERE 同时校验归属（防越权改他人商品） */
    @Update("update product set name=#{name},category_id=#{categoryId},cover_img=#{coverImg},images=#{images}," +
            "video_url=#{videoUrl},description=#{description},price=#{price},market_price=#{marketPrice}," +
            "status=#{status},stock=#{stock},update_time=now() " +
            "where id=#{id} and create_user_id=#{createUserId}")
    int update(Product product);

    /** 下架商品：WHERE 同时校验归属 */
    @Update("update product set status=0,update_time=now() where id=#{id} and create_user_id=#{userId}")
    int offShelf(@Param("id") Integer id, @Param("userId") Integer userId);

    /** 下单扣库存（条件扣减防超卖）：上架且库存充足才成功 */
    @Update("update product set stock=stock-#{quantity},sales_count=sales_count+#{quantity} " +
            "where id=#{id} and status=1 and stock>=#{quantity}")
    int deductStock(@Param("id") Integer id, @Param("quantity") Integer quantity);

    /** 取消/退款回补库存 */
    @Update("update product set stock=stock+#{quantity} where id=#{id}")
    int restoreStock(@Param("id") Integer id, @Param("quantity") Integer quantity);
}
