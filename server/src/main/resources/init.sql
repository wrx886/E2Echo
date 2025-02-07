-- Active: 1736477710229@@127.0.0.1@3306@e2echo

-- ----------------------------
-- Database for e2echo
-- ----------------------------
DROP DATABASE IF EXISTS e2echo;

CREATE DATABASE e2echo CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE e2echo;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `phone` VARCHAR(16) NULL DEFAULT NULL COMMENT '手机号',
    `public_key` VARCHAR(512) UNIQUE NULL DEFAULT NULL COMMENT '公钥',
    `create_time` TIMESTAMP NULL DEFAULT NULL COMMENT '创建时间',
    `update_time` TIMESTAMP NULL DEFAULT NULL COMMENT '更新时间',
    `is_deleted` TINYINT NULL DEFAULT 0 COMMENT '逻辑删除（0：未删除；1：已删除）',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户实体表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for message
-- ----------------------------
DROP TABLE IF EXISTS `message`;

CREATE TABLE `message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `from_public_key` VARCHAR(512) NULL DEFAULT NULL COMMENT '发送者公钥',
    `to_public_key` VARCHAR(512) NULL DEFAULT NULL COMMENT '接收者公钥',
    `from_user_id` BIGINT NULL DEFAULT NULL COMMENT '发送者 user_id',
    `to_user_id` BIGINT NULL DEFAULT NULL COMMENT '接收者 user_id',
    `sign` VARCHAR(512) NULL DEFAULT NULL COMMENT '数据的签名（HEX 格式）',
    `data` VARCHAR(4096) NULL DEFAULT NULL COMMENT '数据（使用接收者的公钥加密，HEX 格式）',
    `send_time` TIMESTAMP NULL DEFAULT NULL COMMENT '发送时间',
    `message_type` TINYINT NULL DEFAULT NULL COMMENT '消息类型（1：用户消息；2：群聊消息）',
    `create_time` TIMESTAMP NULL DEFAULT NULL COMMENT '创建时间',
    `update_time` TIMESTAMP NULL DEFAULT NULL COMMENT '更新时间',
    `is_deleted` TINYINT NULL DEFAULT 0 COMMENT '逻辑删除（0：未删除；1：已删除）',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户实体表' ROW_FORMAT = DYNAMIC;