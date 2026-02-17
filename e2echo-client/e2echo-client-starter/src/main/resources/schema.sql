-- 创建消息
CREATE TABLE IF NOT EXISTS message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    owner_public_key_hex VARCHAR(256) NOT NULL COMMENT '所属登入用户',
    create_time TIMESTAMP NOT NULL COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '版本号',
    uuid VARCHAR(64) NOT NULL COMMENT '消息 uuid',
    timestamp_ BIGINT NOT NULL COMMENT '时间戳',
    from_public_key_hex VARCHAR(256) NOT NULL COMMENT '发送者公钥',
    to_public_key_hex VARCHAR(256) NOT NULL COMMENT '接收者公钥',
    data TEXT NOT NULL COMMENT '消息数据',
    type INT NOT NULL COMMENT '消息类型',
    group_ TINYINT NOT NULL COMMENT '是否是群聊消息',
    UNIQUE (owner_public_key_hex, uuid, is_deleted)
);
-- 创建索引
CREATE INDEX IF NOT EXISTS public_key_hex_timestamp_idx ON message (
    from_public_key_hex,
    to_public_key_hex,
    timestamp_
);
-- 创建别名
CREATE TABLE IF NOT EXISTS alias (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    owner_public_key_hex VARCHAR(256) NOT NULL COMMENT '所属登入用户',
    create_time TIMESTAMP NOT NULL COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '版本号',
    public_key_hex VARCHAR(256) NOT NULL COMMENT '对方公钥或群聊UUID',
    alias VARCHAR(256) NOT NULL COMMENT '别名'
);
-- 创建索引
CREATE INDEX IF NOT EXISTS public_key_hex_idx ON alias (public_key_hex);
-- 创建会话
CREATE TABLE IF NOT EXISTS session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    owner_public_key_hex VARCHAR(256) NOT NULL COMMENT '所属登入用户',
    create_time TIMESTAMP NOT NULL COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '版本号',
    public_key_hex VARCHAR(256) NOT NULL COMMENT '对方公钥或群聊UUID',
    message_id BIGINT NULL COMMENT '最后一条消息ID',
    timestamp_ BIGINT NOT NULL COMMENT '最后消息时间戳',
    group_ TINYINT NOT NULL COMMENT '是否是群聊',
    group_key_id BIGINT NOT NULL COMMENT '群聊密钥ID',
    group_enabled TINYINT NOT NULL COMMENT '群聊是否启用'
);
-- 创建索引
CREATE INDEX IF NOT EXISTS public_key_hex_idx ON session (public_key_hex, timestamp_);
-- 创建群聊密钥
CREATE TABLE IF NOT EXISTS group_key(
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    owner_public_key_hex VARCHAR(256) NOT NULL COMMENT '所属登入用户',
    create_time TIMESTAMP NOT NULL COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '版本号',
    group_uuid VARCHAR(256) NOT NULL COMMENT '群聊 uuid',
    timestamp_ BIGINT NOT NULL COMMENT '时间戳',
    aes_key VARCHAR(256) NOT NULL COMMENT '群聊密钥'
);
-- 创建索引
CREATE INDEX IF NOT EXISTS group_uuid_timestamp_idx ON group_key (group_uuid, timestamp_);
-- 创建群聊成员
CREATE TABLE IF NOT EXISTS group_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    owner_public_key_hex VARCHAR(256) NOT NULL COMMENT '所属登入用户',
    create_time TIMESTAMP NOT NULL COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '版本号',
    group_uuid VARCHAR(256) NOT NULL COMMENT '群聊 uuid',
    public_key_hex VARCHAR(256) NOT NULL COMMENT '群成员公钥'
);
-- 创建索引
CREATE INDEX IF NOT EXISTS group_uuid_idx ON group_member (group_uuid);
-- 创建群聊共享机制
CREATE TABLE IF NOT EXISTS group_key_shared (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    owner_public_key_hex VARCHAR(256) NOT NULL COMMENT '所属登入用户',
    create_time TIMESTAMP NOT NULL COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '版本号',
    group_uuid VARCHAR(256) NOT NULL COMMENT '群聊 uuid',
    from_ VARCHAR(256) NOT NULL COMMENT '发送者公钥',
    to_ VARCHAR(256) NOT NULL COMMENT '接收者公钥',
    UNIQUE (
        owner_public_key_hex,
        group_uuid,
        from_,
        to_,
        is_deleted
    )
);
-- 创建索引
CREATE INDEX IF NOT EXISTS group_uuid_from_to_idx ON group_key_shared (group_uuid, from_, to_);
CREATE INDEX IF NOT EXISTS group_uuid_to_from_idx ON group_key_shared (group_uuid, to_, from_);