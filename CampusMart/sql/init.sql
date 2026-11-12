-- ============================================================
-- CampusMart 初始化脚本
-- 由 mysqldump 风格手工导出：结构全量 + 基础数据（用户/分类/商品）
-- 不包含订单、购物车、库存流水等测试数据
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `campusmart` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `campusmart`;

DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '手机号，登录账号',
  `password` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码哈希',
  `nickname` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `avatar` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1正常 0禁用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='前台用户';

DROP TABLE IF EXISTS `t_category`;
CREATE TABLE `t_category` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `parent_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '0 表示一级分类',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `icon` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序，越小越前',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1启用 0停用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`,`status`,`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类';

DROP TABLE IF EXISTS `t_spu`;
CREATE TABLE `t_spu` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `category_id` bigint unsigned NOT NULL COMMENT '所属分类（二级分类的 id）',
  `name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品名',
  `sub_title` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '卖点/副标题',
  `main_image` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主图 URL',
  `detail` text COLLATE utf8mb4_unicode_ci COMMENT '图文详情',
  `price_low` decimal(10,2) NOT NULL COMMENT '最低 SKU 价，列表页展示"起"',
  `sales` int NOT NULL DEFAULT '0' COMMENT '累计销量',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1上架 0下架',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category_status` (`category_id`,`status`),
  KEY `idx_sales` (`sales` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品 SPU';

DROP TABLE IF EXISTS `t_sku`;
CREATE TABLE `t_sku` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `spu_id` bigint unsigned NOT NULL COMMENT '所属 SPU',
  `sku_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规格描述，如 黑色-iPhone15',
  `price` decimal(10,2) NOT NULL,
  `stock` int NOT NULL DEFAULT '0',
  `image` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '该规格自己的图，可为空',
  `status` tinyint NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_spu` (`spu_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品 SKU';

DROP TABLE IF EXISTS `t_cart`;
CREATE TABLE `t_cart` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `sku_id` bigint unsigned NOT NULL,
  `quantity` int NOT NULL DEFAULT '1',
  `selected` tinyint NOT NULL DEFAULT '1' COMMENT '1勾选 0未勾选',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_sku` (`user_id`,`sku_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车';

DROP TABLE IF EXISTS `t_order`;
CREATE TABLE `t_order` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `order_no` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单号，对外展示',
  `user_id` bigint unsigned NOT NULL,
  `total_amount` decimal(10,2) NOT NULL COMMENT '商品总额',
  `pay_amount` decimal(10,2) NOT NULL COMMENT '应付金额（扣优惠后）',
  `status` tinyint NOT NULL DEFAULT '10' COMMENT '10待支付 20已支付 30已发货 40已完成 50已取消 60已关闭',
  `receiver_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '收货人',
  `receiver_phone` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '联系电话',
  `receiver_address` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '详细地址',
  `expire_time` datetime NOT NULL COMMENT '支付截止时间',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `close_time` datetime DEFAULT NULL COMMENT '关闭时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_status` (`user_id`,`status`),
  KEY `idx_expire` (`status`,`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单主表';

DROP TABLE IF EXISTS `t_order_item`;
CREATE TABLE `t_order_item` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `order_id` bigint unsigned NOT NULL,
  `spu_id` bigint unsigned NOT NULL,
  `sku_id` bigint unsigned NOT NULL,
  `spu_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '下单时的商品名',
  `sku_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '下单时的规格名',
  `image` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '下单时的图片',
  `price` decimal(10,2) NOT NULL COMMENT '下单时的单价',
  `quantity` int NOT NULL,
  `total_amount` decimal(10,2) NOT NULL COMMENT '小计 = price * quantity',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单明细';

DROP TABLE IF EXISTS `t_stock_log`;
CREATE TABLE `t_stock_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sku_id` bigint NOT NULL COMMENT 'SKU id',
  `biz_type` tinyint NOT NULL COMMENT '1=下单扣减 2=归还',
  `biz_no` varchar(32) NOT NULL COMMENT '业务单号，即订单号',
  `change_num` int NOT NULL COMMENT '变动量：扣减为负，归还为正',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz` (`biz_no`,`biz_type`,`sku_id`),
  KEY `idx_sku` (`sku_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存变动流水';

DROP TABLE IF EXISTS `t_address`;
CREATE TABLE `t_address` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `receiver_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `receiver_phone` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `province` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `city` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `district` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `detail` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_default` tinyint NOT NULL DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`,`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收货地址';

-- ---------- t_user 初始数据（3 行）----------
TRUNCATE TABLE `t_user`;
INSERT INTO `t_user` (`id`, `phone`, `password`, `nickname`, `avatar`, `status`, `created_at`, `updated_at`) VALUES (1, '13800000000', 'test', '测试用户', NULL, 1, '2026-10-20 19:25:22', '2026-10-20 19:25:22');
INSERT INTO `t_user` (`id`, `phone`, `password`, `nickname`, `avatar`, `status`, `created_at`, `updated_at`) VALUES (4, '13800138005', '123456', '小明', NULL, 1, '2026-10-21 15:29:04', '2026-10-21 15:29:04');
INSERT INTO `t_user` (`id`, `phone`, `password`, `nickname`, `avatar`, `status`, `created_at`, `updated_at`) VALUES (5, '13800138007', '123456', '用户8007', NULL, 1, '2026-10-21 15:29:26', '2026-10-21 15:29:26');

-- ---------- t_category 初始数据（22 行）----------
TRUNCATE TABLE `t_category`;
INSERT INTO `t_category` (`id`, `parent_id`, `name`, `icon`, `sort`, `status`, `created_at`) VALUES (1, 0, '零食饮业', NULL, 1, 1, '2026-10-22 15:58:17');
INSERT INTO `t_category` (`id`, `parent_id`, `name`, `icon`, `sort`, `status`, `created_at`) VALUES (2, 0, '文具办公', NULL, 2, 1, '2026-10-22 15:58:17');
INSERT INTO `t_category` (`id`, `parent_id`, `name`, `icon`, `sort`, `status`, `created_at`) VALUES (3, 0, '数码配件', NULL, 3, 1, '2026-10-22 15:58:17');
INSERT INTO `t_category` (`id`, `parent_id`, `name`, `icon`, `sort`, `status`, `created_at`) VALUES (4, 1, '薯片饼干', NULL, 1, 1, '2026-10-22 15:58:17');
INSERT INTO `t_category` (`id`, `parent_id`, `name`, `icon`, `sort`, `status`, `created_at`) VALUES (5, 1, '饮料', NULL, 2, 1, '2026-10-22 15:58:17');
INSERT INTO `t_category` (`id`, `parent_id`, `name`, `icon`, `sort`, `status`, `created_at`) VALUES (6, 2, '笔记本', NULL, 1, 1, '2026-10-22 15:58:17');
INSERT INTO `t_category` (`id`, `parent_id`, `name`, `icon`, `sort`, `status`, `created_at`) VALUES (7, 2, '书写工具', NULL, 2, 1, '2026-10-22 15:58:17');
INSERT INTO `t_category` (`id`, `parent_id`, `name`, `icon`, `sort`, `status`, `created_at`) VALUES (8, 3, '手机壳', NULL, 1, 1, '2026-10-22 15:58:17');
INSERT INTO `t_category` (`id`, `parent_id`, `name`, `icon`, `sort`, `status`, `created_at`) VALUES (9, 3, '数据线', NULL, 2, 1, '2026-10-22 15:58:17');
INSERT INTO `t_category` (`id`, `parent_id`, `name`, `icon`, `sort`, `status`, `created_at`) VALUES (10, 0, '生活日用', NULL, 4, 1, '2026-11-09 09:35:00');
INSERT INTO `t_category` (`id`, `parent_id`, `name`, `icon`, `sort`, `status`, `created_at`) VALUES (11, 0, '图书教材', NULL, 5, 1, '2026-11-09 09:35:00');
INSERT INTO `t_category` (`id`, `parent_id`, `name`, `icon`, `sort`, `status`, `created_at`) VALUES (12, 0, '运动户外', NULL, 6, 1, '2026-11-09 09:35:00');
INSERT INTO `t_category` (`id`, `parent_id`, `name`, `icon`, `sort`, `status`, `created_at`) VALUES (13, 1, '泡面速食', NULL, 3, 1, '2026-11-09 09:35:00');
INSERT INTO `t_category` (`id`, `parent_id`, `name`, `icon`, `sort`, `status`, `created_at`) VALUES (14, 1, '糖果巧克力', NULL, 4, 1, '2026-11-09 09:35:00');
INSERT INTO `t_category` (`id`, `parent_id`, `name`, `icon`, `sort`, `status`, `created_at`) VALUES (15, 2, '文件夹收纳', NULL, 3, 1, '2026-11-09 09:35:00');
INSERT INTO `t_category` (`id`, `parent_id`, `name`, `icon`, `sort`, `status`, `created_at`) VALUES (16, 3, '耳机音箱', NULL, 3, 1, '2026-11-09 09:35:00');
INSERT INTO `t_category` (`id`, `parent_id`, `name`, `icon`, `sort`, `status`, `created_at`) VALUES (17, 3, '充电宝', NULL, 4, 1, '2026-11-09 09:35:00');
INSERT INTO `t_category` (`id`, `parent_id`, `name`, `icon`, `sort`, `status`, `created_at`) VALUES (18, 10, '纸品湿巾', NULL, 1, 1, '2026-11-09 09:35:00');
INSERT INTO `t_category` (`id`, `parent_id`, `name`, `icon`, `sort`, `status`, `created_at`) VALUES (19, 10, '洗漱用品', NULL, 2, 1, '2026-11-09 09:35:00');
INSERT INTO `t_category` (`id`, `parent_id`, `name`, `icon`, `sort`, `status`, `created_at`) VALUES (20, 11, '专业教材', NULL, 1, 1, '2026-11-09 09:35:00');
INSERT INTO `t_category` (`id`, `parent_id`, `name`, `icon`, `sort`, `status`, `created_at`) VALUES (21, 11, '课外读物', NULL, 2, 1, '2026-11-09 09:35:00');
INSERT INTO `t_category` (`id`, `parent_id`, `name`, `icon`, `sort`, `status`, `created_at`) VALUES (22, 12, '运动配件', NULL, 1, 1, '2026-11-09 09:35:00');

-- ---------- t_spu 初始数据（36 行）----------
TRUNCATE TABLE `t_spu`;
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (1, 4, '原味薯片', '香脆可口，办公室必备零食', 'https://picsum.photos/seed/spu1/400/400', '<p>净含量 100g</p>', 5.90, 328, 1, '2026-10-23 13:49:25', '2026-11-04 16:22:46');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (2, 4, '番茄味薯片', '酸甜开胃，年轻人的最爱', 'https://picsum.photos/seed/spu2/400/400', '<p>净含量 100g</p>', 9.90, 156, 1, '2026-10-23 13:49:25', '2026-11-04 16:22:46');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (3, 5, '冰红茶', '冰爽解腻，夏天的快乐水', 'https://picsum.photos/seed/spu3/400/400', '<p>500ml 塑料瓶装</p>', 3.50, 892, 1, '2026-10-23 13:49:25', '2026-11-04 16:22:46');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (4, 6, '线圈笔记本', 'A5 大小，100 页加厚纸张', 'https://picsum.photos/seed/spu4/400/400', '<p>横线内页，可平摊</p>', 12.80, 45, 1, '2026-10-23 13:49:25', '2026-11-04 16:22:46');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (5, 8, '磨砂手机保护壳', '防摔抗指纹，四角气囊设计', 'https://picsum.photos/seed/spu5/400/400', '<p>TPU 材质，磨砂手感</p>', 19.90, 210, 1, '2026-10-23 13:49:25', '2026-11-04 16:22:46');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (6, 5, '可口可乐 330ml', '经典口味 冰镇更好喝', 'https://picsum.photos/seed/spu6/600/600', '经典原味碳酸饮料，330ml 罐装。宿舍常备，解腻解渴。', 3.50, 1203, 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (7, 5, '农夫山泉 550ml', '天然弱碱性水 整箱更划算', 'https://picsum.photos/seed/spu7/600/600', '取自优质水源，天然弱碱性，550ml 瓶装。', 2.00, 876, 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (8, 13, '红烧牛肉面', '经典口味 5 连包', 'https://picsum.photos/seed/spu8/600/600', '国民泡面，红烧牛肉味，宿舍夜宵首选。', 2.80, 654, 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (9, 13, '老坛酸菜牛肉面', '酸爽开胃 5 连包', 'https://picsum.photos/seed/spu9/600/600', '老坛酸菜配牛肉，酸爽开胃，冲泡 3 分钟即食。', 13.90, 432, 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (10, 13, '自热米饭 香菇滑鸡', '免明火 15 分钟即食', 'https://picsum.photos/seed/spu10/600/600', '自带加热包，加冷水 15 分钟即可食用，无需明火。', 16.80, 289, 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (11, 14, '德芙丝滑牛奶巧克力', '252g 分享装', 'https://picsum.photos/seed/spu11/600/600', '经典牛奶巧克力，丝滑口感，独立小包装。', 8.50, 512, 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (12, 14, '士力架花生夹心', '横扫饥饿 做回自己', 'https://picsum.photos/seed/spu12/600/600', '花生焦糖夹心巧克力，补充能量。', 4.50, 398, 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (13, 6, '康奈尔笔记本 A5', '5R 笔记法 高效学习', 'https://picsum.photos/seed/spu13/600/600', '康奈尔版式内页，左侧提示栏、右侧笔记区、底部总结区。', 15.80, 231, 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (14, 6, '皮质手账本', '复古皮质 可替换内芯', 'https://picsum.photos/seed/spu14/600/600', 'PU 皮质封面，活页内芯可替换，附赠贴纸和笔插。', 28.00, 167, 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (15, 7, '晨光中性笔 0.5mm', '黑色 10 支装', 'https://picsum.photos/seed/spu15/600/600', '0.5mm 子弹头，顺滑速干，学生考试常用。', 12.90, 1520, 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (16, 7, '斑马荧光笔 5 色', '柔和色系 不透纸', 'https://picsum.photos/seed/spu16/600/600', '5 色柔和荧光笔，划重点不刺眼，不透纸背。', 18.50, 489, 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (17, 7, '2B 铅笔套装', '考试专用 附削笔器', 'https://picsum.photos/seed/spu17/600/600', '12 支 2B 涂卡铅笔，附赠卷笔刀和橡皮。', 9.90, 762, 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (18, 15, 'A4 拉杆文件夹', '透明磨砂 可装 60 页', 'https://picsum.photos/seed/spu18/600/600', 'A4 规格拉杆夹，防水耐磨，适合资料整理。', 6.80, 208, 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (19, 15, '试卷收纳袋', '13 层分类 期末整理神器', 'https://picsum.photos/seed/spu19/600/600', '13 层插页设计，按科目分类存放试卷。', 21.90, 345, 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (20, 8, '透明硅胶防摔壳', '磨砂手感 不发黄', 'https://picsum.photos/seed/spu20/600/600', 'TPU 软胶材质，四角加厚防摔，长期使用不发黄。', 16.90, 623, 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (21, 9, 'Type-C 快充线 1m', '66W 快充 编织线身', 'https://picsum.photos/seed/spu21/600/600', '支持 66W 超级快充，尼龙编织线身抗折断。', 15.90, 891, 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (22, 9, '三合一数据线', '一根搞定三种接口', 'https://picsum.photos/seed/spu22/600/600', 'Type-C / Lightning / Micro USB 三合一，出差旅行一根够用。', 24.90, 267, 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (23, 16, '有线入耳耳机', '带麦通话 3.5mm', 'https://picsum.photos/seed/spu23/600/600', '入耳式设计，带麦克风支持通话，3.5mm 接口。', 29.90, 432, 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (24, 16, '蓝牙耳机', '半入耳式 续航 24 小时', 'https://picsum.photos/seed/spu24/600/600', '蓝牙 5.3，半入耳设计久戴不痛，配合充电仓续航 24 小时。', 89.00, 512, 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (25, 17, '10000mAh 充电宝', '轻薄便携 双向快充', 'https://picsum.photos/seed/spu25/600/600', '10000mAh 容量，支持 22.5W 快充，厚度仅 14mm。', 79.00, 378, 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (26, 17, '20000mAh 快充充电宝', '大容量 可充笔记本', 'https://picsum.photos/seed/spu26/600/600', '20000mAh 大容量，65W 输出可给轻薄本充电，带数显。', 159.00, 203, 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (27, 18, '抽纸 3 层 130 抽', '整箱 6 包装', 'https://picsum.photos/seed/spu27/600/600', '3 层加厚原生木浆，柔韧不掉屑，6 包整箱装。', 26.90, 1102, 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (28, 18, '湿巾 80 抽', '洁净护肤 便携装', 'https://picsum.photos/seed/spu28/600/600', '温和配方，含保湿成分，80 抽抽取式。', 12.90, 543, 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (29, 19, '牙刷牙膏套装', '软毛护龈 旅行装', 'https://picsum.photos/seed/spu29/600/600', '软毛牙刷 + 含氟牙膏，附旅行收纳盒。', 19.90, 421, 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (30, 19, '纯棉毛巾', '34x75cm 吸水快干', 'https://picsum.photos/seed/spu30/600/600', '100% 纯棉，吸水性强，快干不掉毛。', 15.90, 367, 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (31, 20, '数据结构与算法分析', 'C 语言描述 第 2 版', 'https://picsum.photos/seed/spu31/600/600', '经典教材，讲解线性表、树、图及常用算法与复杂度分析。', 59.00, 156, 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (32, 20, '计算机网络 第 7 版', '谢希仁 经典教材', 'https://picsum.photos/seed/spu32/600/600', '国内高校广泛采用的计算机网络教材，配套习题解答。', 49.00, 203, 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (33, 21, '三体 全集', '刘慈欣 科幻巨作', 'https://picsum.photos/seed/spu33/600/600', '地球往事三部曲全集，科幻文学里程碑作品。', 88.00, 876, 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (34, 21, '活着', '余华 经典小说', 'https://picsum.photos/seed/spu34/600/600', '讲述一个人一生的苦难与坚韧，当代文学经典。', 28.00, 654, 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (35, 22, '跳绳', '钢丝绳 可调长度', 'https://picsum.photos/seed/spu35/600/600', '钢丝芯跳绳，长度可调，轴承转动顺滑。', 19.90, 512, 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_spu` (`id`, `category_id`, `name`, `sub_title`, `main_image`, `detail`, `price_low`, `sales`, `status`, `created_at`, `updated_at`) VALUES (36, 22, '运动水壶 750ml', '大容量 便携提手', 'https://picsum.photos/seed/spu36/600/600', '750ml 大容量，食品级材质，带提手和刻度。', 26.90, 398, 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');

-- ---------- t_sku 初始数据（60 行）----------
TRUNCATE TABLE `t_sku`;
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (1, 1, '小包 50g', 5.90, 199, NULL, 1, '2026-10-23 13:49:25', '2026-11-04 20:08:49');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (2, 1, '大包 100g', 9.90, 150, NULL, 1, '2026-10-23 13:49:25', '2026-10-23 13:49:25');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (3, 2, '标准装 100g', 9.90, 78, NULL, 1, '2026-10-23 13:49:25', '2026-11-04 20:08:49');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (4, 3, '500ml', 3.50, 500, NULL, 1, '2026-10-23 13:49:25', '2026-10-23 13:49:25');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (5, 3, '1L', 6.00, 300, NULL, 1, '2026-10-23 13:49:25', '2026-10-23 13:49:25');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (6, 3, '整箱 12 瓶', 39.90, 20, NULL, 1, '2026-10-23 13:49:25', '2026-10-23 13:49:25');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (7, 4, '横线款', 12.80, 55, NULL, 1, '2026-10-23 13:49:25', '2026-11-04 20:05:22');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (8, 4, '方格款', 12.80, 200, NULL, 1, '2026-10-23 13:49:25', '2026-11-09 09:35:08');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (9, 5, '黑色-iPhone 15', 19.90, 94, NULL, 1, '2026-10-23 13:49:25', '2026-11-09 09:41:40');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (10, 5, '黑色-iPhone 15 Pro', 22.90, 50, NULL, 1, '2026-10-23 13:49:25', '2026-10-23 13:49:25');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (11, 5, '白色-iPhone 15', 19.90, 200, NULL, 1, '2026-10-23 13:49:25', '2026-11-09 09:35:08');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (12, 5, '白色-iPhone 15 Pro', 22.90, 30, NULL, 1, '2026-10-23 13:49:25', '2026-10-23 13:49:25');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (13, 6, '单罐装', 3.50, 500, 'https://picsum.photos/seed/sku6/600/600', 1, '2026-11-09 09:35:00', '2026-11-10 16:16:12');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (14, 6, '6 罐装', 19.90, 200, 'https://picsum.photos/seed/sku6/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (15, 7, '单瓶', 2.00, 800, 'https://picsum.photos/seed/sku7/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (16, 7, '整箱 24 瓶', 42.00, 120, 'https://picsum.photos/seed/sku7/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (17, 8, '5 连包', 12.50, 300, 'https://picsum.photos/seed/sku8/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (18, 8, '单包', 2.80, 500, 'https://picsum.photos/seed/sku8/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (19, 9, '5 连包', 13.90, 260, 'https://picsum.photos/seed/sku9/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (20, 10, '香菇滑鸡', 16.80, 150, 'https://picsum.photos/seed/sku10/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (21, 10, '梅菜扣肉', 17.80, 130, 'https://picsum.photos/seed/sku10/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (22, 11, '252g 分享装', 39.90, 180, 'https://picsum.photos/seed/sku11/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (23, 11, '43g 单条', 8.50, 300, 'https://picsum.photos/seed/sku11/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (24, 12, '35g 单条', 4.50, 400, 'https://picsum.photos/seed/sku12/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (25, 12, '6 条装', 25.90, 150, 'https://picsum.photos/seed/sku12/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (26, 13, 'A5 80 页', 15.80, 200, 'https://picsum.photos/seed/sku13/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (27, 13, 'B5 120 页', 22.80, 160, 'https://picsum.photos/seed/sku13/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (28, 14, 'A6 棕色', 28.00, 100, 'https://picsum.photos/seed/sku14/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (29, 14, 'A6 墨绿', 28.00, 90, 'https://picsum.photos/seed/sku14/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (30, 15, '黑色 10 支', 12.90, 400, 'https://picsum.photos/seed/sku15/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (31, 15, '红色 10 支', 12.90, 200, 'https://picsum.photos/seed/sku15/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (32, 16, '5 色套装', 18.50, 250, 'https://picsum.photos/seed/sku16/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (33, 17, '12 支装', 9.90, 350, 'https://picsum.photos/seed/sku17/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (34, 18, '单个装', 6.80, 300, 'https://picsum.photos/seed/sku18/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (35, 18, '5 个装', 29.90, 120, 'https://picsum.photos/seed/sku18/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (36, 19, '13 层 A4', 21.90, 180, 'https://picsum.photos/seed/sku19/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (37, 20, 'iPhone 15', 16.90, 150, 'https://picsum.photos/seed/sku20/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (38, 20, '小米 14', 16.90, 130, 'https://picsum.photos/seed/sku20/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (39, 21, '1m', 15.90, 400, 'https://picsum.photos/seed/sku21/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (40, 21, '2m', 19.90, 250, 'https://picsum.photos/seed/sku21/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (41, 22, '1.2m', 24.90, 200, 'https://picsum.photos/seed/sku22/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (42, 23, '3.5mm 黑色', 29.90, 199, 'https://picsum.photos/seed/sku23/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:41:40');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (43, 23, 'Type-C 黑色', 32.90, 150, 'https://picsum.photos/seed/sku23/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (44, 24, '白色', 89.00, 120, 'https://picsum.photos/seed/sku24/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (45, 24, '黑色', 89.00, 110, 'https://picsum.photos/seed/sku24/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (46, 25, '白色', 79.00, 150, 'https://picsum.photos/seed/sku25/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (47, 25, '黑色', 79.00, 140, 'https://picsum.photos/seed/sku25/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (48, 26, '深空灰', 159.00, 68, 'https://picsum.photos/seed/sku26/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:41:40');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (49, 27, '6 包装', 26.90, 300, 'https://picsum.photos/seed/sku27/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (50, 28, '80 抽', 12.90, 260, 'https://picsum.photos/seed/sku28/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (51, 29, '套装', 19.90, 200, 'https://picsum.photos/seed/sku29/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (52, 30, '单条', 15.90, 220, 'https://picsum.photos/seed/sku30/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (53, 30, '2 条装', 28.90, 130, 'https://picsum.photos/seed/sku30/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (54, 31, '平装', 59.00, 80, 'https://picsum.photos/seed/sku31/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (55, 32, '平装', 49.00, 90, 'https://picsum.photos/seed/sku32/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (56, 33, '全 3 册', 88.00, 100, 'https://picsum.photos/seed/sku33/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (57, 34, '平装', 28.00, 120, 'https://picsum.photos/seed/sku34/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (58, 35, '标准款', 19.90, 200, 'https://picsum.photos/seed/sku35/600/600', 1, '2026-11-09 09:35:00', '2026-11-10 16:46:59');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (59, 36, '750ml 蓝色', 26.90, 139, 'https://picsum.photos/seed/sku36/600/600', 1, '2026-11-09 09:35:00', '2026-11-12 16:36:29');
INSERT INTO `t_sku` (`id`, `spu_id`, `sku_name`, `price`, `stock`, `image`, `status`, `created_at`, `updated_at`) VALUES (60, 36, '750ml 灰色', 26.90, 140, 'https://picsum.photos/seed/sku36/600/600', 1, '2026-11-09 09:35:00', '2026-11-09 09:35:00');

SET FOREIGN_KEY_CHECKS = 1;
