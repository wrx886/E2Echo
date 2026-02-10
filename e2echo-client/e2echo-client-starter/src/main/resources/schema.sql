-- 创建消息
CREATE TABLE IF NOT EXISTS message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    owner_public_key_hex VARCHAR(256) NOT NULL COMMENT '所属登入用户',
    create_time TIMESTAMP NOT NULL COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL COMMENT '更新时间',
    is_deleted TINYINT NOT NULL COMMENT '是否删除',
    uuid VARCHAR(64) NOT NULL UNIQUE COMMENT '消息 uuid',
    timestamp_ BIGINT NOT NULL COMMENT '时间戳',
    from_public_key_hex VARCHAR(256) NOT NULL COMMENT '发送者公钥',
    to_public_key_hex VARCHAR(256) NOT NULL COMMENT '接收者公钥',
    data TEXT NOT NULL COMMENT '消息数据',
    type INT NOT NULL COMMENT '消息类型',
    group_ TINYINT NOT NULL COMMENT '是否是群聊消息'
);
-- 创建索引
CREATE INDEX IF NOT EXISTS public_key_hex_timestamp_idx ON message (
    from_public_key_hex,
    to_public_key_hex,
    timestamp_
);