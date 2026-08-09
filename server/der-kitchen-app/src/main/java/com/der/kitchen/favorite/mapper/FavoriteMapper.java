package com.der.kitchen.favorite.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.der.kitchen.favorite.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    @Insert("""
            INSERT INTO biz_favorite (user_id, dish_id, create_by, update_by)
            VALUES (#{userId}, #{dishId}, CAST(#{userId} AS VARCHAR), CAST(#{userId} AS VARCHAR))
            ON CONFLICT (user_id, dish_id) WHERE deleted = FALSE DO NOTHING
            """)
    int insertIfAbsent(@Param("userId") Long userId, @Param("dishId") Long dishId);
}
