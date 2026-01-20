-- 用户关系表
drop table user_relation;
CREATE TABLE IF NOT EXISTS `user_relation` (
                                               `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                               `follower_id` BIGINT NOT NULL COMMENT '粉丝ID（关注者）',
                                               `following_id` BIGINT NOT NULL COMMENT '被关注者ID',
                                               `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
                                               PRIMARY KEY (`id`),
                                               UNIQUE KEY `uk_follower_followee` (`follower_id`, `following_id`),
                                               KEY `idx_follower` (`follower_id`),
                                               KEY `idx_following` (`following_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关系表';
