
DELIMITER $$

-- 生成测试用户数据
DROP PROCEDURE IF EXISTS generate_test_users$$
CREATE PROCEDURE generate_test_users(IN user_count INT)
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE user_id BIGINT;
    DECLARE random_gender TINYINT;
    DECLARE random_status TINYINT;

    -- 清空现有测试数据（可选，谨慎使用）
    -- DELETE FROM user_relation;
    -- DELETE FROM user WHERE id >= 1000000000000000000;

    WHILE i <= user_count DO
            -- 生成雪花算法模拟的用户ID（使用时间戳+随机数）
            SET user_id = CONCAT(
                    DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'),
                    LPAD(FLOOR(RAND() * 10000), 5, '0')
                          );

            -- 随机性别（0-未知，1-男，2-女）
            SET random_gender = FLOOR(RAND() * 3);

            -- 随机状态（90%正常，10%禁用）
            SET random_status = IF(RAND() < 0.9, 1, 0);

            -- 插入用户数据
            INSERT INTO `user` (
                `id`,
                `username`,
                `password`,
                `nickname`,
                `avatar`,
                `email`,
                `phone`,
                `gender`,
                `birthday`,
                `bio`,
                `status`,
                `deleted`,
                `created_at`,
                `updated_at`
            ) VALUES (
                         user_id,
                         CONCAT('testuser', user_id),
                         -- BCrypt加密后的 "123456"（实际应用中需要后端加密）
                         '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5E',
                         CONCAT('测试用户', i),
                         CONCAT('https://api.dicebear.com/7.x/avataaars/svg?seed=', i),
                         CONCAT('testuser', user_id, '@vibe.com'),
                         CONCAT('1', LPAD(FLOOR(RAND() * 10000000000), 10, '0')),
                         random_gender,
                         DATE_SUB(CURDATE(), INTERVAL FLOOR(18 + RAND() * 42) YEAR),
                         CONCAT('这是测试用户', i, '的个人简介，热爱编程和技术分享！'),
                         random_status,
                         0,
                         DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY),
                         NOW()
                     );

            SET i = i + 1;
        END WHILE;

    SELECT CONCAT('成功生成 ', user_count, ' 个测试用户') AS result;
END$$

# truncate table user;
CALL generate_test_users(1000);