package com.wangheng.common;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



//分页返回结果对象
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageBean < T>{
    private Long total;//总条数
    private List<T> items;//当前页数据集合
}
