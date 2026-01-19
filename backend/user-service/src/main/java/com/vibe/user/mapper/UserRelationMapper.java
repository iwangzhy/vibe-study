package com.vibe.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.user.entity.UserRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户关系Mapper
 */
@Mapper
public interface UserRelationMapper extends BaseMapper<UserRelation> {

    /**
     * 统计关注数
     */
    @Select("SELECT COUNT(*) FROM user_relation WHERE follower_id = #{userId}")
    Long countFollowing(@Param("userId") Long userId);

    /**
     * 统计粉丝数
     */
    @Select("SELECT COUNT(*) FROM user_relation WHERE following_id = #{userId}")
    Long countFollower(@Param("userId") Long userId);

    /**
     * 检查是否关注
     */
    @Select("SELECT COUNT(*) FROM user_relation WHERE follower_id = #{followerId} AND following_id = #{followingId}")
    int checkFollowing(@Param("followerId") Long followerId, @Param("followingId") Long followingId);
}
