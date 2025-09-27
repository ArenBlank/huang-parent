-- ================================================================
-- 健身平台完整数据库SQL脚本（第二版 - 升级版）
-- 包含：基础功能模块 + 教练业务优化模块 + 健康科普文章模块 + 完整测试数据
-- 版本：Spring Boot 3.0.5 + MyBatis-Plus 3.5.3.1
-- 日期：2025-01-24（更新：2025-09-27）
-- 说明：本脚本包含所有表结构创建、逻辑删除字段、完整测试数据，完全兼容 MyBatis-Plus @TableLogic 功能
-- 更新内容（v2.1）：
-- 1. 完全修复了所有 BaseEntity 字段不匹配问题
-- 2. 为所有继承 BaseEntity 的表添加了 update_time 字段
-- 3. 修复了缺失 create_time 字段的表
-- 4. 完全兼容 MyBatis-Plus @TableLogic 和 @FieldFill 功能
-- 5. 保证所有表结构与实体类定义一致
-- 6. 完善了测试数据，确保每张表都有充足数据
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

-- 3. 用户角色关联表
DROP TABLE IF EXISTS `user_role`;
CREATE TABLE `user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`),
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
  UNIQUE KEY `uk_user_id` (`user_id`),
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

-- 7. 课程分类表
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

-- 8. 课程表
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '课程ID',
  `course_name` varchar(100) NOT NULL COMMENT '课程名称',
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `coach_id` bigint NOT NULL COMMENT '教练ID',
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
  KEY `idx_status` (`status`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- 9. 课程排期表
DROP TABLE IF EXISTS `course_schedule`;
CREATE TABLE `course_schedule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '排期ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `coach_id` bigint NOT NULL COMMENT '教练ID',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `location` varchar(100) DEFAULT NULL COMMENT '上课地点',
  `current_participants` int DEFAULT '0' COMMENT '当前报名人数',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-已取消，1-正常，2-已结束',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_coach_id` (`coach_id`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_status` (`status`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程排期表';

-- 10. 课程报名表
DROP TABLE IF EXISTS `course_enrollment`;
CREATE TABLE `course_enrollment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '报名ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `schedule_id` bigint NOT NULL COMMENT '排期ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `enrollment_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-已取消，1-已报名，2-已签到',
  `check_in_time` datetime DEFAULT NULL COMMENT '签到时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_schedule` (`user_id`,`schedule_id`),
  KEY `idx_schedule_id` (`schedule_id`),
  KEY `idx_status` (`status`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程报名表';

-- 11. 运动记录表
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

-- 12. 运动计划表
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

-- 13. 食物数据库表
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

-- 14. 饮食记录表
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

-- 15. 动态表
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

-- 16. 评论表
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

-- 17. 关注表
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

-- 18. 点赞记录表
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

-- 19. 订单表
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

-- 20. 订单详情表
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

-- 21. 支付记录表
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

-- 22. 退款记录表
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

-- 23. 系统配置表
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

-- 24. 操作日志表
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

-- 25. 教练可用时间表
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

-- 26. 教练日程变更申请表
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

-- 27. 教练认证申请表
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
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_apply_time` (`apply_time`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练认证申请表';

-- 28. 教练离职申请表
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

-- 29. 教练服务项目表
DROP TABLE IF EXISTS `coach_service`;
CREATE TABLE `coach_service` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '服务ID',
  `coach_id` bigint NOT NULL COMMENT '教练ID',
  `service_name` varchar(100) NOT NULL COMMENT '服务名称',
  `service_type` varchar(50) NOT NULL COMMENT '服务类型: consultation咨询 training私教 assessment评估',
  `description` text COMMENT '服务描述',
  `duration` int NOT NULL COMMENT '服务时长(分钟)',
  `price` decimal(10,2) NOT NULL COMMENT '价格',
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

-- 30. 教练咨询记录表
DROP TABLE IF EXISTS `coach_consultation`;
CREATE TABLE `coach_consultation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '咨询ID',
  `coach_id` bigint NOT NULL COMMENT '教练ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `consultation_type` varchar(20) NOT NULL COMMENT '咨询类型: online在线 offline线下',
  `consultation_date` datetime NOT NULL COMMENT '咨询时间',
  `duration` int DEFAULT NULL COMMENT '咨询时长(分钟)',
  `topic` varchar(200) NOT NULL COMMENT '咨询主题',
  `content` text COMMENT '咨询内容',
  `coach_advice` text COMMENT '教练建议',
  `status` varchar(20) DEFAULT 'scheduled' COMMENT '状态: scheduled已预约 completed已完成 cancelled已取消',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT 0 COMMENT '是否删除: 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_coach_id` (`coach_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_consultation_date` (`consultation_date`),
  KEY `idx_status` (`status`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练咨询记录表';

-- 31. 教练评价表
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

-- 32. 教练收入记录表
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

-- 33. 教练结算记录表
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

-- ================================================================
-- 第三部分：健康科普文章模块
-- ================================================================

-- 34. 文章分类表
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

-- 35. 健康文章表
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

-- 36. 文章审核日志表
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

-- 37. 文章浏览记录表
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

-- 38. 文章点赞表
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

-- 39. 文章收藏表
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

-- 40. 文章收藏夹表
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

-- 41. 文章分享记录表
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

-- 42. 文章评论表
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
-- 初始化数据
-- ================================================================

-- 插入默认角色
INSERT INTO `role` (`role_name`, `role_code`, `description`) VALUES 
('管理员', 'admin', '系统管理员'),
('教练', 'coach', '健身教练'),
('会员', 'member', '普通会员');

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
-- 存储过程
-- ================================================================

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
UPDATE `role` SET `status` = 1, `create_time` = NOW(), `update_time` = NOW(), `is_deleted` = 0 WHERE `role_code` = 'admin';
UPDATE `role` SET `status` = 1, `create_time` = NOW(), `update_time` = NOW(), `is_deleted` = 0 WHERE `role_code` = 'coach';
UPDATE `role` SET `status` = 1, `create_time` = NOW(), `update_time` = NOW(), `is_deleted` = 0 WHERE `role_code` = 'member';
-- 添加VIP角色
INSERT INTO `role` (`role_name`, `role_code`, `description`, `status`, `create_time`, `update_time`, `is_deleted`) VALUES
('VIP用户', 'vip', 'VIP用户，享受更多特权服务', 1, NOW(), NOW(), 0);

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
INSERT INTO `coach` (`user_id`, `real_name`, `certification_no`, `specialties`, `introduction`, `experience_years`, `rating`, `status`, `create_time`, `update_time`, `is_deleted`) VALUES
(2, '李强', 'CERT001', '力量训练,减脂塑形', '资深健身教练，10年教学经验，擅长力量训练和减脂塑形。', 10, 4.8, 1, NOW(), NOW(), 0),
(3, '王丽', 'CERT002', '瑜伽,普拉提', '瑜伽高级教练，精通各类瑜伽体式，专注身心健康。', 8, 4.9, 1, NOW(), NOW(), 0),
(4, '张伟', 'CERT003', '有氧训练,康复训练', '运动康复专家，帮助学员科学训练，预防运动伤害。', 6, 4.7, 1, NOW(), NOW(), 0),
(5, '刘芳', 'CERT004', '舞蹈,形体', '专业舞蹈教练，擅长各类舞蹈教学，形体塑造专家。', 5, 4.6, 1, NOW(), NOW(), 0),
(1, '管理员', 'ADMIN001', '综合管理', '系统管理员账户', 5, 5.0, 1, NOW(), NOW(), 0);

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

-- 8. 插入课程排期数据
INSERT INTO `course_schedule` (`course_id`, `coach_id`, `start_time`, `end_time`, `location`, `current_participants`, `status`, `create_time`, `is_deleted`) VALUES
(1, 2, '2025-09-27 09:00:00', '2025-09-27 09:45:00', '健身房A区', 8, 1, NOW(), 0),
(1, 2, '2025-09-28 19:00:00', '2025-09-28 19:45:00', '健身房A区', 12, 1, NOW(), 0),
(2, 2, '2025-09-27 14:00:00', '2025-09-27 15:00:00', '力量训练区', 6, 1, NOW(), 0),
(2, 2, '2025-09-29 10:00:00', '2025-09-29 11:00:00', '力量训练区', 9, 1, NOW(), 0),
(3, 3, '2025-09-27 08:00:00', '2025-09-27 09:15:00', '瑜伽室1', 15, 1, NOW(), 0),
(3, 3, '2025-09-28 18:00:00', '2025-09-28 19:15:00', '瑜伽室1', 18, 1, NOW(), 0),
(4, 5, '2025-09-27 20:00:00', '2025-09-27 21:00:00', '舞蹈室', 12, 1, NOW(), 0),
(4, 5, '2025-09-29 19:00:00', '2025-09-29 20:00:00', '舞蹈室', 14, 1, NOW(), 0),
(5, 4, '2025-09-28 10:00:00', '2025-09-28 10:50:00', '功能训练区', 10, 1, NOW(), 0),
(6, 3, '2025-09-29 08:00:00', '2025-09-29 09:00:00', '瑜伽室2', 16, 1, NOW(), 0);

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
(2, '私人训练指导', 'training', '一对一力量训练指导，制定个性化训练计划', 60, 200.00, 1, 1, NOW(), 0),
(3, '瑜伽私教', 'training', '个性化瑜伽指导，针对性解决身体问题', 75, 180.00, 1, 1, NOW(), 0),
(4, '运动康复咨询', 'consultation', '运动伤害评估和康复建议', 45, 150.00, 1, 1, NOW(), 0),
(5, '舞蹈编排服务', 'training', '专业舞蹈编排和教学', 90, 220.00, 1, 1, NOW(), 0);

COMMIT;

-- ================================================================
-- 完成提示
-- ================================================================

SELECT '✅ 健身平台完整数据库创建成功！（第二版）' as message,
       '包含基础功能模块、教练业务优化模块、健康科普文章模块' as modules,
       '共42个数据表，多个存储过程、触发器和视图' as summary,
       '已完整集成 MyBatis-Plus @TableLogic 逻辑删除功能' as logical_delete,
       '增强版：更多测试数据，修复所有字段问题' as enhancement,
       NOW() as complete_time;