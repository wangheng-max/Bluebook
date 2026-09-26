package com.wangheng.product.mapper;

import com.wangheng.product.pojo.ProductSku;






import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 商品 SKU 表 Mapper
 */
@Mapper
public interface ProductSkuMapper {

    @Insert("insert into product_sku(product_id,sku_name,spec_values,price,stock,sales_count) " +
            "values(#{productId},#{skuName},#{specValues},#{price},#{stock},0)")
    void insert(ProductSku sku);

    /** 删除商品全部 SKU（修改商品时整体替换；订单明细已存快照，历史订单不受影响） */
    @Delete("delete from product_sku where product_id=#{productId}")
    void deleteByProductId(Integer productId);

    @Select("select * from product_sku where product_id=#{productId} order by id asc")
    List<ProductSku> findByProductId(Integer productId);

    @Select("select * from product_sku where id=#{id}")
    ProductSku findById(Integer id);

    /** 下单扣 SKU 库存（条件扣减防超卖） */
    @Update("update product_sku set stock=stock-#{quantity},sales_count=sales_count+#{quantity} " +
            "where id=#{id} and stock>=#{quantity}")
    int deductStock(@Param("id") Integer id, @Param("quantity") Integer quantity);

    /** 取消/退款回补 SKU 库存 */
    @Update("update product_sku set stock=stock+#{quantity} where id=#{id}")
    int restoreStock(@Param("id") Integer id, @Param("quantity") Integer quantity);
}
