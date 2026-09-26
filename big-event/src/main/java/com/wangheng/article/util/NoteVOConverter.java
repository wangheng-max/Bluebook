package com.wangheng.article.util;

import com.wangheng.article.pojo.Article;
import com.wangheng.article.pojo.NoteSearchVO;
import com.wangheng.merchant.mapper.MerchantInfoMapper;
import com.wangheng.merchant.pojo.MerchantInfo;
import com.wangheng.product.mapper.ProductMapper;
import com.wangheng.product.pojo.Product;
import com.wangheng.product.pojo.ProductBriefVO;
import com.wangheng.user.mapper.UserMapper;
import com.wangheng.user.pojo.User;


























import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Article → NoteSearchVO 共享转换器。
 * 搜索、社区热点、分类浏览三处共用，避免重复代码。
 */
@Component
public class NoteVOConverter {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private MerchantInfoMapper merchantInfoMapper;

    public NoteSearchVO convert(Article article) {
        NoteSearchVO vo = new NoteSearchVO();
        vo.setNoteId(article.getId());
        vo.setTitle(article.getTitle());

        // 生成摘要：去除HTML/纯文本后截取前150字
        String content = article.getContent();
        if (content != null) {
            String plainText = content.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
            if (plainText.length() > 150) {
                plainText = plainText.substring(0, 150) + "…";
            }
            vo.setSummary(plainText);
        }

        vo.setCoverImage(article.getCoverImg());
        vo.setAuthorId(article.getCreateUser());
        vo.setCreateTime(article.getCreateTime());
        vo.setViewCount(article.getViewCount() != null ? article.getViewCount() : 0);
        vo.setLikeCount(article.getLikeCount() != null ? article.getLikeCount() : 0);
        vo.setFavoriteCount(article.getFavoriteCount() != null ? article.getFavoriteCount() : 0);
        vo.setForwardCount(article.getForwardCount() != null ? article.getForwardCount() : 0);
        vo.setCommentCount(0);

        // 查询作者信息
        if (article.getCreateUser() != null) {
            User author = userMapper.findById(article.getCreateUser());
            if (author != null) {
                vo.setAuthorName(author.getUsername());
                vo.setAuthorAvatar(author.getUserPic());
            }
        }

        // 标签（逗号分隔存储 → 列表返回）
        vo.setTags(splitCsv(article.getTags()));

        // 带货商品：解析商品ID并批量查商品（下架商品照常返回，前端置灰）
        List<Integer> productIds = toIntList(article.getProductIds());
        if (!productIds.isEmpty()) {
            List<ProductBriefVO> briefs = new ArrayList<>();
            for (Product p : productMapper.findByIds(productIds)) {
                briefs.add(new ProductBriefVO(p.getId(), p.getName(), p.getCoverImg(), p.getPrice(), p.getStatus()));
            }
            vo.setProducts(briefs);
        }

        // 作者认证店铺：审核通过才暴露店铺入口（跳店铺页）
        if (article.getCreateUser() != null) {
            MerchantInfo shop = merchantInfoMapper.findByUserId(article.getCreateUser());
            if (shop != null && Integer.valueOf(1).equals(shop.getMerchantStatus())) {
                vo.setShopUserId(shop.getUserId());
                vo.setShopName(shop.getShopName());
            }
        }
        return vo;
    }

    /** 逗号分隔字符串 → 去空白去空项的列表 */
    public static List<String> splitCsv(String csv) {
        List<String> result = new ArrayList<>();
        if (!StringUtils.hasText(csv)) {
            return result;
        }
        for (String s : csv.split("[,，]")) {
            String t = s.trim();
            if (!t.isEmpty()) {
                result.add(t);
            }
        }
        return result;
    }

    /** 逗号分隔ID串 → Integer 列表（非法项忽略） */
    private List<Integer> toIntList(String csv) {
        List<Integer> result = new ArrayList<>();
        if (!StringUtils.hasText(csv)) {
            return result;
        }
        for (String s : csv.split("[,，]")) {
            try {
                result.add(Integer.valueOf(s.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }
}
