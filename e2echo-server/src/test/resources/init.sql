-- 创建数据库
DROP DATABASE IF EXISTS `e2echo_server_test`;
CREATE DATABASE IF NOT EXISTS `e2echo_server_test` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `e2echo_server_test`;
-- 创建表
CREATE TABLE IF NOT EXISTS `message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `create_time` TIMESTAMP NOT NULL COMMENT '创建时间',
  `update_time` TIMESTAMP NOT NULL COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除（0：未删除；1：已删除）',
  `uuid` VARCHAR(64) NOT NULL COMMENT '消息ID',
  `timestamp_` BIGINT NOT NULL COMMENT '时间戳',
  `from_public_key_hex` VARCHAR(256) NOT NULL COMMENT '发送方公钥',
  `to_public_key_hex` VARCHAR(256) NOT NULL COMMENT '接收方公钥',
  `data` TEXT NOT NULL COMMENT '消息内容',
  `signature` VARCHAR(256) NOT NULL COMMENT '签名',
  `group_` TINYINT NOT NULL COMMENT '是否是群聊消息',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `to_public_key_hex_timestamp` (`to_public_key_hex`, `timestamp_`) USING BTREE
)