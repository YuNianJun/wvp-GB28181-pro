-- ----------------------------
-- 分屏监控预案（按用户区分，每个用户可保存多套分屏方案，其中一套为默认）
-- 依赖表：wvp_device_channel（通道信息）、wvp_user（用户信息）
-- 适用数据库：MySQL 5.7 / 8.0
-- 创建时间：2026-11-25
-- ----------------------------

-- 说明：使用 create table if not exists， 重复执行不会清空已有预案数据

-- 分屏监控预案主表
create table if not exists wvp_play_preset
(
    id          int         not null auto_increment comment '主键ID',
    user_id     int         not null comment '所属用户ID',
    name        varchar(50) not null comment '预案名称',
    split_num   int         not null default 4 comment '分屏数， 支持1/4/6/9',
    is_default  tinyint     not null default 0 comment '是否默认预案：0否 1是',
    create_time varchar(50) default null comment '创建时间',
    update_time varchar(50) default null comment '更新时间',
    primary key (id),
    unique key uk_play_preset_user_name (user_id, name),
    key idx_play_preset_user (user_id)
) engine = InnoDB
  default charset = utf8mb4 comment ='分屏监控预案';

-- 分屏监控预案明细表， 一行代表一个分屏格子
create table if not exists wvp_play_preset_item
(
    id         int not null auto_increment comment '主键ID',
    preset_id  int not null comment '所属预案ID',
    channel_id int not null comment '通道ID， 对应 wvp_device_channel.id',
    `sort`     int not null comment '分屏序号， 从1开始',
    primary key (id),
    unique key uk_play_preset_item_sort (preset_id, `sort`),
    key idx_play_preset_item_channel (channel_id)
) engine = InnoDB
  default charset = utf8mb4 comment ='分屏监控预案明细';
