package com.wangheng.article.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.wangheng.article.mapper.ArticleMapper;
import com.wangheng.article.mapper.FavoriteFolderMapper;
import com.wangheng.article.mapper.InteractionMapper;
import com.wangheng.article.pojo.Article;
import com.wangheng.article.pojo.FavoriteFolder;
import com.wangheng.article.pojo.FavoriteFolderVO;
import com.wangheng.article.pojo.InteractStatusVO;
import com.wangheng.article.pojo.NoteSearchVO;
import com.wangheng.article.service.InteractionService;
import com.wangheng.common.PageBean;
import com.wangheng.article.util.NoteVOConverter;
import com.wangheng.friend.mapper.FriendMapper;
import com.wangheng.message.pojo.Message;
import com.wangheng.message.service.MessageService;
import com.wangheng.websocket.ChatWebSocketHandler;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

























@Service
public class InteractionServiceImpl implements InteractionService {

    /** 热点 ZSET：成员=文章ID，score=热度分 */
    private static final String HOT_KEY = "hot:articles";

    /** 热度加权：view×1 + like×2 + favorite×3 + forward×2 */
    private static final double HOT_LIKE = 2;
    private static final double HOT_FAVORITE = 3;
    private static final double HOT_FORWARD = 2;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private FriendMapper friendMapper;

    @Autowired
    private InteractionMapper interactionMapper;
    @Autowired
    private FavoriteFolderMapper favoriteFolderMapper;
    @Autowired
    private NoteVOConverter noteVOConverter;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Long like(Integer articleId, Integer userId) {
        checkArticle(articleId);
        Long added = stringRedisTemplate.opsForSet().add(likeKey(articleId), String.valueOf(userId));
        // 首次点赞才加分（幂等）
        if (added != null && added > 0) {
            stringRedisTemplate.opsForZSet().incrementScore(HOT_KEY, String.valueOf(articleId), HOT_LIKE);
        }
        return stringRedisTemplate.opsForSet().size(likeKey(articleId));
    }

    @Override
    public Long unlike(Integer articleId, Integer userId) {
        Long removed = stringRedisTemplate.opsForSet().remove(likeKey(articleId), String.valueOf(userId));
        if (removed != null && removed > 0) {
            stringRedisTemplate.opsForZSet().incrementScore(HOT_KEY, String.valueOf(articleId), -HOT_LIKE);
        }
        return stringRedisTemplate.opsForSet().size(likeKey(articleId));
    }

    @Override
    public Long favorite(Integer articleId, Integer userId) {
        return favorite(articleId, userId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long favorite(Integer articleId, Integer userId, Integer folderId) {
        checkArticle(articleId);
        if (folderId != null) {
            FavoriteFolder folder = favoriteFolderMapper.findById(folderId);
            if (folder == null || !folder.getUserId().equals(userId)) {
                throw new RuntimeException("收藏夹不存在");
            }
        }
        Long added = stringRedisTemplate.opsForSet().add(favKey(articleId), String.valueOf(userId));
        if (added != null && added > 0) {
            stringRedisTemplate.opsForZSet().incrementScore(HOT_KEY, String.valueOf(articleId), HOT_FAVORITE);
            // 收藏时立即落库以保留收藏夹归属（对账任务的后续 INSERT IGNORE 会因唯一键空操作）
            interactionMapper.insertFavorite(articleId, userId, folderId);
        }
        return stringRedisTemplate.opsForSet().size(favKey(articleId));
    }

    @Override
    public PageBean<NoteSearchVO> likedList(Integer userId, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Article> list = interactionMapper.listLikedArticles(userId);
        Page<Article> page = (Page<Article>) list;
        List<NoteSearchVO> voList = new ArrayList<>();
        for (Article a : page.getResult()) {
            voList.add(noteVOConverter.convert(a));
        }
        return new PageBean<>(page.getTotal(), voList);
    }

    @Override
    public PageBean<NoteSearchVO> favoriteList(Integer userId, Integer folderId, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Article> list = interactionMapper.listFavoriteArticles(userId, folderId);
        Page<Article> page = (Page<Article>) list;
        List<NoteSearchVO> voList = new ArrayList<>();
        for (Article a : page.getResult()) {
            voList.add(noteVOConverter.convert(a));
        }
        return new PageBean<>(page.getTotal(), voList);
    }

    @Override
    public Long unfavorite(Integer articleId, Integer userId) {
        Long removed = stringRedisTemplate.opsForSet().remove(favKey(articleId), String.valueOf(userId));
        if (removed != null && removed > 0) {
            stringRedisTemplate.opsForZSet().incrementScore(HOT_KEY, String.valueOf(articleId), -HOT_FAVORITE);
        }
        return stringRedisTemplate.opsForSet().size(favKey(articleId));
    }

    @Override
    public InteractStatusVO status(Integer articleId, Integer userId) {
        Boolean liked = stringRedisTemplate.opsForSet().isMember(likeKey(articleId), String.valueOf(userId));
        Boolean favorited = stringRedisTemplate.opsForSet().isMember(favKey(articleId), String.valueOf(userId));
        Long likeCount = stringRedisTemplate.opsForSet().size(likeKey(articleId));
        Long favoriteCount = stringRedisTemplate.opsForSet().size(favKey(articleId));
        return new InteractStatusVO(
                Boolean.TRUE.equals(liked),
                Boolean.TRUE.equals(favorited),
                likeCount == null ? 0 : likeCount,
                favoriteCount == null ? 0 : favoriteCount);
    }

    @Override
    public void forward(Integer articleId, Integer fromUserId, Integer toUserId) {
        Article article = articleMapper.findById(articleId);
        if (article == null || !"已发布".equals(article.getState())) {
            throw new RuntimeException("文章不存在或未发布");
        }
        if (fromUserId.equals(toUserId)) {
            throw new RuntimeException("不能转发给自己");
        }
        // 转发目标必须是好友
        if (friendMapper.isFriend(fromUserId, toUserId) <= 0) {
            throw new RuntimeException("只能转发给好友");
        }

        // 记录转发明细（唯一索引幂等）+ 计数 + 热点加分
        interactionMapper.insertForward(articleId, fromUserId, toUserId);
        interactionMapper.incrForwardCount(articleId);
        stringRedisTemplate.opsForZSet().incrementScore(HOT_KEY, String.valueOf(articleId), HOT_FORWARD);

        // 生成 msg_type=1 的消息卡片（content 存标题，卡片图走联表）
        Message message = messageService.save(fromUserId, toUserId, 1, article.getTitle(), articleId);

        // 好友在线 → 实时推送
        try {
            chatWebSocketHandler.sendToUser(toUserId, objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            // 推送失败不阻断，离线用户走未读拉取
        }
    }

    private void checkArticle(Integer articleId) {
        Article article = articleMapper.findById(articleId);
        if (article == null || !"已发布".equals(article.getState())) {
            throw new RuntimeException("文章不存在或未发布");
        }
    }

    private String likeKey(Integer articleId) {
        return com.wangheng.article.util.InteractionRedisKeys.likeKey(articleId);
    }

    private String favKey(Integer articleId) {
        return com.wangheng.article.util.InteractionRedisKeys.favKey(articleId);
    }

    @Override
    public List<FavoriteFolderVO> myFolders(Integer userId) {
        List<FavoriteFolderVO> result = new ArrayList<>();
        FavoriteFolderVO def = new FavoriteFolderVO();
        def.setId(null);
        def.setName("默认收藏夹");
        def.setArticleCount(interactionMapper.countDefaultFolderArticles(userId));
        result.add(def);
        result.addAll(interactionMapper.listFolders(userId));
        return result;
    }

    @Override
    public void createFolder(Integer userId, String name) {
        FavoriteFolder folder = new FavoriteFolder();
        folder.setUserId(userId);
        folder.setName(name);
        try {
            favoriteFolderMapper.insert(folder);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new RuntimeException("已存在同名收藏夹");
        }
    }

    @Override
    public void renameFolder(Integer userId, Integer folderId, String name) {
        try {
            if (favoriteFolderMapper.rename(folderId, userId, name) == 0) {
                throw new RuntimeException("收藏夹不存在");
            }
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new RuntimeException("已存在同名收藏夹");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFolder(Integer userId, Integer folderId) {
        // 夹内收藏先回退默认收藏夹，再删夹
        favoriteFolderMapper.moveToDefaultFolder(folderId, userId);
        if (favoriteFolderMapper.delete(folderId, userId) == 0) {
            throw new RuntimeException("收藏夹不存在");
        }
    }
}
