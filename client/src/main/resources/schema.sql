-- 创建文件表
CREATE TABLE IF NOT EXISTS files (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    owner_id BIGINT NOT NULL COMMENT '所属登入用户',
    message_id BIGINT NOT NULL COMMENT '消息id',
    path VARCHAR(256) NOT NULL COMMENT '文件路径'
);

-- 创建群聊表
CREATE TABLE IF NOT EXISTS group_users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    owner_id BIGINT NOT NULL COMMENT '所属登入用户',
    group_id BIGINT NOT NULL COMMENT '群聊 id',
    member_id BIGINT NOT NULL COMMENT '成员 id'
);

-- 创建登入用户表
CREATE TABLE IF NOT EXISTS login_users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    public_key VARCHAR(256) UNIQUE NOT NULL COMMENT '公钥'
);

-- 创建消息表
CREATE TABLE IF NOT EXISTS messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT NOT NULL COMMENT '自增主键',
    owner_id BIGINT NOT NULL COMMENT '所属登入用户',
    from_id BIGINT NOT NULL COMMENT '发送者',
    session_id BIGINT NOT NULL COMMENT '会话',
    data VARCHAR(4096) NOT NULL COMMENT '消息数据',
    type INT NOT NULL COMMENT '消息类型',
    uuid VARCHAR(64) UNIQUE NOT NULL COMMENT '消息 uuid',
    send_time TIMESTAMP NOT NULL COMMENT '发送时间'
);

-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    owner_id BIGINT NOT NULL COMMENT '所属登入用户',
    name VARCHAR(64) NOT NULL COMMENT '名称',
    type INT NOT NULL COMMENT '用户类型',
    public_key VARCHAR(256) NOT NULL COMMENT '公钥',
    group_uuid VARCHAR(64) COMMENT '群聊 uuid'
);