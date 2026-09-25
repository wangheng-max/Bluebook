package com.wangheng.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体（表 product）。
 * images 为 JSON 数组字符串（前端传 List<String>，服务层用 ObjectMapper 转换）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private Integer id;
    private String name;
    private Integer categoryId;
    private String coverImg;
    /** 详情图 JSON 数组字符串 */
    private String images;
    private String videoUrl;
    private String description;
    private BigDecimal price;
    private BigDecimal marketPrice;
    /** 1=上架 0=下架 */
    private Integer status;
    private Integer stock;
    private Integer salesCount;
    private Integer viewCount;
    private Integer createUserId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
