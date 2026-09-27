package com.wangheng.article.mapper;

import com.wangheng.article.pojo.FavoriteFolder;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 收藏夹 Mapper（默认收藏夹为虚拟夹：folder_id 为 NULL，不占表行）。
 */
@Mapper
public interface FavoriteFolderMapper {

    @Insert("insert into favorite_folder(user_id, name, create_time) values(#{userId}, #{name}, now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(FavoriteFolder folder);

    @Select("select * from favorite_folder where id=#{id}")
    FavoriteFolder findById(Integer id);

    /** 重命名（仅本人，uk_user_name 兜底防重名） */
    @Update("update favorite_folder set name=#{name} where id=#{id} and user_id=#{userId}")
    int rename(@Param("id") Integer id, @Param("userId") Integer userId, @Param("name") String name);

    /** 删除收藏夹（仅本人） */
    @Delete("delete from favorite_folder where id=#{id} and user_id=#{userId}")
    int delete(@Param("id") Integer id, @Param("userId") Integer userId);

    /** 删除夹前：夹内收藏回退到默认收藏夹（folder_id 置 NULL） */
    @Update("update article_favorite set folder_id=null where folder_id=#{folderId} and user_id=#{userId}")
    int moveToDefaultFolder(@Param("folderId") Integer folderId, @Param("userId") Integer userId);
}
