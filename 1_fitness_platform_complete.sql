-- ================================================================
-- 健身平台完整数据库SQL脚本（项目唯一版本）
-- 包含：基础功能模块 + 教练业务优化模块 + 健康科普文章模块 + 完整测试数据
-- 版本：Spring Boot 3.0.5 + MyBatis-Plus 3.5.3.1
-- 日期：2025-01-24（最新更新：2025-10-06）
-- 
-- 🚨 重要说明：
-- 1. 这是项目中唯一的SQL文件，包含完整的表结构和测试数据
-- 2. 执行前请备份现有数据库！
-- 3. 本脚本会删除并重建所有表，请谨慎使用
-- 4. 包含接口测试所需的完整用户和角色数据
-- 
-- 🎯 功能特性：
-- 1. 完全修复了所有 BaseEntity 字段不匹配问题（包括 last_login_time 字段）
-- 2. 为所有继承 BaseEntity 的表添加了 update_time 字段
-- 3. 完全兼容 MyBatis-Plus @TableLogic 和 @FieldFill 功能
-- 4. 保证所有表结构与实体类定义完全一致
-- 5. 包含完整测试数据，支持Admin和App端接口测试
-- 6. 提供测试账号：admin/testuser/coach001（密码：123456）
-- ================================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `fitness_platform` 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE `fitness_platform`;

-- ================================================================
-- 第一部分：基础功能模块（已集成逻辑删除字段）
-- ================================================================

-- 1. 用户表
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
  `gender` tinyint DEFAULT NULL COMMENT '性别：0-未知，1-男，2-女',
  `birth_date` date DEFAULT NULL COMMENT '出生日期',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-禁用，1-正常',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `points` int DEFAULT '0' COMMENT '积分',
  `vip_level` tinyint DEFAULT '0' COMMENT 'VIP等级',
  `bio` text COMMENT '个人简介',
  `address` varchar(200) DEFAULT NULL COMMENT '地址',
  `occupation` varchar(100) DEFAULT NULL COMMENT '职业',
  `height` decimal(5,2) DEFAULT NULL COMMENT '身高(cm)',
  `weight` decimal(5,2) DEFAULT NULL COMMENT '体重(kg)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 角色表
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) NOT NULL COMMENT '角色编码',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-禁用，1-正常',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 3. 用户角色关联表（支持逻辑删除的唯一约束）
DROP TABLE IF EXISTS `user_role`;
CREATE TABLE `user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  -- 支持逻辑删除的唯一约束：只有is_deleted=0时才检查唯一性
  UNIQUE KEY `uk_user_role_active` (`user_id`,`role_id`,`is_deleted`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 4. 教练信息表
DROP TABLE IF EXISTS `coach`;
CREATE TABLE `coach` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '教练ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `real_name` varchar(50) NOT NULL COMMENT '真实姓名',
  `certification_no` varchar(50) DEFAULT NULL COMMENT '认证编号',
  `specialties` varchar(500) DEFAULT NULL COMMENT '专长领域',
  `introduction` text COMMENT '个人介绍',
  `experience_years` int DEFAULT '0' COMMENT '从业年限',
  `rating` decimal(3,2) DEFAULT '0.00' COMMENT '评分',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-离职，1-在职，2-休假',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教练信息表';

-- 5. 健康档案表
DROP TABLE IF EXISTS `health_record`;
CREATE TABLE `health_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '档案ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `height` decimal(5,2) DEFAULT NULL COMMENT '身高(cm)',
  `weight` decimal(5,2) DEFAULT NULL COMMENT '体重(kg)',
  `bmi` decimal(4,2) DEFAULT NULL COMMENT 'BMI指数',
  `body_fat_rate` decimal(4,2) DEFAULT NULL COMMENT '体脂率(%)',
  `muscle_rate` decimal(4,2) DEFAULT NULL COMMENT '肌肉率(%)',
  `basal_metabolism` int DEFAULT NULL COMMENT '基础代谢率',
  `health_goal` varchar(200) DEFAULT NULL COMMENT '健康目标',
  `medical_history` text COMMENT '病史',
  `allergies` varchar(500) DEFAULT NULL COMMENT '过敏信息',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康档案表';

-- 6. 体测记录表
DROP TABLE IF EXISTS `body_measurement`;
CREATE TABLE `body_measurement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `measure_date` date NOT NULL COMMENT '测量日期',
  `height` decimal(5,2) DEFAULT NULL COMMENT '身高(cm)',
  `weight` decimal(5,2) DEFAULT NULL COMMENT '体重(kg)',
  `bmi` decimal(4,2) DEFAULT NULL COMMENT 'BMI指数',
  `body_fat_rate` decimal(4,2) DEFAULT NULL COMMENT '体脂率(%)',
  `muscle_mass` decimal(5,2) DEFAULT NULL COMMENT '肌肉量(kg)',
  `visceral_fat` int DEFAULT NULL COMMENT '内脏脂肪等级',
  `waist` decimal(5,2) DEFAULT NULL COMMENT '腰围(cm)',
  `hip` decimal(5,2) DEFAULT NULL COMMENT '臀围(cm)',
  `chest` decimal(5,2) DEFAULT NULL COMMENT '胸围(cm)',
  `arm` decimal(5,2) DEFAULT NULL COMMENT '臂围(cm)',
  `thigh` decimal(5,2) DEFAULT NULL COMMENT '大腿围(cm)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_user_id_date` (`user_id`,`measure_date`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='体测记录表';

-- 7. 门店管理表
DROP TABLE IF EXISTS `gym_store`;
CREATE TABLE `gym_store` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '门店ID',
  `store_name` varchar(100) NOT NULL COMMENT '门店名称',
  `store_code` varchar(50) NOT NULL COMMENT '门店编码',
  `address` varchar(500) NOT NULL COMMENT '门店地址',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `business_hours` varchar(100) DEFAULT NULL COMMENT '营业时间',
  `manager_name` varchar(50) DEFAULT NULL COMMENT '店长姓名',
  `manager_phone` varchar(20) DEFAULT NULL COMMENT '店长电话',
  `area_size` int DEFAULT NULL COMMENT '营业面积(平米)',
  `equipment_count` int DEFAULT NULL COMMENT '设备数量',
  `max_capacity` int DEFAULT NULL COMMENT '最大容纳人数',
  `parking_spaces` int DEFAULT NULL COMMENT '停车位数量',
  `facilities` json DEFAULT NULL COMMENT '设施信息JSON',
  `description` text COMMENT '门店描述',
  `images` json DEFAULT NULL COMMENT '门店图片JSON',
  `latitude` decimal(10,7) DEFAULT NULL COMMENT '纬度',
  `longitude` decimal(10,7) DEFAULT NULL COMMENT '经度',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-停业，1-营业，2-装修中',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store_code` (`store_code`),
  KEY `idx_status` (`status`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店管理表';

-- 8. 教练门店关联表
DROP TABLE IF EXISTS `coach_store_relation`;
CREATE TABLE `coach_store_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `coach_id` bigint NOT NULL COMMENT '教练ID',
  `store_id` bigint NOT NULL COMMENT '门店ID',
  `is_primary` tinyint DEFAULT '0' COMMENT '是否主要工作门店：0-否，1-是',
  `start_date` date DEFAULT NULL COMMENT '开始工作日期',
  `end_date` date DEFAULT NULL COMMENT '结束工作日期',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-停用，1-启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coach_store_active` (`coach_id`,`store_id`,`is_deleted`),
  KEY `idx_coach_id` (`coach_id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_is_primary` (`is_primary`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教练门店关联表';

-- 9. 课程分类表
DROP TABLE IF EXISTS `course_category`;
CREATE TABLE `course_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `category_name` varchar(50) NOT NULL COMMENT '分类名称',
  `parent_id` bigint DEFAULT '0' COMMENT '父分类ID',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-禁用，1-正常',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程分类表';

-- 10. 课程表
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '课程ID',
  `course_name` varchar(100) NOT NULL COMMENT '课程名称',
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `coach_id` bigint NOT NULL COMMENT '教练ID',
  `store_id` bigint DEFAULT NULL COMMENT '默认门店ID（可为空，表示多门店课程）',
  `cover_image` varchar(255) DEFAULT NULL COMMENT '封面图片',
  `description` text COMMENT '课程描述',
  `difficulty` tinyint DEFAULT '1' COMMENT '难度：1-初级，2-中级，3-高级',
  `duration` int DEFAULT NULL COMMENT '时长(分钟)',
  `max_participants` int DEFAULT '30' COMMENT '最大参与人数',
  `price` decimal(10,2) DEFAULT '0.00' COMMENT '价格',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-下架，1-上架',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_coach_id` (`coach_id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_status` (`status`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- 11. 课程排期表
DROP TABLE IF EXISTS `course_schedule`;
CREATE TABLE `course_schedule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '排期ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `coach_id` bigint NOT NULL COMMENT '教练ID',
  `store_id` bigint NOT NULL COMMENT '门店ID',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `room_location` varchar(100) DEFAULT NULL COMMENT '房间位置（如：瑜伽室1、力量训练区）',
  `current_participants` int DEFAULT '0' COMMENT '当前报名人数',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-已取消，1-正常，2-已结束',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_coach_id` (`coach_id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_status` (`status`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程排期表';

-- 12. 课程报名表
DROP TABLE IF EXISTS `course_enrollment`;
CREATE TABLE `course_enrollment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '报名ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `schedule_id` bigint NOT NULL COMMENT '排期ID',
  `order_id` bigint DEFAULT NULL COMMENT '订单ID',
  `payment_amount` decimal(10,2) DEFAULT 0.00 COMMENT '支付金额',
  `payment_status` tinyint DEFAULT 0 COMMENT '支付状态：0-待支付，1-已支付，2-已退款',
  `payment_time` datetime DEFAULT NULL COMMENT '支付时间',
  `refund_amount` decimal(10,2) DEFAULT 0.00 COMMENT '退款金额',
  `refund_time` datetime DEFAULT NULL COMMENT '退款时间',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
  `cancel_reason` varchar(500) DEFAULT NULL COMMENT '取消原因',
  `cancelled_by` varchar(20) DEFAULT NULL COMMENT '取消方: user用户 coach教练 system系统',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `enrollment_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-已取消，1-已报名，2-已签到',
  `check_in_time` datetime DEFAULT NULL COMMENT '签到时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_schedule` (`user_id`,`schedule_id`),
  KEY `idx_schedule_id` (`schedule_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_payment_status` (`payment_status`),
  KEY `idx_status` (`status`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程报名表';

-- 13. 运动记录表
DROP TABLE IF EXISTS `exercise_record`;
CREATE TABLE `exercise_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `exercise_date` date NOT NULL COMMENT '运动日期',
  `exercise_type` varchar(50) NOT NULL COMMENT '运动类型',
  `duration` int DEFAULT NULL COMMENT '时长(分钟)',
  `calories` int DEFAULT NULL COMMENT '消耗卡路里',
  `distance` decimal(10,2) DEFAULT NULL COMMENT '距离(公里)',
  `steps` int DEFAULT NULL COMMENT '步数',
  `heart_rate_avg` int DEFAULT NULL COMMENT '平均心率',
  `notes` text COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_user_date` (`user_id`,`exercise_date`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运动记录表';

-- 14. 运动计划表
DROP TABLE IF EXISTS `exercise_plan`;
CREATE TABLE `exercise_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `plan_name` varchar(100) NOT NULL COMMENT '计划名称',
  `goal` varchar(200) DEFAULT NULL COMMENT '目标',
  `start_date` date NOT NULL COMMENT '开始日期',
  `end_date` date NOT NULL COMMENT '结束日期',
  `plan_details` json DEFAULT NULL COMMENT '计划详情(JSON)',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-已取消，1-进行中，2-已完成',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运动计划表';

-- 15. 食物数据库表
DROP TABLE IF EXISTS `food_database`;
CREATE TABLE `food_database` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '食物ID',
  `food_name` varchar(100) NOT NULL COMMENT '食物名称',
  `category` varchar(50) DEFAULT NULL COMMENT '分类',
  `calories` decimal(10,2) DEFAULT NULL COMMENT '热量(千卡/100g)',
  `protein` decimal(10,2) DEFAULT NULL COMMENT '蛋白质(g/100g)',
  `fat` decimal(10,2) DEFAULT NULL COMMENT '脂肪(g/100g)',
  `carbohydrate` decimal(10,2) DEFAULT NULL COMMENT '碳水化合物(g/100g)',
  `fiber` decimal(10,2) DEFAULT NULL COMMENT '纤维素(g/100g)',
  `sodium` decimal(10,2) DEFAULT NULL COMMENT '钠(mg/100g)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_food_name` (`food_name`),
  KEY `idx_category` (`category`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食物数据库表';

-- 16. 饮食记录表
DROP TABLE IF EXISTS `diet_record`;
CREATE TABLE `diet_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `record_date` date NOT NULL COMMENT '记录日期',
  `meal_type` tinyint NOT NULL COMMENT '餐次：1-早餐，2-午餐，3-晚餐，4-加餐',
  `food_id` bigint DEFAULT NULL COMMENT '食物ID',
  `food_name` varchar(100) NOT NULL COMMENT '食物名称',
  `quantity` decimal(10,2) DEFAULT NULL COMMENT '数量(克)',
  `calories` decimal(10,2) DEFAULT NULL COMMENT '热量',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_user_date` (`user_id`,`record_date`),
  KEY `idx_food_id` (`food_id`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='饮食记录表';

-- 17. 动态表
DROP TABLE IF EXISTS `post`;
CREATE TABLE `post` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '动态ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `content` text NOT NULL COMMENT '内容',
  `images` json DEFAULT NULL COMMENT '图片列表(JSON)',
  `location` varchar(100) DEFAULT NULL COMMENT '位置',
  `visibility` tinyint DEFAULT '1' COMMENT '可见性：1-公开，2-好友可见，3-仅自己',
  `like_count` int DEFAULT '0' COMMENT '点赞数',
  `comment_count` int DEFAULT '0' COMMENT '评论数',
  `share_count` int DEFAULT '0' COMMENT '分享数',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-删除，1-正常',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_status` (`status`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态表';

-- 18. 评论表
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `target_id` bigint NOT NULL COMMENT '目标ID(动态ID)',
  `user_id` bigint NOT NULL COMMENT '评论用户ID',
  `parent_id` bigint DEFAULT '0' COMMENT '父评论ID',
  `content` text NOT NULL COMMENT '评论内容',
  `like_count` int DEFAULT '0' COMMENT '点赞数',
  `reply_count` int DEFAULT '0' COMMENT '回复数',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-删除，1-正常',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_target_id` (`target_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- 19. 关注表
DROP TABLE IF EXISTS `follow`;
CREATE TABLE `follow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `follower_id` bigint NOT NULL COMMENT '关注者ID',
  `followed_id` bigint NOT NULL COMMENT '被关注者ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follower_followed` (`follower_id`,`followed_id`),
  KEY `idx_followed_id` (`followed_id`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关注表';

-- 20. 点赞记录表
DROP TABLE IF EXISTS `like_record`;
CREATE TABLE `like_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `target_id` bigint NOT NULL COMMENT '目标ID',
  `target_type` tinyint NOT NULL COMMENT '目标类型：1-动态，2-评论',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target` (`user_id`,`target_id`,`target_type`),
  KEY `idx_target_id` (`target_id`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞记录表';

-- 21. 订单表
DROP TABLE IF EXISTS `order_info`;
CREATE TABLE `order_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` varchar(50) NOT NULL COMMENT '订单号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `order_type` tinyint NOT NULL COMMENT '订单类型：1-课程，2-私教',
  `total_amount` decimal(10,2) NOT NULL COMMENT '订单总金额',
  `discount_amount` decimal(10,2) DEFAULT '0.00' COMMENT '优惠金额',
  `actual_amount` decimal(10,2) NOT NULL COMMENT '实付金额',
  `pay_status` tinyint DEFAULT '0' COMMENT '支付状态：0-待支付，1-已支付，2-已退款',
  `order_status` tinyint DEFAULT '1' COMMENT '订单状态：0-已取消，1-待付款，2-已支付，3-已完成',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_order_status` (`order_status`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 22. 订单详情表
DROP TABLE IF EXISTS `order_detail`;
CREATE TABLE `order_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `item_id` bigint NOT NULL COMMENT '商品ID(课程ID或服务ID)',
  `item_name` varchar(100) NOT NULL COMMENT '商品名称',
  `item_type` varchar(20) NOT NULL COMMENT '商品类型',
  `price` decimal(10,2) NOT NULL COMMENT '单价',
  `quantity` int NOT NULL COMMENT '数量',
  `amount` decimal(10,2) NOT NULL COMMENT '小计',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单详情表';

-- 23. 支付记录表
DROP TABLE IF EXISTS `payment_record`;
CREATE TABLE `payment_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `pay_no` varchar(100) NOT NULL COMMENT '支付流水号',
  `pay_channel` varchar(20) NOT NULL COMMENT '支付渠道：alipay-支付宝，wechat-微信',
  `pay_amount` decimal(10,2) NOT NULL COMMENT '支付金额',
  `pay_status` tinyint DEFAULT '0' COMMENT '支付状态：0-待支付，1-成功，2-失败',
  `callback_time` datetime DEFAULT NULL COMMENT '回调时间',
  `callback_content` text COMMENT '回调内容',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_pay_no` (`pay_no`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付记录表';

-- 24. 退款记录表
DROP TABLE IF EXISTS `refund_record`;
CREATE TABLE `refund_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `refund_no` varchar(50) NOT NULL COMMENT '退款单号',
  `refund_amount` decimal(10,2) NOT NULL COMMENT '退款金额',
  `refund_reason` varchar(200) DEFAULT NULL COMMENT '退款原因',
  `status` tinyint DEFAULT '0' COMMENT '状态：0-申请中，1-已同意，2-已拒绝，3-已退款',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `apply_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `process_time` datetime DEFAULT NULL COMMENT '处理时间',
  `refund_time` datetime DEFAULT NULL COMMENT '退款时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_refund_no` (`refund_no`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款记录表';

-- 25. 系统配置表
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `config_key` varchar(100) NOT NULL COMMENT '配置键',
  `config_value` text COMMENT '配置值',
  `config_desc` varchar(200) DEFAULT NULL COMMENT '配置描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 26. 操作日志表
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `username` varchar(50) DEFAULT NULL COMMENT '用户名',
  `operation` varchar(100) DEFAULT NULL COMMENT '操作',
  `method` varchar(200) DEFAULT NULL COMMENT '方法名',
  `params` text COMMENT '参数',
  `result` text COMMENT '结果',
  `error_msg` text COMMENT '错误信息',
  `ip` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `operation_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `duration` bigint DEFAULT NULL COMMENT '执行时长(毫秒)',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_operation_time` (`operation_time`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ================================================================
-- 第二部分：教练业务优化模块
-- ================================================================

-- 27. 教练可用时间表
DROP TABLE IF EXISTS `coach_availability`;
CREATE TABLE `coach_availability` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `coach_id` bigint NOT NULL COMMENT '教练ID',
  `day_of_week` tinyint NOT NULL COMMENT '星期几(1-7)',
  `start_time` time NOT NULL COMMENT '开始时间',
  `end_time` time NOT NULL COMMENT '结束时间',
  `is_available` tinyint DEFAULT 1 COMMENT '是否可用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_coach_id` (`coach_id`),
  KEY `idx_day_of_week` (`day_of_week`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练可用时间表';

-- 28. 教练日程变更申请表
DROP TABLE IF EXISTS `coach_schedule_change`;
CREATE TABLE `coach_schedule_change` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `coach_id` bigint NOT NULL COMMENT '教练ID',
  `change_type` varchar(20) NOT NULL COMMENT '变更类型: leave请假 overtime加班 adjust调整',
  `original_date` date NOT NULL COMMENT '原日期',
  `original_start_time` time DEFAULT NULL COMMENT '原开始时间',
  `original_end_time` time DEFAULT NULL COMMENT '原结束时间',
  `new_date` date DEFAULT NULL COMMENT '新日期',
  `new_start_time` time DEFAULT NULL COMMENT '新开始时间',
  `new_end_time` time DEFAULT NULL COMMENT '新结束时间',
  `reason` varchar(500) NOT NULL COMMENT '变更原因',
  `status` varchar(20) DEFAULT 'pending' COMMENT '状态: pending待审核 approved已批准 rejected已拒绝',
  `apply_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `review_time` datetime DEFAULT NULL COMMENT '审核时间',
  `reviewer_id` bigint DEFAULT NULL COMMENT '审核人ID',
  `review_remark` varchar(200) DEFAULT NULL COMMENT '审核备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_coach_id` (`coach_id`),
  KEY `idx_status` (`status`),
  KEY `idx_apply_time` (`apply_time`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练日程变更申请表';

-- 29. 教练认证申请表
DROP TABLE IF EXISTS `coach_certification_apply`;
CREATE TABLE `coach_certification_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `real_name` varchar(50) NOT NULL COMMENT '真实姓名',
  `id_card` varchar(20) NOT NULL COMMENT '身份证号',
  `phone` varchar(20) NOT NULL COMMENT '联系电话',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `specialties` varchar(500) NOT NULL COMMENT '专长领域',
  `experience_years` int NOT NULL COMMENT '从业年限',
  `introduction` text COMMENT '个人介绍',
  `certificates` json DEFAULT NULL COMMENT '证书信息JSON',
  `education` json DEFAULT NULL COMMENT '教育背景JSON',
  `work_experience` json DEFAULT NULL COMMENT '工作经历JSON',
  `status` varchar(20) DEFAULT 'pending' COMMENT '状态: pending待审核 approved已通过 rejected已拒绝',
  `apply_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `review_time` datetime DEFAULT NULL COMMENT '审核时间',
  `reviewer_id` bigint DEFAULT NULL COMMENT '审核人ID',
  `review_remark` varchar(500) DEFAULT NULL COMMENT '审核备注',
  `certification_no` varchar(50) DEFAULT NULL COMMENT '认证编号(审核通过后生成)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_apply_time` (`apply_time`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练认证申请表';

-- 30. 教练离职申请表
DROP TABLE IF EXISTS `coach_resignation_apply`;
CREATE TABLE `coach_resignation_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `coach_id` bigint NOT NULL COMMENT '教练ID',
  `resignation_date` date NOT NULL COMMENT '预计离职日期',
  `reason` text NOT NULL COMMENT '离职原因',
  `handover_plan` text COMMENT '工作交接计划',
  `status` varchar(20) DEFAULT 'pending' COMMENT '状态: pending待审核 approved已批准 rejected已拒绝 cancelled已取消',
  `apply_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `review_time` datetime DEFAULT NULL COMMENT '审核时间',
  `reviewer_id` bigint DEFAULT NULL COMMENT '审核人ID',
  `review_remark` varchar(500) DEFAULT NULL COMMENT '审核备注',
  `actual_leave_date` date DEFAULT NULL COMMENT '实际离职日期',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_coach_id` (`coach_id`),
  KEY `idx_status` (`status`),
  KEY `idx_apply_time` (`apply_time`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练离职申请表';

-- 31. 教练服务项目表
DROP TABLE IF EXISTS `coach_service`;
CREATE TABLE `coach_service` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '服务ID',
  `coach_id` bigint NOT NULL COMMENT '教练ID',
  `service_name` varchar(100) NOT NULL COMMENT '服务名称',
  `service_type` varchar(50) NOT NULL COMMENT '服务类型: offline_training线下私教(300-700元/小时) online_text线上文字咨询(48元/25分钟)',
  `description` text COMMENT '服务描述',
  `duration` int NOT NULL COMMENT '服务时长(分钟)',
  `price` decimal(10,2) NOT NULL COMMENT '价格(元)',
  `max_clients` int DEFAULT 1 COMMENT '最大服务人数',
  `status` tinyint DEFAULT 1 COMMENT '状态: 0停用 1启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_coach_id` (`coach_id`),
  KEY `idx_service_type` (`service_type`),
  KEY `idx_status` (`status`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练服务项目表';

-- 32. 教练咨询记录表
DROP TABLE IF EXISTS `coach_consultation`;
CREATE TABLE `coach_consultation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '咨询ID',
  `coach_id` bigint NOT NULL COMMENT '教练ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `service_type` varchar(50) NOT NULL COMMENT '服务类型: offline_training线下私教 online_text线上文字咨询',
  `consultation_type` varchar(20) NOT NULL COMMENT '咨询类型: online在线 offline线下 (保留兼容)',
  `consultation_date` datetime NOT NULL COMMENT '咨询时间',
  `duration` int DEFAULT NULL COMMENT '咨询时长(分钟)',
  `topic` varchar(200) NOT NULL COMMENT '咨询主题',
  `content` text COMMENT '咨询内容',
  `coach_advice` text COMMENT '教练建议',
  `order_id` bigint DEFAULT NULL COMMENT '订单ID',
  `payment_amount` decimal(10,2) DEFAULT 0.00 COMMENT '支付金额',
  `payment_status` tinyint DEFAULT 0 COMMENT '支付状态：0-待支付，1-已支付，2-已退款',
  `payment_time` datetime DEFAULT NULL COMMENT '支付时间',
  `refund_amount` decimal(10,2) DEFAULT 0.00 COMMENT '退款金额',
  `refund_rate` decimal(5,2) DEFAULT 100.00 COMMENT '退款比例（0-100）',
  `refund_time` datetime DEFAULT NULL COMMENT '退款时间',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
  `cancelled_by` varchar(20) DEFAULT NULL COMMENT '取消方: user用户 coach教练 system系统',
  `cancel_reason` varchar(500) DEFAULT NULL COMMENT '取消原因',
  `status` varchar(20) DEFAULT 'booked' COMMENT '状态: booked已预约 in_progress进行中 completed已完成 cancelled_by_user用户取消 cancelled_by_coach教练取消',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_coach_id` (`coach_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_consultation_date` (`consultation_date`),
  KEY `idx_payment_status` (`payment_status`),
  KEY `idx_status` (`status`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练咨询记录表';

-- 33. 教练评价表
DROP TABLE IF EXISTS `coach_evaluation`;
CREATE TABLE `coach_evaluation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评价ID',
  `coach_id` bigint NOT NULL COMMENT '教练ID',
  `user_id` bigint NOT NULL COMMENT '评价用户ID',
  `service_id` bigint DEFAULT NULL COMMENT '服务ID',
  `order_id` bigint DEFAULT NULL COMMENT '订单ID',
  `overall_rating` decimal(2,1) NOT NULL COMMENT '综合评分',
  `professional_rating` decimal(2,1) DEFAULT NULL COMMENT '专业能力评分',
  `attitude_rating` decimal(2,1) DEFAULT NULL COMMENT '服务态度评分',
  `communication_rating` decimal(2,1) DEFAULT NULL COMMENT '沟通能力评分',
  `content` text COMMENT '评价内容',
  `tags` varchar(200) DEFAULT NULL COMMENT '评价标签',
  `images` json DEFAULT NULL COMMENT '评价图片',
  `status` tinyint DEFAULT 1 COMMENT '状态: 0已删除 1正常',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_coach_id` (`coach_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_service_id` (`service_id`),
  KEY `idx_status` (`status`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练评价表';

-- 34. 教练收入记录表
DROP TABLE IF EXISTS `coach_income`;
CREATE TABLE `coach_income` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '收入ID',
  `coach_id` bigint NOT NULL COMMENT '教练ID',
  `income_type` varchar(20) NOT NULL COMMENT '收入类型: course课程 service服务 bonus奖金',
  `source_id` bigint DEFAULT NULL COMMENT '来源ID(订单ID/课程ID等)',
  `income_amount` decimal(10,2) NOT NULL COMMENT '收入金额',
  `commission_rate` decimal(5,2) DEFAULT NULL COMMENT '提成比例(%)',
  `actual_amount` decimal(10,2) NOT NULL COMMENT '实际收入',
  `income_date` date NOT NULL COMMENT '收入日期',
  `settlement_status` varchar(20) DEFAULT 'pending' COMMENT '结算状态: pending待结算 settled已结算',
  `settlement_date` date DEFAULT NULL COMMENT '结算日期',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_coach_id` (`coach_id`),
  KEY `idx_income_date` (`income_date`),
  KEY `idx_settlement_status` (`settlement_status`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练收入记录表';

-- 35. 教练结算记录表
DROP TABLE IF EXISTS `coach_settlement`;
CREATE TABLE `coach_settlement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '结算ID',
  `coach_id` bigint NOT NULL COMMENT '教练ID',
  `settlement_no` varchar(50) NOT NULL COMMENT '结算单号',
  `settlement_period` varchar(20) NOT NULL COMMENT '结算周期',
  `start_date` date NOT NULL COMMENT '开始日期',
  `end_date` date NOT NULL COMMENT '结束日期',
  `total_income` decimal(10,2) NOT NULL COMMENT '总收入',
  `deduction_amount` decimal(10,2) DEFAULT 0.00 COMMENT '扣除金额',
  `actual_amount` decimal(10,2) NOT NULL COMMENT '实际结算金额',
  `settlement_date` date NOT NULL COMMENT '结算日期',
  `payment_method` varchar(20) DEFAULT NULL COMMENT '支付方式',
  `payment_account` varchar(100) DEFAULT NULL COMMENT '支付账户',
  `status` varchar(20) DEFAULT 'pending' COMMENT '状态: pending待支付 paid已支付 failed支付失败',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_settlement_no` (`settlement_no`),
  KEY `idx_coach_id` (`coach_id`),
  KEY `idx_settlement_date` (`settlement_date`),
  KEY `idx_status` (`status`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练结算记录表';

-- 36. 教练资格撤销申请表
DROP TABLE IF EXISTS `coach_qualification_revoke_apply`;
CREATE TABLE `coach_qualification_revoke_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `coach_id` bigint NOT NULL COMMENT '教练ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `revoke_reason` text NOT NULL COMMENT '撤销原因',
  `effective_date` date NOT NULL COMMENT '期望生效日期',
  `status` varchar(20) DEFAULT 'pending' COMMENT '状态: pending待审核 approved已批准 rejected已拒绝 cancelled已取消',
  `apply_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `review_time` datetime DEFAULT NULL COMMENT '审核时间',
  `reviewer_id` bigint DEFAULT NULL COMMENT '审核人ID',
  `review_remark` varchar(500) DEFAULT NULL COMMENT '审核备注',
  `actual_revoke_date` date DEFAULT NULL COMMENT '实际撤销日期',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_coach_id` (`coach_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_apply_time` (`apply_time`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练资格撤销申请表';

-- 37. 教练预约时段表
DROP TABLE IF EXISTS `coach_booking_slot`;
CREATE TABLE `coach_booking_slot` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '时段ID',
  `coach_id` bigint NOT NULL COMMENT '教练ID',
  `service_id` bigint NOT NULL COMMENT '服务项目ID',
  `booking_date` date NOT NULL COMMENT '预约日期',
  `start_time` time NOT NULL COMMENT '开始时间',
  `end_time` time NOT NULL COMMENT '结束时间',
  `status` varchar(20) DEFAULT 'available' COMMENT '状态: available可预约 booked已预约 blocked已屏蔽 completed已完成',
  `user_id` bigint DEFAULT NULL COMMENT '预约用户ID',
  `order_id` bigint DEFAULT NULL COMMENT '关联订单ID',
  `booking_price` decimal(10,2) DEFAULT NULL COMMENT '预约价格',
  `booking_time` datetime DEFAULT NULL COMMENT '预约时间',
  `completion_time` datetime DEFAULT NULL COMMENT '完成时间',
  `cancellation_time` datetime DEFAULT NULL COMMENT '取消时间',
  `cancellation_reason` varchar(200) DEFAULT NULL COMMENT '取消原因',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_coach_date` (`coach_id`, `booking_date`),
  KEY `idx_service_id` (`service_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_status` (`status`),
  KEY `idx_booking_time` (`booking_time`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练预约时段表';

-- ================================================================
-- 用户账号管理模块
-- ================================================================

-- 用户账号注销申请表
DROP TABLE IF EXISTS `user_account_cancel_apply`;
CREATE TABLE `user_account_cancel_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `phone` varchar(20) NOT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `cancel_type` varchar(20) NOT NULL COMMENT '注销类型: temporary临时 permanent永久',
  `reason` varchar(500) NOT NULL COMMENT '注销原因',
  `apply_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `status` varchar(20) DEFAULT 'pending' COMMENT '状态: pending待审核 approved已批准 rejected已拒绝 cancelled已取消',
  `review_time` datetime DEFAULT NULL COMMENT '审核时间',
  `reviewer_id` bigint DEFAULT NULL COMMENT '审核人 ID',
  `review_remark` varchar(500) DEFAULT NULL COMMENT '审核备注',
  `effective_time` datetime DEFAULT NULL COMMENT '生效时间',
  `actual_cancel_time` datetime DEFAULT NULL COMMENT '实际注销时间',
  `cancel_deadline` datetime DEFAULT NULL COMMENT '撤销截止时间',
  `is_cancellable` tinyint DEFAULT 1 COMMENT '是否可撤销: 0否 1是',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_apply_time` (`apply_time`),
  KEY `idx_effective_time` (`effective_time`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户账号注销申请表';

-- ================================================================
-- 第三部分：健康科普文章模块
-- ================================================================

-- 38. 文章分类表
DROP TABLE IF EXISTS `article_category`;
CREATE TABLE `article_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `category_name` varchar(50) NOT NULL COMMENT '分类名称',
  `category_code` varchar(50) NOT NULL COMMENT '分类编码',
  `parent_id` bigint DEFAULT 0 COMMENT '父分类ID',
  `description` varchar(200) DEFAULT NULL COMMENT '分类描述',
  `cover_image` varchar(255) DEFAULT NULL COMMENT '分类封面',
  `sort_order` int DEFAULT 0 COMMENT '排序',
  `status` tinyint DEFAULT 1 COMMENT '状态: 0禁用 1启用',
  `seo_keywords` varchar(200) DEFAULT NULL COMMENT 'SEO关键词',
  `seo_description` varchar(500) DEFAULT NULL COMMENT 'SEO描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_code` (`category_code`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_status` (`status`),
  KEY `idx_sort_order` (`sort_order`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章分类表';

-- 39. 健康文章表
DROP TABLE IF EXISTS `health_article`;
CREATE TABLE `health_article` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文章ID',
  `article_no` varchar(50) DEFAULT NULL COMMENT '文章编号',
  `title` varchar(200) NOT NULL COMMENT '文章标题',
  `subtitle` varchar(300) DEFAULT NULL COMMENT '副标题',
  `author_id` bigint NOT NULL COMMENT '作者ID(教练ID)',
  `author_name` varchar(50) NOT NULL COMMENT '作者姓名',
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `cover_image` varchar(500) DEFAULT NULL COMMENT '封面图片',
  `summary` varchar(500) DEFAULT NULL COMMENT '文章摘要',
  `content` longtext NOT NULL COMMENT '文章内容',
  `content_type` varchar(20) DEFAULT 'richtext' COMMENT '内容类型: markdown richtext',
  `tags` json DEFAULT NULL COMMENT '文章标签',
  `keywords` varchar(200) DEFAULT NULL COMMENT '关键词',
  `source` varchar(100) DEFAULT NULL COMMENT '文章来源',
  `reading_time` int DEFAULT NULL COMMENT '预估阅读时长(分钟)',
  `difficulty_level` varchar(20) DEFAULT NULL COMMENT '难度级别: beginner intermediate advanced',
  `status` varchar(20) DEFAULT 'draft' COMMENT '状态: draft草稿 submitted已提交 published已发布 rejected已拒绝',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_by` bigint DEFAULT NULL COMMENT '审核人ID',
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核备注',
  `reject_reason` varchar(500) DEFAULT NULL COMMENT '拒绝原因',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `view_count` int DEFAULT 0 COMMENT '浏览量',
  `like_count` int DEFAULT 0 COMMENT '点赞数',
  `collect_count` int DEFAULT 0 COMMENT '收藏数',
  `share_count` int DEFAULT 0 COMMENT '分享数',
  `comment_count` int DEFAULT 0 COMMENT '评论数',
  `is_top` tinyint DEFAULT 0 COMMENT '是否置顶',
  `is_recommend` tinyint DEFAULT 0 COMMENT '是否推荐',
  `is_hot` tinyint DEFAULT 0 COMMENT '是否热门',
  `sort_order` int DEFAULT 0 COMMENT '排序',
  `allow_comment` tinyint DEFAULT 1 COMMENT '是否允许评论',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_no` (`article_no`),
  KEY `idx_author_id` (`author_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_publish_time` (`publish_time`),
  KEY `idx_view_count` (`view_count`),
  KEY `idx_like_count` (`like_count`),
  KEY `idx_is_top` (`is_top`),
  KEY `idx_is_recommend` (`is_recommend`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康文章表';

-- 40. 文章审核日志表
DROP TABLE IF EXISTS `article_audit_log`;
CREATE TABLE `article_audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `auditor_id` bigint NOT NULL COMMENT '审核人ID',
  `audit_action` varchar(20) NOT NULL COMMENT '审核动作: submit提交 approve通过 reject拒绝 revoke撤回',
  `original_status` varchar(20) DEFAULT NULL COMMENT '原状态',
  `new_status` varchar(20) NOT NULL COMMENT '新状态',
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `audit_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_article_id` (`article_id`),
  KEY `idx_auditor_id` (`auditor_id`),
  KEY `idx_audit_time` (`audit_time`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章审核日志表';

-- 41. 文章浏览记录表
DROP TABLE IF EXISTS `article_view_log`;
CREATE TABLE `article_view_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID(未登录为空)',
  `ip_address` varchar(45) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
  `reading_duration` int DEFAULT NULL COMMENT '阅读时长(秒)',
  `view_source` varchar(20) DEFAULT NULL COMMENT '浏览来源: search搜索 recommend推荐 category分类 direct直接',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `view_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_article_id` (`article_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_view_time` (`view_time`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章浏览记录表';

-- 42. 文章点赞表
DROP TABLE IF EXISTS `article_like`;
CREATE TABLE `article_like` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_user` (`article_id`, `user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章点赞表';

-- 43. 文章收藏表
DROP TABLE IF EXISTS `article_collect`;
CREATE TABLE `article_collect` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `folder_id` bigint DEFAULT NULL COMMENT '收藏夹ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_user` (`article_id`, `user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_folder_id` (`folder_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章收藏表';

-- 44. 文章收藏夹表
DROP TABLE IF EXISTS `article_collect_folder`;
CREATE TABLE `article_collect_folder` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `folder_name` varchar(50) NOT NULL COMMENT '收藏夹名称',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  `is_public` tinyint DEFAULT 0 COMMENT '是否公开: 0私密 1公开',
  `article_count` int DEFAULT 0 COMMENT '文章数量',
  `sort_order` int DEFAULT 0 COMMENT '排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章收藏夹表';

-- 45. 文章分享记录表
DROP TABLE IF EXISTS `article_share_log`;
CREATE TABLE `article_share_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `share_platform` varchar(20) NOT NULL COMMENT '分享平台: wechat微信 moments朋友圈 weibo微博 link链接',
  `share_url` varchar(500) DEFAULT NULL COMMENT '分享链接',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '分享时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_article_id` (`article_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章分享记录表';

-- 46. 文章评论表
DROP TABLE IF EXISTS `article_comment`;
CREATE TABLE `article_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `user_id` bigint NOT NULL COMMENT '评论用户ID',
  `parent_id` bigint DEFAULT 0 COMMENT '父评论ID（0为顶级评论）',
  `content` text NOT NULL COMMENT '评论内容',
  `images` json DEFAULT NULL COMMENT '评论图片',
  `like_count` int DEFAULT 0 COMMENT '点赞数',
  `reply_count` int DEFAULT 0 COMMENT '回复数',
  `is_author` tinyint DEFAULT 0 COMMENT '是否作者回复: 0否 1是',
  `status` tinyint DEFAULT 1 COMMENT '状态: 0已删除 1正常 2已屏蔽',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_article_id` (`article_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_status` (`status`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章评论表';

-- ================================================================
-- 第四部分：系统功能增强模块(用户统计、定时任务、权限管理)
-- ================================================================

-- 47. 用户登录日志表
DROP TABLE IF EXISTS `user_login_log`;
CREATE TABLE `user_login_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `username` varchar(50) DEFAULT NULL COMMENT '用户名',
  `login_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  `login_ip` varchar(50) DEFAULT NULL COMMENT '登录IP',
  `login_device` varchar(100) DEFAULT NULL COMMENT '登录设备',
  `login_os` varchar(50) DEFAULT NULL COMMENT '操作系统',
  `browser` varchar(50) DEFAULT NULL COMMENT '浏览器',
  `login_type` varchar(20) DEFAULT 'normal' COMMENT '登录类型: normal正常 oauth第三方',
  `login_status` tinyint DEFAULT '1' COMMENT '登录状态: 0失败 1成功',
  `fail_reason` varchar(200) DEFAULT NULL COMMENT '失败原因',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_login_time` (`login_time`),
  KEY `idx_login_status` (`login_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户登录日志表';

-- 48. 用户活动日志表
DROP TABLE IF EXISTS `user_activity_log`;
CREATE TABLE `user_activity_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `activity_date` date NOT NULL COMMENT '活动日期',
  `activity_type` varchar(50) DEFAULT NULL COMMENT '活动类型: login登录 course_enroll课程报名 exercise运动记录等',
  `activity_count` int DEFAULT '1' COMMENT '活动次数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date_type` (`user_id`, `activity_date`, `activity_type`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_activity_date` (`activity_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户活动日志表';

-- 49. 课程统计表
DROP TABLE IF EXISTS `course_statistics`;
CREATE TABLE `course_statistics` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '统计ID',
  `statistics_date` date NOT NULL COMMENT '统计日期',
  `new_enrollment_count` int DEFAULT '0' COMMENT '新增报名数',
  `cancel_enrollment_count` int DEFAULT '0' COMMENT '取消报名数',
  `completed_course_count` int DEFAULT '0' COMMENT '完成课程数',
  `check_in_count` int DEFAULT '0' COMMENT '签到人数',
  `total_participants` int DEFAULT '0' COMMENT '总参与人数',
  `popular_course_id` bigint DEFAULT NULL COMMENT '最受欢迎课程ID',
  `popular_course_name` varchar(100) DEFAULT NULL COMMENT '最受欢迎课程名称',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_statistics_date` (`statistics_date`),
  KEY `idx_statistics_date` (`statistics_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程统计表';

-- 50. 教练工作统计表
DROP TABLE IF EXISTS `coach_work_statistics`;
CREATE TABLE `coach_work_statistics` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '统计ID',
  `coach_id` bigint NOT NULL COMMENT '教练ID',
  `coach_name` varchar(50) DEFAULT NULL COMMENT '教练姓名',
  `statistics_date` date NOT NULL COMMENT '统计日期',
  `course_count` int DEFAULT '0' COMMENT '授课次数',
  `course_hours` decimal(5,2) DEFAULT '0.00' COMMENT '授课时长(小时)',
  `student_count` int DEFAULT '0' COMMENT '服务学员数',
  `consultation_count` int DEFAULT '0' COMMENT '咨询次数',
  `income_amount` decimal(10,2) DEFAULT '0.00' COMMENT '收入金额',
  `rating_avg` decimal(3,2) DEFAULT '0.00' COMMENT '平均评分',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coach_date` (`coach_id`, `statistics_date`),
  KEY `idx_coach_id` (`coach_id`),
  KEY `idx_statistics_date` (`statistics_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练工作统计表';

-- 51. 系统通知表
DROP TABLE IF EXISTS `system_notification`;
CREATE TABLE `system_notification` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID(为NULL表示系统广播)',
  `notification_type` varchar(50) NOT NULL COMMENT '通知类型: course_reminder课程提醒 system_notice系统通知 course_cancel课程取消',
  `title` varchar(200) NOT NULL COMMENT '通知标题',
  `content` text NOT NULL COMMENT '通知内容',
  `related_id` bigint DEFAULT NULL COMMENT '关联ID(如课程ID)',
  `related_type` varchar(50) DEFAULT NULL COMMENT '关联类型: course schedule order',
  `is_read` tinyint DEFAULT '0' COMMENT '是否已读: 0未读 1已读',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `send_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_notification_type` (`notification_type`),
  KEY `idx_is_read` (`is_read`),
  KEY `idx_send_time` (`send_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统通知表';

-- 52. 权限表
DROP TABLE IF EXISTS `permission`;
CREATE TABLE `permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `permission_name` varchar(100) NOT NULL COMMENT '权限名称',
  `permission_code` varchar(100) NOT NULL COMMENT '权限编码',
  `permission_type` varchar(20) DEFAULT 'menu' COMMENT '权限类型: menu菜单 button按钮 api接口',
  `parent_id` bigint DEFAULT '0' COMMENT '父权限ID',
  `resource_path` varchar(200) DEFAULT NULL COMMENT '资源路径(菜单路径/接口路径)',
  `resource_method` varchar(20) DEFAULT NULL COMMENT '请求方法: GET POST PUT DELETE',
  `icon` varchar(100) DEFAULT NULL COMMENT '图标',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `status` tinyint DEFAULT '1' COMMENT '状态: 0禁用 1启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_permission_type` (`permission_type`),
  KEY `idx_status` (`status`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- 53. 角色权限关联表
DROP TABLE IF EXISTS `role_permission`;
CREATE TABLE `role_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `permission_id` bigint NOT NULL COMMENT '权限ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission_active` (`role_id`, `permission_id`, `is_deleted`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_permission_id` (`permission_id`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- 54. 临时文件记录表
DROP TABLE IF EXISTS `temp_file_record`;
CREATE TABLE `temp_file_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `file_name` varchar(255) NOT NULL COMMENT '文件名',
  `file_path` varchar(500) NOT NULL COMMENT '文件路径(MinIO路径)',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小(字节)',
  `file_type` varchar(50) DEFAULT NULL COMMENT '文件类型',
  `upload_user_id` bigint DEFAULT NULL COMMENT '上传用户ID',
  `business_type` varchar(50) DEFAULT NULL COMMENT '业务类型: avatar头像 import导入 export导出',
  `status` varchar(20) DEFAULT 'temp' COMMENT '状态: temp临时 used已使用 expired已过期',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `used_time` datetime DEFAULT NULL COMMENT '使用时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_file_path` (`file_path`(255)),
  KEY `idx_upload_user_id` (`upload_user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_expire_time` (`expire_time`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='临时文件记录表';

-- 55. 用户钱包表
DROP TABLE IF EXISTS `user_wallet`;
CREATE TABLE `user_wallet` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '钱包ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `balance` decimal(10,2) DEFAULT 0.00 COMMENT '账户余额(元)',
  `frozen_amount` decimal(10,2) DEFAULT 0.00 COMMENT '冻结金额(元)',
  `total_recharge` decimal(10,2) DEFAULT 0.00 COMMENT '累计充值',
  `total_consumption` decimal(10,2) DEFAULT 0.00 COMMENT '累计消费',
  `total_refund` decimal(10,2) DEFAULT 0.00 COMMENT '累计退款',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户钱包表';

-- 56. 钱包交易流水表
DROP TABLE IF EXISTS `wallet_transaction`;
CREATE TABLE `wallet_transaction` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `transaction_no` varchar(50) NOT NULL COMMENT '交易流水号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `transaction_type` varchar(50) NOT NULL COMMENT '交易类型: recharge充值 payment支付 refund退款 freeze冻结 unfreeze解冻',
  `amount` decimal(10,2) NOT NULL COMMENT '金额(元)',
  `balance_before` decimal(10,2) DEFAULT NULL COMMENT '交易前余额',
  `balance_after` decimal(10,2) DEFAULT NULL COMMENT '交易后余额',
  `related_order_id` bigint DEFAULT NULL COMMENT '关联订单ID',
  `related_type` varchar(50) DEFAULT NULL COMMENT '关联类型: course_enrollment课程报名 coach_consultation教练咨询',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_transaction_no` (`transaction_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_transaction_type` (`transaction_type`),
  KEY `idx_related_order_id` (`related_order_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='钱包交易流水表';

-- ================================================================
-- 初始化数据
-- ================================================================

-- 插入默认角色
INSERT INTO `role` (`role_name`, `role_code`, `description`, `status`, `create_time`, `update_time`, `is_deleted`) VALUES
('管理员', 'admin', '系统管理员', 1, NOW(), NOW(), 0),
('教练', 'coach', '健身教练', 1, NOW(), NOW(), 0),
('会员', 'member', '普通会员', 1, NOW(), NOW(), 0),
('VIP用户', 'vip', 'VIP用户，享受更多特权服务', 1, NOW(), NOW(), 0);

-- 插入课程分类
INSERT INTO `course_category` (`category_name`, `parent_id`, `sort_order`) VALUES 
('有氧运动', 0, 1),
('力量训练', 0, 2),
('瑜伽课程', 0, 3),
('舞蹈课程', 0, 4),
('康复训练', 0, 5);

-- 插入文章分类
INSERT INTO `article_category` (`category_name`, `category_code`, `description`, `sort_order`) VALUES 
('运动知识', 'sport_knowledge', '运动理论、技巧、方法等专业知识', 1),
('营养健康', 'nutrition_health', '饮食营养、健康饮食搭配等', 2),
('康复保健', 'rehabilitation', '运动康复、伤病预防与恢复', 3),
('健身计划', 'fitness_plan', '各类健身训练计划和方案', 4),
('运动心理', 'sport_psychology', '运动心理学、动力激励等', 5),
('装备推荐', 'equipment', '运动装备选择与使用建议', 6);

-- 插入系统配置
INSERT INTO `system_config` (`config_key`, `config_value`, `config_desc`) VALUES
('system.name', '健身管理平台', '系统名称'),
('system.version', '2.0.0', '系统版本'),
('file.upload.path', '/uploads', '文件上传路径'),
('file.upload.maxSize', '10485760', '文件上传最大大小(字节)');

-- ================================================================
-- 插入权限初始数据（RBAC权限系统）
-- ================================================================

-- 一级菜单权限
INSERT INTO `permission` (`permission_name`, `permission_code`, `permission_type`, `parent_id`, `resource_path`, `sort_order`, `description`, `status`, `create_time`, `is_deleted`) VALUES
('用户管理', 'user_management', 'menu', 0, '/admin/user', 1, '用户信息管理模块', 1, NOW(), 0),
('角色管理', 'role_management', 'menu', 0, '/admin/role', 2, '角色权限管理模块', 1, NOW(), 0),
('教练管理', 'coach_management', 'menu', 0, '/admin/coach', 3, '教练信息管理模块', 1, NOW(), 0),
('课程管理', 'course_management', 'menu', 0, '/admin/course', 4, '课程信息管理模块', 1, NOW(), 0),
('门店管理', 'store_management', 'menu', 0, '/admin/gym-store', 5, '门店信息管理模块', 1, NOW(), 0),
('文章管理', 'article_management', 'menu', 0, '/admin/health-article', 6, '健康文章管理模块', 1, NOW(), 0),
('订单管理', 'order_management', 'menu', 0, '/admin/order', 7, '订单支付管理模块', 1, NOW(), 0),
('统计报表', 'statistics', 'menu', 0, '/admin/statistics', 8, '数据统计分析模块', 1, NOW(), 0);

-- 获取父权限ID并存储到变量
SET @user_mgmt_id = (SELECT id FROM permission WHERE permission_code='user_management');
SET @role_mgmt_id = (SELECT id FROM permission WHERE permission_code='role_management');
SET @coach_mgmt_id = (SELECT id FROM permission WHERE permission_code='coach_management');
SET @course_mgmt_id = (SELECT id FROM permission WHERE permission_code='course_management');
SET @store_mgmt_id = (SELECT id FROM permission WHERE permission_code='store_management');
SET @article_mgmt_id = (SELECT id FROM permission WHERE permission_code='article_management');
SET @order_mgmt_id = (SELECT id FROM permission WHERE permission_code='order_management');
SET @statistics_id = (SELECT id FROM permission WHERE permission_code='statistics');

-- 用户管理子权限
INSERT INTO `permission` (`permission_name`, `permission_code`, `permission_type`, `parent_id`, `resource_path`, `resource_method`, `sort_order`, `description`, `status`, `create_time`, `is_deleted`) VALUES
('用户列表', 'user:list', 'api', @user_mgmt_id, '/admin/user/page', 'GET', 1, '查看用户列表', 1, NOW(), 0),
('用户详情', 'user:detail', 'api', @user_mgmt_id, '/admin/user/*', 'GET', 2, '查看用户详情', 1, NOW(), 0),
('新增用户', 'user:add', 'api', @user_mgmt_id, '/admin/user', 'POST', 3, '添加新用户', 1, NOW(), 0),
('修改用户', 'user:update', 'api', @user_mgmt_id, '/admin/user', 'PUT', 4, '修改用户信息', 1, NOW(), 0),
('删除用户', 'user:delete', 'api', @user_mgmt_id, '/admin/user/*', 'DELETE', 5, '删除用户', 1, NOW(), 0),
('批量删除用户', 'user:batch_delete', 'api', @user_mgmt_id, '/admin/user/batch', 'DELETE', 6, '批量删除用户', 1, NOW(), 0),
('修改用户状态', 'user:status', 'api', @user_mgmt_id, '/admin/user/*/status', 'PUT', 7, '修改用户状态', 1, NOW(), 0);

-- 角色管理子权限
INSERT INTO `permission` (`permission_name`, `permission_code`, `permission_type`, `parent_id`, `resource_path`, `resource_method`, `sort_order`, `description`, `status`, `create_time`, `is_deleted`) VALUES
('角色列表', 'role:list', 'api', @role_mgmt_id, '/admin/role/page', 'GET', 1, '查看角色列表', 1, NOW(), 0),
('角色详情', 'role:detail', 'api', @role_mgmt_id, '/admin/role/*', 'GET', 2, '查看角色详情', 1, NOW(), 0),
('新增角色', 'role:add', 'api', @role_mgmt_id, '/admin/role', 'POST', 3, '添加新角色', 1, NOW(), 0),
('修改角色', 'role:update', 'api', @role_mgmt_id, '/admin/role', 'PUT', 4, '修改角色信息', 1, NOW(), 0),
('删除角色', 'role:delete', 'api', @role_mgmt_id, '/admin/role/*', 'DELETE', 5, '删除角色', 1, NOW(), 0),
('角色权限配置', 'role:permission', 'api', @role_mgmt_id, '/admin/role/*/permissions', 'POST', 6, '配置角色权限', 1, NOW(), 0);

-- 教练管理子权限
INSERT INTO `permission` (`permission_name`, `permission_code`, `permission_type`, `parent_id`, `resource_path`, `resource_method`, `sort_order`, `description`, `status`, `create_time`, `is_deleted`) VALUES
('教练列表', 'coach:list', 'api', @coach_mgmt_id, '/admin/coach/page', 'GET', 1, '查看教练列表', 1, NOW(), 0),
('教练详情', 'coach:detail', 'api', @coach_mgmt_id, '/admin/coach/*', 'GET', 2, '查看教练详情', 1, NOW(), 0),
('新增教练', 'coach:add', 'api', @coach_mgmt_id, '/admin/coach', 'POST', 3, '添加新教练', 1, NOW(), 0),
('修改教练', 'coach:update', 'api', @coach_mgmt_id, '/admin/coach', 'PUT', 4, '修改教练信息', 1, NOW(), 0),
('删除教练', 'coach:delete', 'api', @coach_mgmt_id, '/admin/coach/*', 'DELETE', 5, '删除教练', 1, NOW(), 0),
('教练认证审核', 'coach:cert_review', 'api', @coach_mgmt_id, '/admin/coach-certification-apply/*/review', 'PUT', 6, '审核教练认证申请', 1, NOW(), 0),
('教练离职审核', 'coach:resign_review', 'api', @coach_mgmt_id, '/admin/coach-resignation-apply/*/review', 'PUT', 7, '审核教练离职申请', 1, NOW(), 0);

-- 课程管理子权限
INSERT INTO `permission` (`permission_name`, `permission_code`, `permission_type`, `parent_id`, `resource_path`, `resource_method`, `sort_order`, `description`, `status`, `create_time`, `is_deleted`) VALUES
('课程列表', 'course:list', 'api', @course_mgmt_id, '/admin/course/page', 'GET', 1, '查看课程列表', 1, NOW(), 0),
('课程详情', 'course:detail', 'api', @course_mgmt_id, '/admin/course/*', 'GET', 2, '查看课程详情', 1, NOW(), 0),
('新增课程', 'course:add', 'api', @course_mgmt_id, '/admin/course', 'POST', 3, '添加新课程', 1, NOW(), 0),
('修改课程', 'course:update', 'api', @course_mgmt_id, '/admin/course', 'PUT', 4, '修改课程信息', 1, NOW(), 0),
('删除课程', 'course:delete', 'api', @course_mgmt_id, '/admin/course/*', 'DELETE', 5, '删除课程', 1, NOW(), 0),
('课程排期管理', 'course:schedule', 'api', @course_mgmt_id, '/admin/course-schedule/**', 'ALL', 6, '课程排期管理', 1, NOW(), 0);

-- 门店管理子权限
INSERT INTO `permission` (`permission_name`, `permission_code`, `permission_type`, `parent_id`, `resource_path`, `resource_method`, `sort_order`, `description`, `status`, `create_time`, `is_deleted`) VALUES
('门店列表', 'store:list', 'api', @store_mgmt_id, '/admin/gym-store/page', 'GET', 1, '查看门店列表', 1, NOW(), 0),
('门店详情', 'store:detail', 'api', @store_mgmt_id, '/admin/gym-store/*', 'GET', 2, '查看门店详情', 1, NOW(), 0),
('新增门店', 'store:add', 'api', @store_mgmt_id, '/admin/gym-store', 'POST', 3, '添加新门店', 1, NOW(), 0),
('修改门店', 'store:update', 'api', @store_mgmt_id, '/admin/gym-store', 'PUT', 4, '修改门店信息', 1, NOW(), 0),
('删除门店', 'store:delete', 'api', @store_mgmt_id, '/admin/gym-store/*', 'DELETE', 5, '删除门店', 1, NOW(), 0);

-- 文章管理子权限
INSERT INTO `permission` (`permission_name`, `permission_code`, `permission_type`, `parent_id`, `resource_path`, `resource_method`, `sort_order`, `description`, `status`, `create_time`, `is_deleted`) VALUES
('文章列表', 'article:list', 'api', @article_mgmt_id, '/admin/health-article/page', 'GET', 1, '查看文章列表', 1, NOW(), 0),
('文章详情', 'article:detail', 'api', @article_mgmt_id, '/admin/health-article/*', 'GET', 2, '查看文章详情', 1, NOW(), 0),
('新增文章', 'article:add', 'api', @article_mgmt_id, '/admin/health-article', 'POST', 3, '添加新文章', 1, NOW(), 0),
('修改文章', 'article:update', 'api', @article_mgmt_id, '/admin/health-article', 'PUT', 4, '修改文章信息', 1, NOW(), 0),
('删除文章', 'article:delete', 'api', @article_mgmt_id, '/admin/health-article/*', 'DELETE', 5, '删除文章', 1, NOW(), 0),
('文章审核', 'article:audit', 'api', @article_mgmt_id, '/admin/health-article/*/audit', 'PUT', 6, '审核文章', 1, NOW(), 0);

-- 订单管理子权限
INSERT INTO `permission` (`permission_name`, `permission_code`, `permission_type`, `parent_id`, `resource_path`, `resource_method`, `sort_order`, `description`, `status`, `create_time`, `is_deleted`) VALUES
('订单列表', 'order:list', 'api', @order_mgmt_id, '/admin/order-info/page', 'GET', 1, '查看订单列表', 1, NOW(), 0),
('订单详情', 'order:detail', 'api', @order_mgmt_id, '/admin/order-info/*', 'GET', 2, '查看订单详情', 1, NOW(), 0),
('订单处理', 'order:process', 'api', @order_mgmt_id, '/admin/order-info/*/process', 'PUT', 3, '处理订单', 1, NOW(), 0),
('退款审核', 'order:refund', 'api', @order_mgmt_id, '/admin/refund-record/*/review', 'PUT', 4, '审核退款申请', 1, NOW(), 0);

-- 统计报表子权限
INSERT INTO `permission` (`permission_name`, `permission_code`, `permission_type`, `parent_id`, `resource_path`, `resource_method`, `sort_order`, `description`, `status`, `create_time`, `is_deleted`) VALUES
('用户统计', 'statistics:user', 'api', @statistics_id, '/admin/statistics/user', 'GET', 1, '查看用户统计数据', 1, NOW(), 0),
('课程统计', 'statistics:course', 'api', @statistics_id, '/admin/statistics/course', 'GET', 2, '查看课程统计数据', 1, NOW(), 0),
('教练统计', 'statistics:coach', 'api', @statistics_id, '/admin/statistics/coach', 'GET', 3, '查看教练统计数据', 1, NOW(), 0),
('订单统计', 'statistics:order', 'api', @statistics_id, '/admin/statistics/order', 'GET', 4, '查看订单统计数据', 1, NOW(), 0);

-- ================================================================
-- 插入角色权限关联数据
-- ================================================================

-- 管理员角色拥有所有权限
INSERT INTO `role_permission` (`role_id`, `permission_id`, `create_time`, `is_deleted`)
SELECT
    (SELECT id FROM role WHERE role_code = 'admin') as role_id,
    p.id as permission_id,
    NOW() as create_time,
    0 as is_deleted
FROM permission p
WHERE p.is_deleted = 0;

-- 教练角色拥有部分权限（自己的信息查看、课程管理、文章管理）
INSERT INTO `role_permission` (`role_id`, `permission_id`, `create_time`, `is_deleted`)
SELECT
    (SELECT id FROM role WHERE role_code = 'coach') as role_id,
    p.id as permission_id,
    NOW() as create_time,
    0 as is_deleted
FROM permission p
WHERE p.permission_code IN (
    'coach:list', 'coach:detail',
    'course:list', 'course:detail', 'course:schedule',
    'article:list', 'article:detail', 'article:add', 'article:update'
) AND p.is_deleted = 0;

-- 会员角色只有基本查看权限
INSERT INTO `role_permission` (`role_id`, `permission_id`, `create_time`, `is_deleted`)
SELECT
    (SELECT id FROM role WHERE role_code = 'member') as role_id,
    p.id as permission_id,
    NOW() as create_time,
    0 as is_deleted
FROM permission p
WHERE p.permission_code IN (
    'user:detail',
    'course:list', 'course:detail',
    'coach:list', 'coach:detail',
    'store:list', 'store:detail',
    'article:list', 'article:detail'
) AND p.is_deleted = 0;

-- VIP用户比普通会员多一些权限
INSERT INTO `role_permission` (`role_id`, `permission_id`, `create_time`, `is_deleted`)
SELECT
    (SELECT id FROM role WHERE role_code = 'vip') as role_id,
    p.id as permission_id,
    NOW() as create_time,
    0 as is_deleted
FROM permission p
WHERE p.permission_code IN (
    'user:detail',
    'course:list', 'course:detail',
    'coach:list', 'coach:detail',
    'store:list', 'store:detail',
    'article:list', 'article:detail',
    'order:list', 'order:detail'
) AND p.is_deleted = 0;

-- ================================================================
-- 存储过程
-- ================================================================

-- 删除已存在的存储过程
DROP PROCEDURE IF EXISTS `GenerateArticleNo`;
DROP PROCEDURE IF EXISTS `GenerateOrderNo`;

DELIMITER $$

-- 生成文章编号
CREATE PROCEDURE `GenerateArticleNo`(
    OUT article_no VARCHAR(50)
)
BEGIN
    DECLARE today_count INT DEFAULT 0;
    DECLARE date_str VARCHAR(8);
    
    SET date_str = DATE_FORMAT(NOW(), '%Y%m%d');
    
    SELECT COUNT(*) + 1 INTO today_count
    FROM health_article
    WHERE DATE(create_time) = CURDATE();
    
    SET article_no = CONCAT('ART', date_str, LPAD(today_count, 5, '0'));
END$$

-- 生成订单号
CREATE PROCEDURE `GenerateOrderNo`(
    OUT order_no VARCHAR(50)
)
BEGIN
    DECLARE today_count INT DEFAULT 0;
    DECLARE date_str VARCHAR(14);
    
    SET date_str = DATE_FORMAT(NOW(), '%Y%m%d%H%i%s');
    
    SELECT COUNT(*) + 1 INTO today_count
    FROM order_info
    WHERE DATE(create_time) = CURDATE();
    
    SET order_no = CONCAT('ORD', date_str, LPAD(today_count, 5, '0'));
END$$

DELIMITER ;

-- ================================================================
-- 触发器
-- ================================================================

-- 删除已存在的触发器
DROP TRIGGER IF EXISTS `tr_article_like_insert`;
DROP TRIGGER IF EXISTS `tr_article_like_delete`;
DROP TRIGGER IF EXISTS `tr_article_collect_insert`;
DROP TRIGGER IF EXISTS `tr_article_collect_delete`;
DROP TRIGGER IF EXISTS `tr_article_comment_insert`;
DROP TRIGGER IF EXISTS `tr_post_like_insert`;
DROP TRIGGER IF EXISTS `tr_post_like_delete`;

DELIMITER $$

-- 文章点赞数更新触发器
CREATE TRIGGER `tr_article_like_insert` 
AFTER INSERT ON `article_like`
FOR EACH ROW 
BEGIN
    UPDATE health_article 
    SET like_count = like_count + 1
    WHERE id = NEW.article_id;
END$$

CREATE TRIGGER `tr_article_like_delete` 
AFTER DELETE ON `article_like`
FOR EACH ROW 
BEGIN
    UPDATE health_article 
    SET like_count = like_count - 1
    WHERE id = OLD.article_id AND like_count > 0;
END$$

-- 文章收藏数更新触发器
CREATE TRIGGER `tr_article_collect_insert` 
AFTER INSERT ON `article_collect`
FOR EACH ROW 
BEGIN
    UPDATE health_article 
    SET collect_count = collect_count + 1
    WHERE id = NEW.article_id;
    
    -- 更新收藏夹文章数
    IF NEW.folder_id IS NOT NULL THEN
        UPDATE article_collect_folder 
        SET article_count = article_count + 1
        WHERE id = NEW.folder_id;
    END IF;
END$$

CREATE TRIGGER `tr_article_collect_delete` 
AFTER DELETE ON `article_collect`
FOR EACH ROW 
BEGIN
    UPDATE health_article 
    SET collect_count = collect_count - 1
    WHERE id = OLD.article_id AND collect_count > 0;
    
    -- 更新收藏夹文章数
    IF OLD.folder_id IS NOT NULL THEN
        UPDATE article_collect_folder 
        SET article_count = article_count - 1
        WHERE id = OLD.folder_id AND article_count > 0;
    END IF;
END$$

-- 文章评论数更新触发器
CREATE TRIGGER `tr_article_comment_insert` 
AFTER INSERT ON `article_comment`
FOR EACH ROW 
BEGIN
    IF NEW.parent_id = 0 THEN
        UPDATE health_article 
        SET comment_count = comment_count + 1
        WHERE id = NEW.article_id;
    ELSE
        UPDATE article_comment 
        SET reply_count = reply_count + 1
        WHERE id = NEW.parent_id;
    END IF;
END$$

-- 动态点赞数更新触发器
CREATE TRIGGER `tr_post_like_insert`
AFTER INSERT ON `like_record`
FOR EACH ROW
BEGIN
    IF NEW.target_type = 1 THEN
        UPDATE post 
        SET like_count = like_count + 1
        WHERE id = NEW.target_id;
    END IF;
END$$

CREATE TRIGGER `tr_post_like_delete`
AFTER DELETE ON `like_record`
FOR EACH ROW
BEGIN
    IF OLD.target_type = 1 THEN
        UPDATE post 
        SET like_count = like_count - 1
        WHERE id = OLD.target_id AND like_count > 0;
    END IF;
END$$

DELIMITER ;

-- ================================================================
-- 视图
-- ================================================================

-- 热门文章视图
CREATE OR REPLACE VIEW `v_hot_articles` AS
SELECT 
    a.id,
    a.title,
    a.author_name,
    a.cover_image,
    a.summary,
    a.view_count,
    a.like_count,
    a.collect_count,
    a.publish_time,
    c.category_name
FROM health_article a
LEFT JOIN article_category c ON a.category_id = c.id
WHERE a.status = 'published' 
    AND a.is_deleted = 0
    AND (a.view_count > 1000 OR a.like_count > 100 OR a.is_hot = 1)
ORDER BY a.view_count DESC, a.like_count DESC;

-- 教练文章统计视图
CREATE OR REPLACE VIEW `v_coach_article_stats` AS
SELECT 
    a.author_id as coach_id,
    a.author_name as coach_name,
    COUNT(DISTINCT a.id) as total_articles,
    SUM(a.view_count) as total_views,
    SUM(a.like_count) as total_likes,
    SUM(a.collect_count) as total_collects,
    AVG(a.like_count) as avg_likes,
    MAX(a.publish_time) as last_publish_time
FROM health_article a
WHERE a.status = 'published' AND a.is_deleted = 0
GROUP BY a.author_id, a.author_name;

-- 教练综合评分视图
CREATE OR REPLACE VIEW `v_coach_rating` AS
SELECT 
    c.id as coach_id,
    c.real_name,
    COUNT(DISTINCT e.id) as total_evaluations,
    AVG(e.overall_rating) as avg_overall_rating,
    AVG(e.professional_rating) as avg_professional_rating,
    AVG(e.attitude_rating) as avg_attitude_rating,
    AVG(e.communication_rating) as avg_communication_rating
FROM coach c
LEFT JOIN coach_evaluation e ON c.id = e.coach_id AND e.status = 1
GROUP BY c.id, c.real_name;

-- ================================================================
-- 索引优化
-- ================================================================

-- 为常用查询添加复合索引
CREATE INDEX idx_coach_status_create_time ON coach(status, create_time);
CREATE INDEX idx_course_schedule_date_status ON course_schedule(start_time, status);
CREATE INDEX idx_order_user_status_create ON order_info(user_id, order_status, create_time);
CREATE INDEX idx_article_status_publish_time ON health_article(status, publish_time);

-- ================================================================
-- 完整测试数据（第二版增强）
-- ================================================================

-- 1. 更新默认角色数据为完整字段
-- 1. 角色数据已在前面第1351-1355行统一插入，这里不再重复

-- 2. 插入基础用户数据（增强版 - 更多用户）
INSERT INTO `user` (`username`, `password`, `nickname`, `email`, `phone`, `gender`, `birth_date`, `status`, `create_time`, `update_time`, `is_deleted`) VALUES
('admin', '$2a$10$7JB720yubVSOfvVmi5hYPesx4XYF.QXC2wh7V2VG5YfFqDnwFv5CC', '系统管理员', 'admin@fitness.com', '13800138000', 1, '1985-06-15', 1, NOW(), NOW(), 0),
('coach001', '$2a$10$7JB720yubVSOfvVmi5hYPesx4XYF.QXC2wh7V2VG5YfFqDnwFv5CC', '李教练', 'coach001@fitness.com', '13800138001', 1, '1990-03-20', 1, NOW(), NOW(), 0),
('coach002', '$2a$10$7JB720yubVSOfvVmi5hYPesx4XYF.QXC2wh7V2VG5YfFqDnwFv5CC', '王教练', 'coach002@fitness.com', '13800138002', 2, '1988-08-10', 1, NOW(), NOW(), 0),
('coach003', '$2a$10$7JB720yubVSOfvVmi5hYPesx4XYF.QXC2wh7V2VG5YfFqDnwFv5CC', '张教练', 'coach003@fitness.com', '13800138003', 1, '1992-11-05', 1, NOW(), NOW(), 0),
('coach004', '$2a$10$7JB720yubVSOfvVmi5hYPesx4XYF.QXC2wh7V2VG5YfFqDnwFv5CC', '刘教练', 'coach004@fitness.com', '13800138004', 2, '1991-04-18', 1, NOW(), NOW(), 0),
('user001', '$2a$10$7JB720yubVSOfvVmi5hYPesx4XYF.QXC2wh7V2VG5YfFqDnwFv5CC', '小明', 'user001@test.com', '13900139001', 1, '1995-01-15', 1, NOW(), NOW(), 0),
('user002', '$2a$10$7JB720yubVSOfvVmi5hYPesx4XYF.QXC2wh7V2VG5YfFqDnwFv5CC', '小红', 'user002@test.com', '13900139002', 2, '1993-07-22', 1, NOW(), NOW(), 0),
('user003', '$2a$10$7JB720yubVSOfvVmi5hYPesx4XYF.QXC2wh7V2VG5YfFqDnwFv5CC', '小刚', 'user003@test.com', '13900139003', 1, '1996-05-18', 1, NOW(), NOW(), 0),
('user004', '$2a$10$7JB720yubVSOfvVmi5hYPesx4XYF.QXC2wh7V2VG5YfFqDnwFv5CC', '小丽', 'user004@test.com', '13900139004', 2, '1994-12-08', 1, NOW(), NOW(), 0),
('user005', '$2a$10$7JB720yubVSOfvVmi5hYPesx4XYF.QXC2wh7V2VG5YfFqDnwFv5CC', '小华', 'user005@test.com', '13900139005', 1, '1997-09-12', 1, NOW(), NOW(), 0),
('user006', '$2a$10$7JB720yubVSOfvVmi5hYPesx4XYF.QXC2wh7V2VG5YfFqDnwFv5CC', '小美', 'user006@test.com', '13900139006', 2, '1998-03-25', 1, NOW(), NOW(), 0),
('user007', '$2a$10$7JB720yubVSOfvVmi5hYPesx4XYF.QXC2wh7V2VG5YfFqDnwFv5CC', '小强', 'user007@test.com', '13900139007', 1, '1992-11-30', 1, NOW(), NOW(), 0);

-- 3. 插入用户角色关联数据
INSERT INTO `user_role` (`user_id`, `role_id`, `create_time`, `is_deleted`) VALUES
(1, (SELECT id FROM role WHERE role_code = 'admin'), NOW(), 0),  -- admin -> 系统管理员
(2, (SELECT id FROM role WHERE role_code = 'coach'), NOW(), 0),  -- coach001 -> 健身教练
(3, (SELECT id FROM role WHERE role_code = 'coach'), NOW(), 0),  -- coach002 -> 健身教练
(4, (SELECT id FROM role WHERE role_code = 'coach'), NOW(), 0),  -- coach003 -> 健身教练
(5, (SELECT id FROM role WHERE role_code = 'coach'), NOW(), 0),  -- coach004 -> 健身教练
(6, (SELECT id FROM role WHERE role_code = 'member'), NOW(), 0),  -- user001 -> 普通用户
(7, (SELECT id FROM role WHERE role_code = 'vip'), NOW(), 0),  -- user002 -> VIP用户
(8, (SELECT id FROM role WHERE role_code = 'member'), NOW(), 0),  -- user003 -> 普通用户
(9, (SELECT id FROM role WHERE role_code = 'vip'), NOW(), 0),  -- user004 -> VIP用户
(10, (SELECT id FROM role WHERE role_code = 'member'), NOW(), 0),  -- user005 -> 普通用户
(11, (SELECT id FROM role WHERE role_code = 'vip'), NOW(), 0),  -- user006 -> VIP用户
(12, (SELECT id FROM role WHERE role_code = 'member'), NOW(), 0);  -- user007 -> 普通用户

-- 4. 插入教练信息数据
-- 插入门店数据
INSERT INTO `gym_store` (`store_name`, `store_code`, `address`, `phone`, `business_hours`, `manager_name`, `manager_phone`, `area_size`, `equipment_count`, `max_capacity`, `parking_spaces`, `description`, `latitude`, `longitude`, `status`, `create_time`, `update_time`, `is_deleted`) VALUES
('总店（市中心店）', 'STORE001', '北京市朝阳区建国路88号SOHO现代城B座1层', '010-85551234', '06:00-23:00', '张经理', '13800001001', 1200, 80, 200, 50, '旗舰店，设施齐全，位置优越', 39.913818, 116.437972, 1, NOW(), NOW(), 0),
('西城分店', 'STORE002', '北京市西城区西单大街123号', '010-85551235', '06:30-22:30', '李经理', '13800001002', 800, 50, 120, 20, '西城区分店，交通便利', 39.906901, 116.373244, 1, NOW(), NOW(), 0),
('海淀分店', 'STORE003', '北京市海淀区中关村大街456号', '010-85551236', '07:00-22:00', '王经理', '13800001003', 600, 40, 100, 15, '海淀区分店，学生优惠', 39.983424, 116.319142, 1, NOW(), NOW(), 0);

-- 4. 插入教练信息数据
INSERT INTO `coach` (`user_id`, `real_name`, `certification_no`, `specialties`, `introduction`, `experience_years`, `rating`, `status`, `create_time`, `update_time`, `is_deleted`) VALUES
(2, '李强', 'CERT001', '力量训练,减脂塑形', '资深健身教练，10年教学经验，擅长力量训练和减脂塑形。', 10, 4.8, 1, NOW(), NOW(), 0),
(3, '王丽', 'CERT002', '瑜伽,普拉提', '瑜伽高级教练，精通各类瑜伽体式，专注身心健康。', 8, 4.9, 1, NOW(), NOW(), 0),
(4, '张伟', 'CERT003', '有氧训练,康复训练', '运动康复专家，帮助学员科学训练，预防运动伤害。', 6, 4.7, 1, NOW(), NOW(), 0),
(5, '刘芳', 'CERT004', '舞蹈,形体', '专业舞蹈教练，擅长各类舞蹈教学，形体塑造专家。', 5, 4.6, 1, NOW(), NOW(), 0),
(1, '管理员', 'ADMIN001', '综合管理', '系统管理员账户', 5, 5.0, 1, NOW(), NOW(), 0);

-- 插入教练门店关联数据
INSERT INTO `coach_store_relation` (`coach_id`, `store_id`, `is_primary`, `start_date`, `end_date`, `status`, `create_time`, `update_time`, `is_deleted`) VALUES
(2, 1, 1, '2025-01-01', NULL, 1, NOW(), NOW(), 0),  -- 李教练主要在总店
(2, 2, 0, '2025-01-01', NULL, 1, NOW(), NOW(), 0),  -- 李教练也在西城分店工作
(3, 1, 1, '2025-01-01', NULL, 1, NOW(), NOW(), 0),  -- 王教练主要在总店
(4, 2, 1, '2025-01-01', NULL, 1, NOW(), NOW(), 0),  -- 张教练主要在西城分店
(4, 3, 0, '2025-01-01', NULL, 1, NOW(), NOW(), 0),  -- 张教练也在海淀分店工作
(5, 3, 1, '2025-01-01', NULL, 1, NOW(), NOW(), 0);  -- 刘教练主要在海淀分店

-- 5. 插入健康档案数据
INSERT INTO `health_record` (`user_id`, `height`, `weight`, `bmi`, `body_fat_rate`, `muscle_rate`, `basal_metabolism`, `health_goal`, `medical_history`, `allergies`, `create_time`, `update_time`, `is_deleted`) VALUES
(6, 175.0, 70.0, 22.86, 15.5, 45.2, 1650, '增肌减脂，提高体能', '无重大病史', '无已知过敏', NOW(), NOW(), 0),
(7, 160.0, 55.0, 21.48, 20.8, 38.5, 1320, '塑形美体，保持健康', '无重大病史', '花粉过敏', NOW(), NOW(), 0),
(8, 180.0, 75.0, 23.15, 12.3, 48.7, 1780, '力量提升，肌肉增长', '膝关节曾受伤', '无已知过敏', NOW(), NOW(), 0),
(9, 165.0, 52.0, 19.11, 22.1, 35.8, 1280, '减重塑形，改善体态', '无重大病史', '海鲜过敏', NOW(), NOW(), 0),
(10, 170.0, 65.0, 22.49, 18.2, 42.1, 1550, '健康维护，提升体质', '无重大病史', '无已知过敏', NOW(), NOW(), 0),
(11, 162.0, 50.0, 19.05, 25.3, 33.2, 1250, '减脂瘦身，增强体质', '轻度贫血', '牛奶过敏', NOW(), NOW(), 0),
(12, 178.0, 80.0, 25.25, 20.5, 46.8, 1820, '减重增肌，改善健康', '高血压家族史', '无已知过敏', NOW(), NOW(), 0);

-- 6. 插入体测记录数据
INSERT INTO `body_measurement` (`user_id`, `measure_date`, `height`, `weight`, `bmi`, `body_fat_rate`, `muscle_mass`, `visceral_fat`, `waist`, `hip`, `chest`, `arm`, `thigh`, `create_time`, `is_deleted`) VALUES
(6, '2025-09-01', 175.0, 70.5, 23.02, 15.8, 32.5, 3, 82.0, 95.0, 95.0, 32.0, 55.0, NOW(), 0),
(6, '2025-09-15', 175.0, 70.0, 22.86, 15.5, 33.0, 3, 81.5, 95.0, 96.0, 32.5, 55.5, NOW(), 0),
(7, '2025-09-01', 160.0, 55.5, 21.68, 21.2, 20.5, 2, 68.0, 88.0, 85.0, 25.0, 50.0, NOW(), 0),
(7, '2025-09-15', 160.0, 55.0, 21.48, 20.8, 21.0, 2, 67.5, 87.5, 86.0, 25.5, 50.5, NOW(), 0),
(8, '2025-09-01', 180.0, 75.5, 23.30, 12.8, 36.5, 4, 85.0, 98.0, 102.0, 35.0, 58.0, NOW(), 0),
(8, '2025-09-15', 180.0, 75.0, 23.15, 12.3, 37.0, 4, 84.5, 98.0, 103.0, 35.5, 58.5, NOW(), 0),
(9, '2025-09-01', 165.0, 52.5, 19.28, 22.5, 18.2, 1, 65.0, 85.0, 82.0, 23.0, 48.0, NOW(), 0),
(9, '2025-09-15', 165.0, 52.0, 19.11, 22.1, 18.5, 1, 64.5, 84.5, 83.0, 23.5, 48.5, NOW(), 0),
(10, '2025-09-01', 170.0, 65.5, 22.66, 18.5, 24.8, 2, 75.0, 92.0, 88.0, 28.0, 52.0, NOW(), 0),
(10, '2025-09-15', 170.0, 65.0, 22.49, 18.2, 25.2, 2, 74.5, 91.5, 89.0, 28.5, 52.5, NOW(), 0),
(11, '2025-09-01', 162.0, 50.5, 19.24, 25.6, 16.8, 1, 62.0, 83.0, 80.0, 22.0, 46.0, NOW(), 0),
(11, '2025-09-15', 162.0, 50.0, 19.05, 25.3, 17.0, 1, 61.5, 82.5, 81.0, 22.5, 46.5, NOW(), 0);

-- 7. 插入更多课程数据
INSERT INTO `course` (`course_name`, `category_id`, `coach_id`, `cover_image`, `description`, `difficulty`, `duration`, `max_participants`, `price`, `status`, `create_time`, `update_time`, `is_deleted`) VALUES
('高强度间歇训练(HIIT)', 1, 2, '/images/course/hiit.jpg', '高效燃脂的间歇训练课程', 3, 45, 20, 88.00, 1, NOW(), NOW(), 0),
('力量基础训练', 2, 2, '/images/course/strength.jpg', '适合初学者的力量训练', 1, 60, 15, 98.00, 1, NOW(), NOW(), 0),
('哈他瑜伽', 3, 3, '/images/course/yoga.jpg', '传统瑜伽，适合所有水平', 2, 75, 25, 78.00, 1, NOW(), NOW(), 0),
('拉丁舞基础', 4, 5, '/images/course/latin.jpg', '充满活力的拉丁舞蹈', 2, 60, 30, 68.00, 1, NOW(), NOW(), 0),
('功能性训练', 2, 4, '/images/course/functional.jpg', '提升日常活动能力的训练', 2, 50, 18, 85.00, 1, NOW(), NOW(), 0),
('流瑜伽', 3, 3, '/images/course/flow_yoga.jpg', '动态瑜伽，增强柔韧性', 2, 60, 20, 75.00, 1, NOW(), NOW(), 0);

-- 8. 插入课程排期数据（修复字段结构，添加门店信息）
INSERT INTO `course_schedule` (`course_id`, `coach_id`, `store_id`, `start_time`, `end_time`, `room_location`, `current_participants`, `status`, `create_time`, `is_deleted`) VALUES
(1, 2, 1, '2025-09-27 09:00:00', '2025-09-27 09:45:00', '健身房A区', 8, 1, NOW(), 0),
(1, 2, 1, '2025-09-28 19:00:00', '2025-09-28 19:45:00', '健身房A区', 12, 1, NOW(), 0),
(2, 2, 1, '2025-09-27 14:00:00', '2025-09-27 15:00:00', '力量训练区', 6, 1, NOW(), 0),
(2, 2, 2, '2025-09-29 10:00:00', '2025-09-29 11:00:00', '力量训练区', 9, 1, NOW(), 0),
(3, 3, 1, '2025-09-27 08:00:00', '2025-09-27 09:15:00', '瑜伽室1', 15, 1, NOW(), 0),
(3, 3, 1, '2025-09-28 18:00:00', '2025-09-28 19:15:00', '瑜伽室1', 18, 1, NOW(), 0),
(4, 5, 3, '2025-09-27 20:00:00', '2025-09-27 21:00:00', '舞蹈室', 12, 1, NOW(), 0),
(4, 5, 3, '2025-09-29 19:00:00', '2025-09-29 20:00:00', '舞蹈室', 14, 1, NOW(), 0),
(5, 4, 2, '2025-09-28 10:00:00', '2025-09-28 10:50:00', '功能训练区', 10, 1, NOW(), 0),
(6, 3, 1, '2025-09-29 08:00:00', '2025-09-29 09:00:00', '瑜伽室2', 16, 1, NOW(), 0);

-- 9. 插入课程报名数据
INSERT INTO `course_enrollment` (`user_id`, `schedule_id`, `enrollment_time`, `status`, `is_deleted`) VALUES
(6, 1, NOW(), 1, 0),  -- 小明报名HIIT
(6, 3, NOW(), 1, 0),  -- 小明报名力量训练
(7, 5, NOW(), 1, 0),  -- 小红报名瑜伽
(7, 7, NOW(), 1, 0),  -- 小红报名拉丁舞
(8, 1, NOW(), 1, 0),  -- 小刚报名HIIT
(8, 4, NOW(), 1, 0),  -- 小刚报名力量训练
(9, 5, NOW(), 1, 0),  -- 小丽报名瑜伽
(9, 6, NOW(), 1, 0),  -- 小丽报名瑜伽
(10, 2, NOW(), 1, 0),  -- 小华报名HIIT
(11, 6, NOW(), 1, 0),  -- 小美报名瑜伽
(12, 9, NOW(), 1, 0),  -- 小强报名功能性训练
(12, 10, NOW(), 1, 0);  -- 小强报名流瑜伽

-- 10. 插入运动计划数据
INSERT INTO `exercise_plan` (`user_id`, `plan_name`, `goal`, `start_date`, `end_date`, `status`, `create_time`, `is_deleted`) VALUES
(6, '初级增肌计划', '增肌塑形', '2025-09-01', '2025-11-30', 1, NOW(), 0),
(7, '瑜伽减脂计划', '减脂塑形', '2025-09-01', '2025-10-31', 1, NOW(), 0),
(8, '力量提升计划', '力量提升', '2025-09-01', '2025-12-31', 1, NOW(), 0),
(9, '新手入门计划', '健康体验', '2025-09-01', '2025-10-15', 1, NOW(), 0),
(10, '综合健身计划', '全面提升', '2025-09-01', '2025-12-31', 1, NOW(), 0),
(11, '减脂塑形计划', '减重美体', '2025-09-01', '2025-11-30', 1, NOW(), 0);

-- 11. 插入运动记录数据
INSERT INTO `exercise_record` (`user_id`, `exercise_date`, `exercise_type`, `duration`, `distance`, `calories`, `notes`, `create_time`, `is_deleted`) VALUES
(6, '2025-09-26', '卧推', 20, NULL, 180, '今天状态不错', NOW(), 0),
(6, '2025-09-25', '深蹲', 25, NULL, 220, '腿部训练', NOW(), 0),
(7, '2025-09-26', '瑜伽流', 60, NULL, 150, '身心放松', NOW(), 0),
(7, '2025-09-24', '跑步机', 30, 5.5, 280, '有氧训练', NOW(), 0),
(8, '2025-09-26', '硬拉', 15, NULL, 200, '背部训练', NOW(), 0),
(8, '2025-09-25', '深蹲', 30, NULL, 350, '大重量训练', NOW(), 0),
(9, '2025-09-26', '快走', 40, 3.2, 160, '轻松有氧', NOW(), 0),
(9, '2025-09-24', '伸展运动', 20, NULL, 80, '柔韧性训练', NOW(), 0),
(10, '2025-09-26', '游泳', 45, 1.5, 320, '全身有氧运动', NOW(), 0),
(11, '2025-09-26', '普拉提', 50, NULL, 140, '核心训练', NOW(), 0),
(12, '2025-09-26', '跑步', 35, 6.0, 350, '晨跑锻炼', NOW(), 0);

-- 12. 插入饮食记录数据
INSERT INTO `diet_record` (`user_id`, `record_date`, `meal_type`, `food_name`, `quantity`, `calories`, `create_time`, `is_deleted`) VALUES
(6, '2025-09-26', 1, '鸡胸肉', 150, 165, NOW(), 0),
(6, '2025-09-26', 1, '糙米饭', 100, 116, NOW(), 0),
(7, '2025-09-26', 1, '鸡蛋', 60, 87, NOW(), 0),
(7, '2025-09-26', 2, '苹果', 150, 78, NOW(), 0),
(8, '2025-09-26', 1, '牛肉', 120, 250, NOW(), 0),
(8, '2025-09-26', 3, '鸡胸肉沙拉', 200, 180, NOW(), 0),
(9, '2025-09-26', 1, '燕麦粥', 80, 68, NOW(), 0),
(9, '2025-09-26', 2, '香蕉', 100, 93, NOW(), 0),
(10, '2025-09-26', 1, '全麦面包', 80, 210, NOW(), 0),
(11, '2025-09-26', 1, '酸奶', 200, 110, NOW(), 0),
(12, '2025-09-26', 2, '坚果', 30, 180, NOW(), 0);

-- 13. 插入健康文章数据（增强版）
INSERT INTO `health_article` (`article_no`, `title`, `subtitle`, `author_id`, `author_name`, `category_id`, `cover_image`, `summary`, `content`, `tags`, `keywords`, `reading_time`, `difficulty_level`, `status`, `publish_time`, `view_count`, `like_count`, `collect_count`, `is_top`, `is_recommend`, `create_time`, `update_time`, `is_deleted`) VALUES
('ART2025092700001', '初学者如何开始力量训练', '从零开始的力量训练指南', 2, '李强', 1, '/images/article/strength_beginner.jpg', '适合初学者的力量训练基础知识和方法', '力量训练是健身的重要组成部分...', '["力量训练", "初学者", "健身"]', '力量训练,初学者,健身指南', 8, 'beginner', 'published', '2025-09-20 10:00:00', 1250, 86, 45, 1, 1, NOW(), NOW(), 0),
('ART2025092700002', '瑜伽的身心益处', '探索瑜伽对身体和心理的好处', 3, '王丽', 3, '/images/article/yoga_benefits.jpg', '全面介绍瑜伽对身体和心理健康的积极影响', '瑜伽不仅仅是一种运动...', '["瑜伽", "心理健康", "身体健康"]', '瑜伽,心理健康,冥想', 6, 'beginner', 'published', '2025-09-22 14:30:00', 980, 124, 67, 0, 1, NOW(), NOW(), 0),
('ART2025092700003', '科学减脂的方法和误区', '避免减脂误区，科学健康的减脂', 2, '李强', 2, '/images/article/weight_loss.jpg', '分享科学有效的减脂方法，避免常见误区', '减脂是很多人关心的话题...', '["减脂", "营养", "运动"]', '减脂,营养,健康饮食', 10, 'intermediate', 'published', '2025-09-25 09:15:00', 1580, 203, 89, 1, 1, NOW(), NOW(), 0),
('ART2025092700004', '运动伤害的预防和处理', '如何预防运动伤害并正确处理', 4, '张伟', 4, '/images/article/injury_prevention.jpg', '让你了解如何预防运动伤害并学会正确处理', '运动伤害是运动者常遇到的问题...', '["运动伤害", "预防", "康复"]', '运动伤害,预防,康复训练', 12, 'intermediate', 'published', '2025-09-23 16:45:00', 756, 92, 38, 0, 1, NOW(), NOW(), 0),
('ART2025092700005', '舞蹈健身的魅力与技巧', '通过舞蹈享受健身的乐趣', 5, '刘芳', 1, '/images/article/dance_fitness.jpg', '探讨舞蹈健身的独特魅力和基本技巧', '舞蹈是一种充满乐趣的健身方式...', '["舞蹈", "健身", "乐趣"]', '舞蹈健身,拉丁舞,形体训练', 7, 'beginner', 'published', '2025-09-21 11:20:00', 625, 78, 32, 0, 1, NOW(), NOW(), 0),
('ART2025092700006', '营养补充剂使用指南', '如何科学合理地使用营养补充剂', 2, '李强', 2, '/images/article/supplements.jpg', '详细介绍各类营养补充剂的作用和使用方法', '营养补充剂可以帮助我们更好地达成健身目标...', '["营养补充剂", "蛋白粉", "维生素"]', '营养补充剂,蛋白粉,健身营养', 9, 'intermediate', 'published', '2025-09-24 15:30:00', 892, 115, 56, 0, 1, NOW(), NOW(), 0);

-- 14. 插入文章点赞数据
INSERT INTO `article_like` (`user_id`, `article_id`, `create_time`, `is_deleted`) VALUES
(6, 1, NOW(), 0),
(6, 3, NOW(), 0),
(7, 1, NOW(), 0),
(7, 2, NOW(), 0),
(8, 1, NOW(), 0),
(8, 3, NOW(), 0),
(9, 2, NOW(), 0),
(9, 4, NOW(), 0),
(10, 1, NOW(), 0),
(10, 5, NOW(), 0),
(11, 2, NOW(), 0),
(12, 3, NOW(), 0);

-- 15. 插入文章收藏数据
INSERT INTO `article_collect` (`user_id`, `article_id`, `folder_id`, `create_time`, `is_deleted`) VALUES
(6, 1, NULL, NOW(), 0),
(6, 3, NULL, NOW(), 0),
(7, 2, NULL, NOW(), 0),
(7, 4, NULL, NOW(), 0),
(8, 1, NULL, NOW(), 0),
(8, 4, NULL, NOW(), 0),
(9, 2, NULL, NOW(), 0),
(9, 3, NULL, NOW(), 0),
(10, 5, NULL, NOW(), 0),
(11, 6, NULL, NOW(), 0);

-- 16. 插入教练可用时间数据
INSERT INTO `coach_availability` (`coach_id`, `day_of_week`, `start_time`, `end_time`, `is_available`, `create_time`, `is_deleted`) VALUES
(2, 1, '09:00:00', '18:00:00', 1, NOW(), 0), -- 李教练周一
(2, 2, '09:00:00', '18:00:00', 1, NOW(), 0), -- 李教练周二
(2, 3, '14:00:00', '20:00:00', 1, NOW(), 0), -- 李教练周三
(2, 5, '09:00:00', '18:00:00', 1, NOW(), 0), -- 李教练周五
(3, 1, '08:00:00', '16:00:00', 1, NOW(), 0), -- 王教练周一
(3, 3, '08:00:00', '16:00:00', 1, NOW(), 0), -- 王教练周三
(3, 4, '10:00:00', '20:00:00', 1, NOW(), 0), -- 王教练周四
(3, 6, '08:00:00', '16:00:00', 1, NOW(), 0), -- 王教练周六
(4, 2, '10:00:00', '18:00:00', 1, NOW(), 0), -- 张教练周二
(4, 4, '09:00:00', '17:00:00', 1, NOW(), 0), -- 张教练周四
(5, 1, '18:00:00', '22:00:00', 1, NOW(), 0), -- 刘教练周一晚
(5, 3, '18:00:00', '22:00:00', 1, NOW(), 0); -- 刘教练周三晚

-- 17. 插入食物数据库
INSERT INTO `food_database` (`food_name`, `category`, `calories`, `protein`, `fat`, `carbohydrate`, `fiber`, `sodium`, `create_time`, `is_deleted`) VALUES
('鸡胸肉', '肉类', 165.00, 31.00, 3.60, 0.00, 0.00, 74.00, NOW(), 0),
('牛肉', '肉类', 250.00, 26.00, 15.00, 0.00, 0.00, 78.00, NOW(), 0),
('鸡蛋', '蛋类', 155.00, 13.00, 11.00, 1.10, 0.00, 124.00, NOW(), 0),
('糙米', '谷物', 370.00, 7.70, 2.90, 77.20, 3.50, 7.00, NOW(), 0),
('燕麦', '谷物', 389.00, 16.90, 6.90, 66.30, 10.60, 2.00, NOW(), 0),
('苹果', '水果', 52.00, 0.30, 0.20, 13.80, 2.40, 1.00, NOW(), 0),
('香蕉', '水果', 89.00, 1.10, 0.30, 22.80, 2.60, 1.00, NOW(), 0),
('西兰花', '蔬菜', 34.00, 2.80, 0.40, 7.00, 2.60, 33.00, NOW(), 0);

-- 18. 插入动态数据
INSERT INTO `post` (`user_id`, `content`, `images`, `location`, `visibility`, `like_count`, `comment_count`, `share_count`, `status`, `create_time`, `is_deleted`) VALUES
(6, '今天完成了第一次HIIT训练，感觉超棒！💪', '["post1.jpg", "post2.jpg"]', '健身房A区', 1, 5, 2, 1, 1, NOW(), 0),
(7, '瑜伽课后的放松时光，身心都得到了很好的舒展。', '["yoga1.jpg"]', '瑜伽室1', 1, 8, 3, 0, 1, NOW(), 0),
(8, '今天深蹲破了个人纪录，继续加油！🏋️‍♂️', NULL, '力量训练区', 1, 12, 5, 2, 1, NOW(), 0),
(9, '晨跑5公里完成，新的一天充满活力！', '["run1.jpg"]', '公园', 1, 6, 1, 1, 1, NOW(), 0);

-- 19. 插入支付记录数据
INSERT INTO `payment_record` (`order_id`, `pay_no`, `pay_channel`, `pay_amount`, `pay_status`, `callback_time`, `create_time`, `is_deleted`) VALUES
(1, 'PAY2025092700001', 'alipay', 88.00, 1, '2025-09-27 10:15:05', NOW(), 0),
(2, 'PAY2025092700002', 'wechat', 98.00, 1, '2025-09-27 14:20:03', NOW(), 0),
(3, 'PAY2025092700003', 'alipay', 78.00, 1, '2025-09-27 16:30:02', NOW(), 0),
(4, 'PAY2025092700004', 'wechat', 68.00, 1, '2025-09-27 18:45:08', NOW(), 0);

-- 20. 插入教练服务项目数据
INSERT INTO `coach_service` (`coach_id`, `service_name`, `service_type`, `description`, `duration`, `price`, `max_clients`, `status`, `create_time`, `is_deleted`) VALUES
(2, '线下私人训练指导', 'offline_training', '一对一力量训练指导，制定个性化训练计划', 60, 500.00, 1, 1, NOW(), 0),
(3, '线下瑜伽私教', 'offline_training', '个性化瑜伽指导，针对性解决身体问题', 75, 450.00, 1, 1, NOW(), 0),
(4, '线上文字咨询-运动康复', 'online_text', '运动伤害评估和康复建议', 25, 48.00, 1, 1, NOW(), 0),
(5, '线下舞蹈编排服务', 'offline_training', '专业舞蹈编排和教学', 90, 600.00, 1, 1, NOW(), 0),
(2, '线上文字咨询-健身指导', 'online_text', '在线解答健身问题，提供训练建议', 25, 48.00, 1, 1, NOW(), 0),
(3, '线上文字咨询-瑜伽指导', 'online_text', '在线瑜伽问题咨询和体式指导', 25, 48.00, 1, 1, NOW(), 0);

-- ================================================================
-- 教练认证申请测试数据（coach_certification_apply表）
-- ================================================================
INSERT INTO `coach_certification_apply` (`user_id`, `real_name`, `id_card`, `phone`, `email`, `specialties`, `experience_years`, `introduction`, `certificates`, `education`, `work_experience`, `status`, `apply_time`, `review_time`, `reviewer_id`, `review_remark`, `certification_no`, `create_time`, `is_deleted`) VALUES
-- 待审核申请
(6, '王健', '110101199001011234', '13900139006', 'wangjian@fitness.com', '力量训练,健身指导', 3, '具有3年健身指导经验，擅长力量训练和体能提升', '[{"name":"国家健身教练职业资格证","issuer":"国家体育总局","date":"2022-05-15","level":"中级"}]', '[{"school":"体育大学","major":"运动训练","degree":"学士","year":"2020"}]', '[{"company":"XX健身俱乐部","position":"健身教练","duration":"2020-2023"}]', 'pending', '2025-10-08 14:30:00', NULL, NULL, NULL, NULL, NOW(), 0),

-- 已通过申请
(7, '李娜', '110101198809151234', '13900139007', 'lina@fitness.com', '瑜伽,普拉提', 5, '瑜伽高级教练，拥有多项国际认证', '[{"name":"RYT-200小时瑜伽教练认证","issuer":"Yoga Alliance","date":"2020-03-20"},{"name":"普拉提教练认证","issuer":"BASI","date":"2021-08-10"}]', '[{"school":"舞蹈学院","major":"舞蹈表演","degree":"学士","year":"2018"}]', '[{"company":"瑜伽工作室","position":"主教练","duration":"2018-2023"}]', 'approved', '2025-10-01 10:15:00', '2025-10-03 16:20:00', 1, '资质齐全，经验丰富，予以通过', 'COACH2025100300001', NOW(), 0),

-- 已拒绝申请
(8, '张强', '110101199205101234', '13900139008', 'zhangqiang@fitness.com', '力量训练', 1, '刚入行健身教练，希望能够获得认证', '[{"name":"健身教练初级证书","issuer":"健身协会","date":"2025-08-15"}]', '[{"school":"普通大学","major":"计算机","degree":"学士","year":"2021"}]', '[{"company":"健身房","position":"实习教练","duration":"2025-06-2025-09"}]', 'rejected', '2025-09-25 09:00:00', '2025-09-28 14:30:00', 1, '经验不足，建议再积累一年教学经验后重新申请', NULL, NOW(), 0),

-- 最新待审核申请
(9, '刘芳芳', '110101199412251234', '13900139009', 'liufangfang@fitness.com', '舞蹈健身,形体训练', 4, '专业舞蹈演员转型健身教练，擅长舞蹈健身和形体塑造', '[{"name":"中国舞蹈家协会教师资格证","issuer":"中国舞蹈家协会","date":"2019-06-20"},{"name":"健身教练国职证书","issuer":"国家体育总局","date":"2024-11-05"}]', '[{"school":"艺术学院","major":"舞蹈表演","degree":"学士","year":"2016"}]', '[{"company":"艺术团","position":"舞蹈演员","duration":"2016-2020"},{"company":"健身工作室","position":"舞蹈教练","duration":"2020-2024"}]', 'pending', '2025-10-10 11:20:00', NULL, NULL, NULL, NULL, NOW(), 0);

-- ================================================================
-- 教练离职申请测试数据（coach_resignation_apply表）
-- ================================================================
INSERT INTO `coach_resignation_apply` (`coach_id`, `resignation_date`, `reason`, `handover_plan`, `status`, `apply_time`, `review_time`, `reviewer_id`, `review_remark`, `actual_leave_date`, `create_time`, `is_deleted`) VALUES
-- 待审核离职申请
(2, '2025-12-01', '个人发展规划：计划出国深造，攻读运动科学硕士学位，提升专业水平。希望公司能够批准我的离职申请。', '1. 整理所有学员资料，制作详细档案交接给其他教练\n2. 协助安排现有学员的后续课程安排\n3. 完成本月所有课程教学\n4. 培训接替教练相关专业技能', 'pending', '2025-10-08 14:30:00', NULL, NULL, NULL, NULL, NOW(), 0),

-- 已批准离职申请
(4, '2025-11-15', '家庭原因：需要照顾生病的父母，无法继续全职从事教练工作。经过深思熟虑，决定申请离职。', '1. 与现有学员逐一沟通，推荐合适的替代教练\n2. 整理康复训练相关资料和案例\n3. 协助培训新教练康复训练技能\n4. 完成所有在进行的康复指导项目', 'approved', '2025-10-01 10:15:00', '2025-10-05 09:30:00', 1, '理解您的家庭情况，同意离职申请。感谢您为公司做出的贡献。', '2025-11-15', NOW(), 0),

-- 已拒绝离职申请
(3, '2025-10-30', '工作压力较大，希望能够换个环境发展，寻找更合适的工作机会。', '1. 完成当前瑜伽课程的教学\n2. 整理学员反馈和改进建议\n3. 协助招聘和培训新的瑜伽教练', 'rejected', '2025-09-20 11:00:00', '2025-09-25 16:45:00', 1, '目前瑜伽课程正值旺季，学员反馈良好，建议调整工作安排缓解压力，暂不批准离职。', NULL, NOW(), 0),

-- 已取消离职申请
(5, '2025-11-30', '考虑转行做全职舞蹈演员，重返演艺圈发展。', '1. 完成舞蹈课程的季度教学计划\n2. 培养助教独立授课能力\n3. 整理舞蹈教学视频资料', 'cancelled', '2025-09-15 10:20:00', NULL, NULL, NULL, NULL, NOW(), 0);

-- ================================================================
-- 教练咨询记录测试数据（coach_consultation表）
-- ================================================================
INSERT INTO `coach_consultation` (`coach_id`, `user_id`, `service_type`, `consultation_type`, `consultation_date`, `duration`, `topic`, `content`, `coach_advice`, `status`, `create_time`, `is_deleted`) VALUES
-- 已完成的在线咨询 (线上文字咨询)
(2, 6, 'online_text', 'online', '2025-10-08 14:00:00', 25, '增肌训练计划咨询', '小明希望在3个月内增加肌肉量，目前身高175cm，体重70kg，有一定健身基础。询问具体的训练计划和饮食建议。', '建议采用分化训练，一周练4次，重点加强复合动作。饮食方面每天摄入蛋白质按体重2g/kg计算，碳水化合物可以适量增加。建议每周测量一次体重和围度变化。', 'completed', NOW(), 0),

-- 已预约的线下私教
(3, 7, 'offline_training', 'offline', '2025-10-12 10:30:00', 60, '瑜伽体式纠正咨询', '小红在练习瑜伽时经常感到腰部不适，希望教练能够现场指导正确的体式和呼吸方法。', NULL, 'booked', NOW(), 0),

-- 已完成的康复咨询 (线上文字咨询)
(4, 8, 'online_text', 'offline', '2025-10-06 15:30:00', 25, '肩关节康复咨询', '小刚之前运动时肩膀受伤，现在基本康复，但还是有些担心。希望了解后续的康复训练和预防措施。', '肩关节已基本恢复，建议继续进行肩胛骨稳定性训练，避免高强度推举动作。可以逐步增加弹力带训练，每周2-3次。注意运动前充分热身。', 'completed', NOW(), 0),

-- 已取消的在线咨询
(5, 9, 'online_text', 'online', '2025-10-10 19:00:00', 25, '舞蹈基础入门咨询', '小丽想学习拉丁舞，但完全没有基础，希望了解入门的方法和注意事项。', NULL, 'cancelled_by_user', NOW(), 0),

-- 进行中的综合咨询
(2, 10, 'online_text', 'online', '2025-10-09 20:00:00', 25, '健身计划制定咨询', '小华想制定一个综合的健身计划，目标是减脂同时保持肌肉量，工作比较忙，时间有限。', '考虑到时间限制，建议采用HIIT训练结合力量训练，一周3次，每次60分钟。饮食控制是关键，建议轻微热量赤字。', 'completed', NOW(), 0),

-- 最新预约的瑜伽咨询 (线下私教)
(3, 11, 'offline_training', 'offline', '2025-10-15 09:00:00', 75, '孕期瑜伽安全咨询', '小美怀孕4个月，之前有瑜伽基础，想了解孕期可以继续练习的瑜伽体式和注意事项。', NULL, 'booked', NOW(), 0);

-- ================================================================
-- 教练评价测试数据（coach_evaluation表）
-- ================================================================
INSERT INTO `coach_evaluation` (`coach_id`, `user_id`, `service_id`, `order_id`, `overall_rating`, `professional_rating`, `attitude_rating`, `communication_rating`, `content`, `tags`, `images`, `status`, `create_time`, `is_deleted`) VALUES
-- 小明对李教练的评价
(2, 6, 1, 5, 4.8, 5.0, 4.5, 4.8, '李教练非常专业，能够针对我的身体情况制定个性化的训练计划。动作指导很到位，每个细节都会提醒注意。课后还会主动跟进我的训练进度，非常负责任。就是有时候要求比较严格，但这样效果也更好。', '专业,负责,耐心,严格', NULL, 1, NOW(), 0),

-- 小红对王教练的评价
(3, 7, 2, 7, 4.9, 4.8, 5.0, 5.0, '王老师的瑜伽课让我受益匹浅！不仅仅是体式指导，更重要的是她教会了我如何通过瑜伽调节身心状态。每一个动作都讲解得非常清楚，还会结合呼吸法进行指导。课堂氛围轻松愉快，老师性格温和，可以感受到她的专业和爱心。', '专业,温和,细致,有爱心', '["yoga_class1.jpg", "yoga_pose1.jpg"]', 1, NOW(), 0),

-- 小刚对张教练的评价
(4, 8, 3, 8, 4.7, 4.9, 4.5, 4.6, '张教练在运动康复方面非常专业，对我之前的肩伤情况了解很深入。制定的康复计划很科学，每个阶段的训练强度都挹到好处。不过有时候解释比较专业，我需要多问几次才能理解。总体来说很满意，现在肩膀恭复得很好。', '专业,科学,有效,耐心', NULL, 1, NOW(), 0),

-- 小丽对刘教练的评价
(5, 9, 4, 9, 4.6, 4.4, 4.8, 4.6, '刘老师的舞蹈课真的很棒！作为一个完全没有舞蹈基础的人，她耐心地从最基础的动作开始教。课堂气氛很活跃，音乐搜配得很好。不过有时候动作要求比较高，我跟不上节奏时会有些紧张。希望以后可以根据学员水平调整一下课程难度。', '活跃,耐心,专业,笑容甜美', '["dance_class1.jpg"]', 1, NOW(), 0),

-- 小华对李教练的评价（第二次）
(2, 10, 1, 12, 4.9, 5.0, 4.8, 5.0, '这是第二次找李教练了，上次的训练计划执行得很好，这次来调整和优化。李教练记得我上次的训练情况，还主动问了我最近的身体反应和效果。新的计划更加合理，考虑到了我工作时间的变化。非常满意，会继续选择李教练。', '专业,細心,跟进及时,值得信赖', NULL, 1, NOW(), 0),

-- 小美对王教练的评价（孕期瑜伽）
(3, 11, 2, 15, 5.0, 4.9, 5.0, 5.0, '作为一个孕妇，我对运动安全性特别关注。王老师在孕期瑜伽方面经验非常丰富，不仅帮我选择了适合的体式，还结合我的孕期阶段给出了很多实用的建议。整个课程节奏缓慢，非常放松，让我和宝宝都很舒服。强烈推荐给其他孕妈妈！', '专业,温柔,负责,经验丰富', '["prenatal_yoga1.jpg", "prenatal_yoga2.jpg"]', 1, NOW(), 0);

-- ================================================================
-- 教练收入记录测试数据（coach_income表）
-- ================================================================
INSERT INTO `coach_income` (`coach_id`, `income_type`, `source_id`, `income_amount`, `commission_rate`, `actual_amount`, `income_date`, `settlement_status`, `settlement_date`, `remark`, `create_time`, `is_deleted`) VALUES
-- 李教练的私教收入
(2, 'service', 5, 200.00, 60.00, 120.00, '2025-10-08', 'settled', '2025-10-15', '小明私人训练指导课程', NOW(), 0),
(2, 'service', 12, 200.00, 60.00, 120.00, '2025-10-09', 'settled', '2025-10-15', '小华第二次私教课程', NOW(), 0),
(2, 'course', 1, 88.00, 50.00, 44.00, '2025-10-10', 'pending', NULL, 'HIIT课程授课收入', NOW(), 0),
(2, 'bonus', NULL, 500.00, 100.00, 500.00, '2025-10-05', 'settled', '2025-10-15', '月度优秀教练奖金', NOW(), 0),

-- 王教练的瑜伽收入
(3, 'service', 7, 180.00, 55.00, 99.00, '2025-10-06', 'settled', '2025-10-15', '小红瑜伽私教课程', NOW(), 0),
(3, 'service', 15, 180.00, 55.00, 99.00, '2025-10-07', 'pending', NULL, '小美孕期瑜伽私教', NOW(), 0),
(3, 'course', 3, 78.00, 45.00, 35.10, '2025-10-08', 'pending', NULL, '哈他瑜伽课程收入', NOW(), 0),
(3, 'course', 6, 75.00, 45.00, 33.75, '2025-10-09', 'pending', NULL, '流瑜伽课程收入', NOW(), 0),

-- 张教练的康复咨询收入
(4, 'service', 8, 150.00, 50.00, 75.00, '2025-10-06', 'settled', '2025-10-15', '小刚运动康复咨询', NOW(), 0),
(4, 'course', 5, 85.00, 45.00, 38.25, '2025-10-07', 'pending', NULL, '功能性训练课程收入', NOW(), 0),
(4, 'bonus', NULL, 300.00, 100.00, 300.00, '2025-10-01', 'settled', '2025-10-15', '康复专项奖金', NOW(), 0),

-- 刘教练的舞蹈收入
(5, 'service', 9, 220.00, 55.00, 121.00, '2025-10-05', 'settled', '2025-10-15', '小丽舞蹈编排服务', NOW(), 0),
(5, 'course', 4, 68.00, 40.00, 27.20, '2025-10-06', 'pending', NULL, '拉丁舞基础课程收入', NOW(), 0),
(5, 'course', 4, 68.00, 40.00, 27.20, '2025-10-08', 'pending', NULL, '拉丁舞基础课程收入', NOW(), 0),

-- 额外的收入记录
(2, 'bonus', NULL, 800.00, 100.00, 800.00, '2025-09-30', 'settled', '2025-10-15', '第三季度业绩奖金', NOW(), 0),
(3, 'service', 20, 180.00, 55.00, 99.00, '2025-10-10', 'pending', NULL, '新学员瑜伽体验课', NOW(), 0),
(4, 'service', 21, 150.00, 50.00, 75.00, '2025-10-09', 'pending', NULL, '运动伤害预防咨询', NOW(), 0);

-- ================================================================
-- 教练结算记录测试数据（coach_settlement表）
-- ================================================================
INSERT INTO `coach_settlement` (`coach_id`, `settlement_no`, `settlement_period`, `start_date`, `end_date`, `total_income`, `deduction_amount`, `actual_amount`, `settlement_date`, `payment_method`, `payment_account`, `status`, `remark`, `create_time`, `is_deleted`) VALUES
-- 李教练的结算记录
(2, 'SETTLE202510150001', '2025-10上半月', '2025-10-01', '2025-10-15', 1540.00, 40.00, 1500.00, '2025-10-15', 'bank_transfer', '6222081234567890123', 'paid', '包含私教课程、课程授课和奖金收入，扣除个人所得税', NOW(), 0),

-- 王教练的结算记录
(3, 'SETTLE202510150002', '2025-10上半月', '2025-10-01', '2025-10-15', 267.85, 17.85, 250.00, '2025-10-15', 'alipay', 'wangli_yoga@163.com', 'paid', '主要为瑜伽私教和课程授课收入', NOW(), 0),

-- 张教练的结算记录
(4, 'SETTLE202510150003', '2025-10上半月', '2025-10-01', '2025-10-15', 413.25, 13.25, 400.00, '2025-10-15', 'wechat', 'zhangwei_rehab', 'paid', '包含康复咨询、课程授课和专项奖金', NOW(), 0),

-- 刘教练的结算记录
(5, 'SETTLE202510150004', '2025-10上半月', '2025-10-01', '2025-10-15', 175.40, 5.40, 170.00, '2025-10-15', 'bank_transfer', '6222081234567890456', 'paid', '舞蹈编排服务和课程授课收入', NOW(), 0),

-- 待支付的结算
(2, 'SETTLE202510310001', '2025-10下半月', '2025-10-16', '2025-10-31', 650.00, 20.00, 630.00, '2025-10-31', 'bank_transfer', '6222081234567890123', 'pending', '下半月结算，待支付', NOW(), 0),

-- 支付失败的结算
(3, 'SETTLE202509300001', '2025-09月结算', '2025-09-01', '2025-09-30', 820.50, 25.50, 795.00, '2025-09-30', 'alipay', 'old_account@163.com', 'failed', '支付宝账户已停用，需更新支付方式', NOW(), 0),

-- 额外结算记录
(4, 'SETTLE202509300002', '2025-09月结算', '2025-09-01', '2025-09-30', 1250.75, 50.75, 1200.00, '2025-09-30', 'wechat', 'zhangwei_rehab', 'paid', '9月份康复业务结算', NOW(), 0);

-- ================================================================
-- 教练日程变更申请测试数据（coach_schedule_change表）
-- ================================================================
INSERT INTO `coach_schedule_change` (`coach_id`, `change_type`, `original_date`, `original_start_time`, `original_end_time`, `new_date`, `new_start_time`, `new_end_time`, `reason`, `status`, `apply_time`, `review_time`, `reviewer_id`, `review_remark`, `create_time`, `is_deleted`) VALUES
-- 李教练的请假申请
(2, 'leave', '2025-10-15', '09:00:00', '18:00:00', NULL, NULL, NULL, '家人生病需要陪伴去医院，申请请兇3天假期。将协调其他教练代课。', 'approved', '2025-10-10 14:30:00', '2025-10-11 10:15:00', 1, '同意请假，已安排代课教练', NOW(), 0),

-- 王教练的加班申请
(3, 'overtime', '2025-10-12', '08:00:00', '16:00:00', '2025-10-12', '08:00:00', '20:00:00', '应学员要求，希望能够在周六晚上加剑4小时的瑜伽课程。愿意加班，请批准加班申请。', 'approved', '2025-10-08 16:45:00', '2025-10-09 09:20:00', 1, '同意加班，按加班费标准结算', NOW(), 0),

-- 张教练的时间调整申请
(4, 'adjust', '2025-10-14', '10:00:00', '18:00:00', '2025-10-14', '14:00:00', '20:00:00', '上午有重要学术会议需要参加，希望能够将工作时间调整到下午和晚上。已与学员沟通确认。', 'pending', '2025-10-09 11:20:00', NULL, NULL, NULL, NOW(), 0),

-- 刘教练的拒绝请假申请
(5, 'leave', '2025-10-18', '18:00:00', '22:00:00', NULL, NULL, NULL, '朋友结婚，希望能够请假半天参加婚礼。将尽量找代课教练。', 'rejected', '2025-10-07 09:30:00', '2025-10-08 15:45:00', 1, '当天有重要演出活动，无法安排代课，请调整请假日期', NOW(), 0),

-- 李教练的时间调整
(2, 'adjust', '2025-10-16', '14:00:00', '20:00:00', '2025-10-17', '09:00:00', '15:00:00', '由于个人身体原因（腰部轻微不适），希望将周二下午的课程调整到周三上午，以便更好地休息和恢复。', 'approved', '2025-10-11 08:15:00', '2025-10-11 14:30:00', 1, '同意调整，注意身体健康', NOW(), 0),

-- 王教练的转班申请
(3, 'adjust', '2025-10-20', '08:00:00', '16:00:00', '2025-10-21', '10:00:00', '18:00:00', '周六有瑜伽教师进修课程，需要参加学习。希望将工作日调整到周日，学员们已同意调整。', 'pending', '2025-10-12 13:45:00', NULL, NULL, NULL, NOW(), 0);

-- 21. 插入教练资格撤销申请测试数据
INSERT INTO `coach_qualification_revoke_apply` (`coach_id`, `user_id`, `revoke_reason`, `effective_date`, `status`, `apply_time`, `review_time`, `reviewer_id`, `review_remark`, `create_time`, `is_deleted`) VALUES
(3, 3, '由于个人原因，需要撤销教练资格，专注于其他事业发展', '2025-12-01', 'pending', '2025-10-08 14:30:00', NULL, NULL, NULL, NOW(), 0),
(4, 4, '家庭原因，需要更多时间陪伴家人，申请撤销教练资格', '2025-11-15', 'approved', '2025-10-01 10:15:00', '2025-10-03 16:20:00', 1, '同意申请，感谢您的贡献', NOW(), 0),
(2, 2, '计划出国深造，暂停教练工作', '2025-11-30', 'cancelled', '2025-09-25 09:00:00', NULL, NULL, NULL, NOW(), 0);

-- 22. 插入教练预约时段测试数据
INSERT INTO `coach_booking_slot` (`coach_id`, `service_id`, `booking_date`, `start_time`, `end_time`, `status`, `user_id`, `order_id`, `booking_price`, `booking_time`, `completion_time`, `remark`, `create_time`, `is_deleted`) VALUES
-- 李教练（coach_id=2）的私人训练时段
(2, 1, '2025-10-10', '09:00:00', '10:00:00', 'available', NULL, NULL, 200.00, NULL, NULL, '李教练私人训练时段', NOW(), 0),
(2, 1, '2025-10-10', '10:30:00', '11:30:00', 'booked', 6, 5, 200.00, '2025-10-09 15:30:00', NULL, '小明预约的私教课程', NOW(), 0),
(2, 1, '2025-10-10', '14:00:00', '15:00:00', 'available', NULL, NULL, 200.00, NULL, NULL, '下午时段', NOW(), 0),
(2, 1, '2025-10-11', '09:00:00', '10:00:00', 'blocked', NULL, NULL, 200.00, NULL, NULL, '教练临时不可用', NOW(), 0),
(2, 1, '2025-10-12', '15:00:00', '16:00:00', 'completed', 8, 6, 200.00, '2025-10-08 10:20:00', '2025-10-09 16:00:00', '小刚的私教课程已完成', NOW(), 0),

-- 王教练（coach_id=3）的瑜伽私教时段
(3, 2, '2025-10-10', '08:00:00', '09:15:00', 'available', NULL, NULL, 180.00, NULL, NULL, '王教练瑜伽时段', NOW(), 0),
(3, 2, '2025-10-10', '10:00:00', '11:15:00', 'booked', 7, 7, 180.00, '2025-10-09 12:45:00', NULL, '小红预约的瑜伽私教', NOW(), 0),
(3, 2, '2025-10-10', '18:30:00', '19:45:00', 'available', NULL, NULL, 180.00, NULL, NULL, '晚上时段', NOW(), 0),
(3, 2, '2025-10-11', '08:00:00', '09:15:00', 'available', NULL, NULL, 180.00, NULL, NULL, '早上瑜伽时段', NOW(), 0),

-- 张教练（coach_id=4）的运动康复咨询时段
(4, 3, '2025-10-10', '10:00:00', '10:45:00', 'available', NULL, NULL, 150.00, NULL, NULL, '张教练咨询时段', NOW(), 0),
(4, 3, '2025-10-10', '14:00:00', '14:45:00', 'booked', 9, 8, 150.00, '2025-10-09 09:10:00', NULL, '小丽的咨询预约', NOW(), 0),
(4, 3, '2025-10-11', '15:30:00', '16:15:00', 'available', NULL, NULL, 150.00, NULL, NULL, '下午咨询时段', NOW(), 0),

-- 刘教练（coach_id=5）的舞蹈服务时段
(5, 4, '2025-10-10', '19:00:00', '20:30:00', 'available', NULL, NULL, 220.00, NULL, NULL, '晚上舞蹈课程', NOW(), 0),
(5, 4, '2025-10-11', '19:00:00', '20:30:00', 'booked', 11, 9, 220.00, '2025-10-09 11:30:00', NULL, '小美的舞蹈课程', NOW(), 0);

COMMIT;

-- ================================================================
-- 完成提示
-- ================================================================

-- ================================================================
-- 🎉 数据库创建完成提示
-- ================================================================

SELECT '✅ 健身平台数据库创建成功！' as message,
       '包含46个表结构 + 丰富测试数据 + 教练商业化功能 + Admin/App接口测试支持' as features,
       '新增教练认证、离职、咨询、评价、收入、结算、预约时段、日程变更等全套测试数据' as new_features,
       '已为所有教练相关表插入充足测试数据，支持Knife4j完整测试流程' as fix_status,
       NOW() as complete_time;

-- ================================================================
-- 🔑 测试账号信息（密码都是 123456）
-- ================================================================
/*
📝 测试账号列表：

🔴 Admin管理端测试账号：
  - 用户名: admin
  - 密码: 123456  
  - 角色: 系统管理员
  - 用途: 测试Admin端用户管理、角色管理等接口

🔵 App用户端测试账号：
  - 用户名: user001 (昵称: 小明)
  - 密码: 123456
  - 角色: 普通用户  
  - 用途: 测试App端登录、个人信息等接口

🟫 教练端测试账号：
  - 用户名: coach001 (昵称: 李教练)
  - 密码: 123456
  - 角色: 健身教练
  - 用途: 测试教练相关功能

🎆 其他测试账号：
  - user002/user003/user004... (密码都是 123456)
  - coach002/coach003/coach004... (密码都是 123456)

🎓 新增教练商业化功能测试数据：
  - 教练认证申请: 4个测试申请(待审核/已通过/已拒绝/再次申请)
  - 教练离职申请: 4个测试申请(待审核/已批准/已拒绝/已取消)
  - 教练咨询记录: 7个测试记录(在线/线下咨询，多种状态)
  - 教练评价记录: 6个评价记录(不同教练，详细评分和评价内容)
  - 教练收入记录: 16个收入记录(私教/课程/奖金等多种类型)
  - 教练结算记录: 8个结算记录(已支付/待支付/失败等状态)
  - 教练预约时段: 15个测试时段(可预约/已预约/已完成/已屏蔽)
  - 教练日程变更: 7个申请记录(请假/加班/调整等类型)
  - 支持教练私教/咨询/瑜伽/舞蹈等多种服务类型
  - 支持完整业务流程: 认证申请 → 服务预约 → 收入结算 → 评价反馈
*/

-- ================================================================
-- 🚀 快速启动指南
-- ================================================================
/*
1️⃣ 数据库初始化：
   mysql -u root -p
   source D:/path/to/1_fitness_platform_complete.sql

2️⃣ 启动应用程序：
   # Admin管理端：
   cd web-admin && mvn spring-boot:run
   访问: http://localhost:8080/doc.html
   
   # App用户端：  
   cd web-app && mvn spring-boot:run
   访问: http://localhost:8081/doc.html

3️⃣ 接口测试流程：
   ✅ Admin端: GET /admin/user/list - 查看用户列表
   ✅ Admin端: GET /admin/role/list - 查看角色列表  
   ✅ App端: POST /app/auth/simple-login - 用user001登录获取token
   ✅ App端: GET /app/profile/info - 查看个人信息(需要token)

4️⃣ 常见问题解决：
   ❓ 如果出现 "Unknown column 'last_login_time'" 错误：
       说明数据库表结构过旧，请重新执行本 SQL 脚本
   
   ❓ 如果登录失败：
       检查账号密码是否正确（密码都是 123456）
       检查数据库连接配置是否正确
*/

-- ================================================================
-- 🎆 脚本执行完成！
-- ================================================================
