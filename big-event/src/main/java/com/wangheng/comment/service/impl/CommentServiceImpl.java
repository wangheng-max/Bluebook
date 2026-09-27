package com.wangheng.comment.service.impl;

import com.wangheng.article.mapper.ArticleMapper;
import com.wangheng.article.util.NoteVOConverter;
import com.wangheng.comment.mapper.CommentMapper;
import com.wangheng.comment.pojo.Comment;
import com.wangheng.comment.pojo.CommentSaveDTO;
import com.wangheng.comment.pojo.CommentVO;
import com.wangheng.comment.service.CommentService;
import com.wangheng.common.PageBean;
import com.wangheng.order.mapper.OrderItemMapper;
import com.wangheng.order.pojo.OrderItem;
import com.wangheng.order.mapper.OrderMapper;
import com.wangheng.product.mapper.ProductMapper;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



@Service
public class CommentServiceImpl implements CommentService {

    /** 列表页每个顶级评论默认预览的回复条数，其余通过回复分页接口加载 */
    private static final int REPLY_PREVIEW_SIZE = 2;

    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private com.wangheng.merchant.mapper.MerchantRatingMapper merchantRatingMapper;

    @Override
    public void add(CommentSaveDTO dto, Integer userId) {
        checkTargetExists(dto.getTargetType(), dto.getTargetId());

        Comment comment = new Comment();
        comment.setTargetType(dto.getTargetType());
        comment.setTargetId(dto.getTargetId());
        comment.setUserId(userId);
        comment.setContent(dto.getContent().trim());

        // 商品顶级评论：评分 + 晒图 + 晒单（回复不支持）
        boolean topLevel = dto.getParentId() == null || dto.getParentId() <= 0;
        if ("product".equals(dto.getTargetType()) && topLevel) {
            comment.setScore(dto.getScore());
            if (dto.getImages() != null && !dto.getImages().isEmpty()) {
                comment.setImages(String.join(",", dto.getImages()));
            }
            if (dto.getOrderId() != null) {
                // 晒单校验：必须是自己的、已支付及之后状态、且包含该商品的订单
                if (orderMapper.countOwnPaidOrderWithProduct(dto.getOrderId(), userId, dto.getTargetId()) == 0) {
                    throw new RuntimeException("只能关联自己购买该商品的订单");
                }
                comment.setOrderId(dto.getOrderId());
            }
        }

        if (dto.getParentId() == null || dto.getParentId() <= 0) {
            // 顶级评论
            comment.setParentId(0);
            comment.setRootId(0);
            comment.setReplyUserId(null);
        } else {
            // 回复：parent 指向被回复的评论，root 归属其所在楼层
            Comment parent = commentMapper.findById(dto.getParentId());
            if (parent == null) {
                throw new RuntimeException("回复的评论不存在或已删除");
            }
            if (!parent.getTargetType().equals(dto.getTargetType()) || !parent.getTargetId().equals(dto.getTargetId())) {
                throw new RuntimeException("回复的评论不属于该内容");
            }
            comment.setParentId(parent.getId());
            comment.setRootId(parent.getParentId() == 0 ? parent.getId() : parent.getRootId());
            comment.setReplyUserId(parent.getUserId());
        }
        commentMapper.insert(comment);
    }

    @Override
    public PageBean<CommentVO> list(String targetType, Integer targetId, String sort, Integer pageNum, Integer pageSize) {
        String order = "likes".equals(sort) ? "likes" : "time";
        Long total = commentMapper.countRoots(targetType, targetId);
        if (total == null || total == 0) {
            return new PageBean<>(0L, new ArrayList<>());
        }

        List<CommentVO> roots = commentMapper.pageRoots(targetType, targetId, order, (pageNum - 1) * pageSize, pageSize);
        if (roots.isEmpty()) {
            return new PageBean<>(total, roots);
        }

        // 楼中楼预览 + 回复总数：按根评论批量装配，避免逐条查询
        List<Integer> rootIds = roots.stream().map(CommentVO::getId).collect(Collectors.toList());
        List<CommentVO> allReplies = commentMapper.selectRepliesByRootIds(rootIds);
        Map<Integer, List<CommentVO>> repliesByRoot = allReplies.stream()
                .collect(Collectors.groupingBy(CommentVO::getRootId));

        for (CommentVO root : roots) {
            List<CommentVO> replies = repliesByRoot.getOrDefault(root.getId(), new ArrayList<>());
            root.setReplyCount(replies.size());
            root.setReplies(replies.size() <= REPLY_PREVIEW_SIZE ? replies
                    : new ArrayList<>(replies.subList(0, REPLY_PREVIEW_SIZE)));
        }

        markLikedStatus(roots, repliesByRoot);
        fillProductExtras(roots);
        fillProductExtras(allReplies);
        return new PageBean<>(total, roots);
    }

    @Override
    public PageBean<CommentVO> replies(Integer rootId, Integer pageNum, Integer pageSize) {
        Comment root = commentMapper.findById(rootId);
        if (root == null || root.getParentId() != 0) {
            throw new RuntimeException("评论不存在或已删除");
        }
        Long total = commentMapper.countReplies(rootId);
        if (total == null || total == 0) {
            return new PageBean<>(0L, new ArrayList<>());
        }
        List<CommentVO> items = commentMapper.pageReplies(rootId, (pageNum - 1) * pageSize, pageSize);
        markLikedStatus(new ArrayList<>(), Map.of(rootId, new ArrayList<>(items)));
        fillProductExtras(items);
        return new PageBean<>(total, items);
    }

    @Override
    public Long like(Integer commentId, Integer userId) {
        requireComment(commentId);
        if (commentMapper.insertLike(commentId, userId) > 0) {
            commentMapper.incrLikeCount(commentId);
        }
        return Long.valueOf(commentMapper.findById(commentId).getLikeCount());
    }

    @Override
    public Long unlike(Integer commentId, Integer userId) {
        requireComment(commentId);
        if (commentMapper.deleteLike(commentId, userId) > 0) {
            commentMapper.decrLikeCount(commentId);
        }
        return Long.valueOf(commentMapper.findById(commentId).getLikeCount());
    }

    @Override
    @Transactional
    public void delete(Integer commentId, Integer userId) {
        Comment comment = requireComment(commentId);
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("只能删除自己的评论");
        }
        if (comment.getParentId() == 0) {
            // 顶级评论：连带全部回复及其点赞明细一起删除
            List<Integer> ids = new ArrayList<>(commentMapper.selectReplyIds(commentId));
            ids.add(commentId);
            commentMapper.deleteLikesByCommentIds(ids);
            commentMapper.deleteRootCascade(commentId);
        } else {
            commentMapper.deleteLikesByCommentIds(List.of(commentId));
            commentMapper.deleteById(commentId);
        }
    }

    /** 商品评论扩展装配：解析晒图列表 + 批量回填已购快照文本 */
    private void fillProductExtras(List<CommentVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return;
        }
        List<Integer> orderIds = vos.stream().filter(v -> v.getOrderId() != null)
                .map(CommentVO::getOrderId).distinct().collect(Collectors.toList());
        Map<Integer, String> snapshots = orderIds.isEmpty() ? Map.of()
                : orderItemMapper.findByOrderIds(orderIds).stream()
                        .collect(Collectors.toMap(OrderItem::getOrderId, oi -> {
                            String spec = oi.getSkuName() == null || oi.getSkuName().isBlank() ? ""
                                    : " 规格：" + oi.getSkuName();
                            return "已购「" + oi.getProductName() + "」" + spec;
                        }, (a, b) -> a));
        for (CommentVO vo : vos) {
            if (vo.getImagesJson() != null && !vo.getImagesJson().isBlank()) {
                vo.setImages(NoteVOConverter.splitCsv(vo.getImagesJson()));
                vo.setImagesJson(null);
            }
            if (vo.getOrderId() != null) {
                vo.setPurchasedText(snapshots.get(vo.getOrderId()));
            }
        }
    }

    @Override
    public Map<String, Object> stats(String targetType, Integer targetId) {
        Double avg = commentMapper.avgScore(targetType, targetId);
        Map<String, Object> result = new HashMap<>();
        result.put("avgScore", avg == null ? null : Math.round(avg * 10) / 10.0);
        result.put("scoreCount", commentMapper.countScored(targetType, targetId));
        return result;
    }

    /** 给本页展示的评论（含楼中楼预览）打上当前用户的点赞状态；匿名访问全部为 false */
    private void markLikedStatus(List<CommentVO> roots, Map<Integer, List<CommentVO>> repliesByRoot) {
        Integer userId = currentUserId();
        if (userId == null) {
            return;
        }
        List<Integer> shownIds = new ArrayList<>();
        roots.forEach(r -> shownIds.add(r.getId()));
        repliesByRoot.values().forEach(list -> list.forEach(r -> shownIds.add(r.getId())));
        if (shownIds.isEmpty()) {
            return;
        }
        Set<Integer> likedIds = new HashSet<>(commentMapper.selectLikedCommentIds(userId, shownIds));
        roots.forEach(r -> r.setLiked(likedIds.contains(r.getId())));
        repliesByRoot.values().forEach(list -> list.forEach(r -> r.setLiked(likedIds.contains(r.getId()))));
    }

    private void checkTargetExists(String targetType, Integer targetId) {
        if ("article".equals(targetType)) {
            if (articleMapper.findById(targetId) == null) {
                throw new RuntimeException("评论的文章不存在");
            }
        } else if ("product".equals(targetType)) {
            if (productMapper.findById(targetId) == null) {
                throw new RuntimeException("评论的商品不存在");
            }
        } else if ("rating".equals(targetType)) {
            // 商家评价也可被评论（店铺页评价展开评论区）
            if (merchantRatingMapper.countById(targetId) == 0) {
                throw new RuntimeException("评论的评价不存在");
            }
        } else {
            throw new RuntimeException("评论目标类型不合法");
        }
    }

    private Comment requireComment(Integer commentId) {
        Comment comment = commentMapper.findById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在或已删除");
        }
        return comment;
    }

    private Integer currentUserId() {
        Map<String, Object> claims = com.wangheng.utils.ThreadLocalUtil.get();
        return claims == null ? null : (Integer) claims.get("id");
    }
}
