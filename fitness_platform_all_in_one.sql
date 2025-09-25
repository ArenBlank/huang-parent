-- ================================================================
-- 健身平台完整数据库SQL脚本（整合版）
-- 包含：基础功能模块 + 教练业务优化模块 + 健康科普文章模块
-- 版本：Spring Boot 3.0.5 + MyBatis-Plus 3.5.3.1
-- 日期：2025-01-24
-- ================================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `fitness_platform` 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE `fitness_platform`;

-- ================================================================
-- 第一部分：基础功能模块
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
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  UNIQUE KEY `uk_email` (`email`)
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
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 3. 用户角色关联表
DROP TABLE IF EXISTS `user_role`;
CREATE TABLE `user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`),
  KEY `idx_role_id` (`role_id`)
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
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_status` (`status`)
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
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
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
  PRIMARY KEY (`id`),
  KEY `idx_user_id_date` (`user_id`,`measure_date`)
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
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
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
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_coach_id` (`coach_id`),
  KEY `idx_status` (`status`)
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
  PRIMARY KEY (`id`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_coach_id` (`coach_id`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程排期表';

-- 10. 课程报名表
DROP TABLE IF EXISTS `course_enrollment`;
CREATE TABLE `course_enrollment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '报名ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `schedule_id` bigint NOT NULL COMMENT '排期ID',
  `enrollment_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-已取消，1-已报名，2-已签到',
  `check_in_time` datetime DEFAULT NULL COMMENT '签到时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_schedule` (`user_id`,`schedule_id`),
  KEY `idx_schedule_id` (`schedule_id`),
  KEY `idx_status` (`status`)
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
  PRIMARY KEY (`id`),
  KEY `idx_user_date` (`user_id`,`exercise_date`)
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
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
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
  PRIMARY KEY (`id`),
  KEY `idx_food_name` (`food_name`),
  KEY `idx_category` (`category`)
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
  PRIMARY KEY (`id`),
  KEY `idx_user_date` (`user_id`,`record_date`),
  KEY `idx_food_id` (`food_id`)
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
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态表';

-- 16. 评论表
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `post_id` bigint NOT NULL COMMENT '动态ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `parent_id` bigint DEFAULT '0' COMMENT '父评论ID',
  `content` text NOT NULL COMMENT '评论内容',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-删除，1-正常',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- 17. 点赞表
DROP TABLE IF EXISTS `like_record`;
CREATE TABLE `like_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `target_id` bigint NOT NULL COMMENT '目标ID',
  `target_type` tinyint NOT NULL COMMENT '目标类型：1-动态，2-评论',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target` (`user_id`,`target_id`,`target_type`),
  KEY `idx_target` (`target_id`,`target_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞表';

-- 18. 关注表
DROP TABLE IF EXISTS `follow`;
CREATE TABLE `follow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `follower_id` bigint NOT NULL COMMENT '关注者ID',
  `following_id` bigint NOT NULL COMMENT '被关注者ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follow` (`follower_id`,`following_id`),
  KEY `idx_following_id` (`following_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关注表';

-- 19. 订单表
DROP TABLE IF EXISTS `order_info`;
CREATE TABLE `order_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` varchar(50) NOT NULL COMMENT '订单号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `order_type` tinyint NOT NULL COMMENT '订单类型：1-课程，2-会员，3-私教',
  `total_amount` decimal(10,2) NOT NULL COMMENT '总金额',
  `pay_amount` decimal(10,2) DEFAULT NULL COMMENT '实付金额',
  `status` tinyint DEFAULT '0' COMMENT '状态：0-待支付，1-已支付，2-已取消，3-已退款',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 20. 订单详情表
DROP TABLE IF EXISTS `order_detail`;
CREATE TABLE `order_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `item_id` bigint NOT NULL COMMENT '商品ID',
  `item_type` tinyint NOT NULL COMMENT '商品类型：1-课程，2-会员，3-私教',
  `item_name` varchar(200) NOT NULL COMMENT '商品名称',
  `price` decimal(10,2) NOT NULL COMMENT '单价',
  `quantity` int DEFAULT '1' COMMENT '数量',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
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
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_pay_no` (`pay_no`)
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
  `apply_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `process_time` datetime DEFAULT NULL COMMENT '处理时间',
  `refund_time` datetime DEFAULT NULL COMMENT '退款时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_refund_no` (`refund_no`)
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
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
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
  `operation_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `duration` bigint DEFAULT NULL COMMENT '执行时长(毫秒)',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_operation_time` (`operation_time`)
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
  PRIMARY KEY (`id`),
  KEY `idx_coach_id` (`coach_id`),
  KEY `idx_day_of_week` (`day_of_week`)
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
  PRIMARY KEY (`id`),
  KEY `idx_coach_id` (`coach_id`),
  KEY `idx_status` (`status`),
  KEY `idx_apply_time` (`apply_time`)
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
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_apply_time` (`apply_time`)
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
  PRIMARY KEY (`id`),
  KEY `idx_coach_id` (`coach_id`),
  KEY `idx_status` (`status`),
  KEY `idx_apply_time` (`apply_time`)
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
  PRIMARY KEY (`id`),
  KEY `idx_coach_id` (`coach_id`),
  KEY `idx_service_type` (`service_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练服务项目表';

-- 30. 教练咨询预约表
DROP TABLE IF EXISTS `coach_consultation`;
CREATE TABLE `coach_consultation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '预约ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `coach_id` bigint NOT NULL COMMENT '教练ID',
  `service_id` bigint NOT NULL COMMENT '服务ID',
  `appointment_date` date NOT NULL COMMENT '预约日期',
  `start_time` time NOT NULL COMMENT '开始时间',
  `end_time` time NOT NULL COMMENT '结束时间',
  `consultation_type` varchar(50) DEFAULT NULL COMMENT '咨询类型',
  `problem_desc` text COMMENT '问题描述',
  `status` varchar(20) DEFAULT 'pending' COMMENT '状态: pending待确认 confirmed已确认 completed已完成 cancelled已取消',
  `confirm_time` datetime DEFAULT NULL COMMENT '确认时间',
  `complete_time` datetime DEFAULT NULL COMMENT '完成时间',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
  `cancel_reason` varchar(200) DEFAULT NULL COMMENT '取消原因',
  `coach_notes` text COMMENT '教练备注',
  `user_feedback` text COMMENT '用户反馈',
  `rating` decimal(2,1) DEFAULT NULL COMMENT '评分',
  `order_id` bigint DEFAULT NULL COMMENT '关联订单ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_coach_id` (`coach_id`),
  KEY `idx_appointment_date` (`appointment_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练咨询预约表';

-- 31. 教练评价表
DROP TABLE IF EXISTS `coach_evaluation`;
CREATE TABLE `coach_evaluation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评价ID',
  `coach_id` bigint NOT NULL COMMENT '教练ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `order_id` bigint DEFAULT NULL COMMENT '关联订单ID',
  `consultation_id` bigint DEFAULT NULL COMMENT '关联咨询ID',
  `overall_rating` decimal(2,1) NOT NULL COMMENT '总体评分',
  `professional_rating` decimal(2,1) DEFAULT NULL COMMENT '专业能力评分',
  `attitude_rating` decimal(2,1) DEFAULT NULL COMMENT '服务态度评分',
  `communication_rating` decimal(2,1) DEFAULT NULL COMMENT '沟通能力评分',
  `content` text COMMENT '评价内容',
  `images` json DEFAULT NULL COMMENT '评价图片',
  `is_anonymous` tinyint DEFAULT 0 COMMENT '是否匿名: 0否 1是',
  `reply_content` text COMMENT '教练回复',
  `reply_time` datetime DEFAULT NULL COMMENT '回复时间',
  `status` tinyint DEFAULT 1 COMMENT '状态: 0隐藏 1显示',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_coach_id` (`coach_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练评价表';

-- 32. 教练收入记录表
DROP TABLE IF EXISTS `coach_income`;
CREATE TABLE `coach_income` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `coach_id` bigint NOT NULL COMMENT '教练ID',
  `income_type` varchar(50) NOT NULL COMMENT '收入类型: course课程 consultation咨询 bonus奖金',
  `order_id` bigint DEFAULT NULL COMMENT '关联订单ID',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  `commission_rate` decimal(5,2) DEFAULT NULL COMMENT '提成比例(%)',
  `actual_amount` decimal(10,2) NOT NULL COMMENT '实际收入',
  `income_date` date NOT NULL COMMENT '收入日期',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `settlement_status` varchar(20) DEFAULT 'pending' COMMENT '结算状态: pending待结算 settled已结算',
  `settlement_time` datetime DEFAULT NULL COMMENT '结算时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_coach_id` (`coach_id`),
  KEY `idx_income_date` (`income_date`),
  KEY `idx_settlement_status` (`settlement_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练收入记录表';

-- 33. 教练收入结算表
DROP TABLE IF EXISTS `coach_settlement`;
CREATE TABLE `coach_settlement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '结算ID',
  `coach_id` bigint NOT NULL COMMENT '教练ID',
  `settlement_no` varchar(50) NOT NULL COMMENT '结算单号',
  `settlement_period` varchar(20) NOT NULL COMMENT '结算周期',
  `start_date` date NOT NULL COMMENT '开始日期',
  `end_date` date NOT NULL COMMENT '结束日期',
  `total_amount` decimal(10,2) NOT NULL COMMENT '总金额',
  `deduction_amount` decimal(10,2) DEFAULT 0.00 COMMENT '扣除金额',
  `actual_amount` decimal(10,2) NOT NULL COMMENT '实际结算金额',
  `income_details` json DEFAULT NULL COMMENT '收入明细',
  `status` varchar(20) DEFAULT 'pending' COMMENT '状态: pending待确认 confirmed已确认 paid已支付',
  `confirm_time` datetime DEFAULT NULL COMMENT '确认时间',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `pay_method` varchar(50) DEFAULT NULL COMMENT '支付方式',
  `pay_account` varchar(100) DEFAULT NULL COMMENT '支付账号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_settlement_no` (`settlement_no`),
  KEY `idx_coach_id` (`coach_id`),
  KEY `idx_settlement_period` (`settlement_period`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练收入结算表';

-- ================================================================
-- 第三部分：健康科普文章模块
-- ================================================================

-- 34. 文章分类表
DROP TABLE IF EXISTS `article_category`;
CREATE TABLE `article_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `parent_id` bigint DEFAULT 0 COMMENT '父分类ID',
  `category_name` varchar(50) NOT NULL COMMENT '分类名称',
  `category_code` varchar(50) DEFAULT NULL COMMENT '分类编码',
  `icon` varchar(100) DEFAULT NULL COMMENT '分类图标',
  `description` varchar(200) DEFAULT NULL COMMENT '分类描述',
  `sort_order` int DEFAULT 0 COMMENT '排序',
  `status` tinyint DEFAULT 1 COMMENT '状态: 0禁用 1启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章分类表';

-- 35. 健康科普文章表
DROP TABLE IF EXISTS `health_article`;
CREATE TABLE `health_article` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文章ID',
  `article_no` varchar(50) DEFAULT NULL COMMENT '文章编号',
  `title` varchar(200) NOT NULL COMMENT '文章标题',
  `subtitle` varchar(300) DEFAULT NULL COMMENT '副标题',
  `author_id` bigint NOT NULL COMMENT '作者ID（教练用户ID）',
  `author_name` varchar(50) NOT NULL COMMENT '作者名称',
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `cover_image` varchar(500) DEFAULT NULL COMMENT '封面图片',
  `summary` varchar(500) DEFAULT NULL COMMENT '文章摘要',
  `content` longtext NOT NULL COMMENT '文章内容（富文本）',
  `content_type` varchar(20) DEFAULT 'richtext' COMMENT '内容类型: richtext富文本 markdown',
  `tags` json DEFAULT NULL COMMENT '标签JSON数组',
  `keywords` varchar(200) DEFAULT NULL COMMENT 'SEO关键词',
  `source` varchar(100) DEFAULT NULL COMMENT '文章来源',
  `reading_time` int DEFAULT NULL COMMENT '预计阅读时长(分钟)',
  `difficulty_level` varchar(20) DEFAULT NULL COMMENT '难度等级: beginner初级 intermediate中级 advanced高级',
  
  -- 审核相关字段
  `status` varchar(20) DEFAULT 'draft' COMMENT '状态: draft草稿 pending待审核 approved已通过 rejected已拒绝 published已发布 offline已下线',
  `submit_time` datetime DEFAULT NULL COMMENT '提交审核时间',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_by` bigint DEFAULT NULL COMMENT '审核人ID',
  `audit_remark` varchar(500) DEFAULT NULL COMMENT '审核备注',
  `reject_reason` varchar(500) DEFAULT NULL COMMENT '拒绝原因',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  
  -- 统计字段
  `view_count` int DEFAULT 0 COMMENT '浏览次数',
  `like_count` int DEFAULT 0 COMMENT '点赞数',
  `collect_count` int DEFAULT 0 COMMENT '收藏数',
  `share_count` int DEFAULT 0 COMMENT '分享数',
  `comment_count` int DEFAULT 0 COMMENT '评论数',
  
  -- 排序推荐字段
  `is_top` tinyint DEFAULT 0 COMMENT '是否置顶: 0否 1是',
  `is_recommend` tinyint DEFAULT 0 COMMENT '是否推荐: 0否 1是',
  `is_hot` tinyint DEFAULT 0 COMMENT '是否热门: 0否 1是',
  `sort_order` int DEFAULT 0 COMMENT '排序权重',
  
  -- 其他字段
  `allow_comment` tinyint DEFAULT 1 COMMENT '是否允许评论: 0否 1是',
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
  KEY `idx_is_recommend` (`is_recommend`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康科普文章表';

-- 36. 文章审核记录表
DROP TABLE IF EXISTS `article_audit_log`;
CREATE TABLE `article_audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `operation` varchar(20) NOT NULL COMMENT '操作类型: submit提交 approve通过 reject拒绝 offline下线',
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) DEFAULT NULL COMMENT '操作人姓名',
  `operator_role` varchar(20) DEFAULT NULL COMMENT '操作人角色: coach教练 admin管理员',
  `before_status` varchar(20) DEFAULT NULL COMMENT '操作前状态',
  `after_status` varchar(20) DEFAULT NULL COMMENT '操作后状态',
  `remark` varchar(500) DEFAULT NULL COMMENT '操作备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_article_id` (`article_id`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章审核记录表';

-- 37. 文章浏览记录表
DROP TABLE IF EXISTS `article_view_log`;
CREATE TABLE `article_view_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID（游客为空）',
  `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
  `duration` int DEFAULT 0 COMMENT '阅读时长(秒)',
  `source` varchar(50) DEFAULT NULL COMMENT '来源: search搜索 recommend推荐 share分享',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_article_id` (`article_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章浏览记录表';

-- 38. 文章点赞表
DROP TABLE IF EXISTS `article_like`;
CREATE TABLE `article_like` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_user` (`article_id`, `user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章点赞表';

-- 39. 文章收藏表
DROP TABLE IF EXISTS `article_collect`;
CREATE TABLE `article_collect` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `folder_id` bigint DEFAULT NULL COMMENT '收藏夹ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_user` (`article_id`, `user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_folder_id` (`folder_id`),
  KEY `idx_create_time` (`create_time`)
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
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
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
  PRIMARY KEY (`id`),
  KEY `idx_article_id` (`article_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
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
  PRIMARY KEY (`id`),
  KEY `idx_article_id` (`article_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_status` (`status`)
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
('system.version', '1.0.0', '系统版本'),
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
CREATE INDEX idx_order_user_status_create ON order_info(user_id, status, create_time);
CREATE INDEX idx_article_status_publish_time ON health_article(status, publish_time);

-- ================================================================
-- 完成提示
-- ================================================================

SELECT '✅ 健身平台完整数据库创建成功！' as message,
       '包含基础功能模块、教练业务优化模块、健康科普文章模块' as modules,
       '共42个数据表，多个存储过程、触发器和视图' as summary,
       NOW() as complete_time;