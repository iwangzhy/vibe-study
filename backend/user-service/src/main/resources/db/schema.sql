-- 创建数据库
USE vibe_coding;

-- 用户表
truncate table user;
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL auto_increment COMMENT '用户ID（雪花算法生成）',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名（唯一，用于登录）',
  `password` VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
  `nickname` VARCHAR(50) NOT NULL COMMENT '昵称',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `gender` TINYINT DEFAULT 0 COMMENT '性别（0-未知，1-男，2-女）',
  `birthday` DATE DEFAULT NULL COMMENT '生日',
  `bio` VARCHAR(500) DEFAULT NULL COMMENT '个人简介',
  `status` TINYINT DEFAULT 1 COMMENT '状态（0-禁用，1-正常）',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除（0-未删除，1-已删除）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 用户关系表
CREATE TABLE IF NOT EXISTS `user_relation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `follower_id` BIGINT NOT NULL COMMENT '粉丝ID（关注者）',
  `followee_id` BIGINT NOT NULL COMMENT '被关注者ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follower_followee` (`follower_id`, `followee_id`),
  KEY `idx_follower` (`follower_id`),
  KEY `idx_followee` (`followee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关系表';

-- ========================================================================
-- 测试数据生成存储过程
-- ========================================================================

-- 生成测试用户关系数据
DROP PROCEDURE IF EXISTS generate_test_relations$$
CREATE PROCEDURE generate_test_relations(IN relation_count INT)
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE follower_id BIGINT;
    DECLARE followee_id BIGINT;
    DECLARE max_attempts INT DEFAULT 10;
    DECLARE attempt INT;
    
    WHILE i <= relation_count DO
        SET attempt = 0;
        
        -- 尝试生成不重复的关注关系
        retry_loop: WHILE attempt < max_attempts DO
            -- 随机选择两个不同的用户
            SELECT id INTO follower_id FROM `user` 
            WHERE deleted = 0 AND status = 1 
            ORDER BY RAND() LIMIT 1;
            
            SELECT id INTO followee_id FROM `user` 
            WHERE deleted = 0 AND status = 1 AND id != follower_id 
            ORDER BY RAND() LIMIT 1;
            
            -- 检查关系是否已存在
            IF NOT EXISTS (
                SELECT 1 FROM user_relation 
                WHERE follower_id = follower_id AND followee_id = followee_id
            ) THEN
                -- 插入关注关系
                INSERT INTO `user_relation` (
                    `follower_id`,
                    `followee_id`,
                    `created_at`
                ) VALUES (
                    follower_id,
                    followee_id,
                    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 180) DAY)
                );
                
                SET i = i + 1;
                LEAVE retry_loop;
            END IF;
            
            SET attempt = attempt + 1;
        END WHILE;
        
        -- 如果尝试次数过多，跳过此次循环
        IF attempt >= max_attempts THEN
            SET i = i + 1;
        END IF;
    END WHILE;
    
    SELECT CONCAT('成功生成 ', relation_count, ' 条测试用户关系') AS result;
END$$

-- 一键生成完整测试数据集
DROP PROCEDURE IF EXISTS generate_full_test_data$$
CREATE PROCEDURE generate_full_test_data(
    IN user_count INT,
    IN relation_count INT
)
BEGIN
    -- 生成用户数据
    CALL generate_test_users(user_count);
    
    -- 生成用户关系数据
    CALL generate_test_relations(relation_count);
    
    -- 统计信息
    SELECT 
        (SELECT COUNT(*) FROM `user` WHERE deleted = 0) AS total_users,
        (SELECT COUNT(*) FROM `user` WHERE deleted = 0 AND status = 1) AS active_users,
        (SELECT COUNT(*) FROM `user_relation`) AS total_relations,
        (SELECT COUNT(DISTINCT follower_id) FROM `user_relation`) AS users_with_following,
        (SELECT COUNT(DISTINCT followee_id) FROM `user_relation`) AS users_with_followers;
END$$

-- 清空所有测试数据
# DROP PROCEDURE IF EXISTS clear_test_data$$
# CREATE PROCEDURE clear_test_data()
# BEGIN
#     DELETE FROM user_relation;
#     DELETE FROM `user` WHERE username LIKE 'testuser%';
#
#     SELECT '测试数据已清空' AS result;
# END$$
#
# DELIMITER ;

-- ========================================================================
-- 使用示例
-- ========================================================================
-- 1. 生成 100 个测试用户
-- CALL generate_test_users(10011111);

-- 2. 生成 200 条用户关系
-- CALL generate_test_relations(200);

-- 3. 一键生成完整测试数据（100个用户 + 300条关系）
-- CALL generate_full_test_data(100, 300);

-- 4. 清空所有测试数据
-- CALL clear_test_data();

-- 5. 查看统计信息
-- SELECT 
--     (SELECT COUNT(*) FROM `user` WHERE deleted = 0) AS total_users,
--     (SELECT COUNT(*) FROM `user` WHERE status = 1) AS active_users,
--     (SELECT COUNT(*) FROM `user_relation`) AS total_relations;
