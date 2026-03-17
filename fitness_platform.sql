-- 01_schema.sql
-- Fitness platform normalized schema

USE fitness_platform;

CREATE TABLE IF NOT EXISTS user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL,
  password VARCHAR(255) NOT NULL,
  nickname VARCHAR(50) DEFAULT NULL,
  email VARCHAR(100) DEFAULT NULL,
  phone VARCHAR(20) DEFAULT NULL,
  avatar VARCHAR(255) DEFAULT NULL,
  gender TINYINT DEFAULT 0,
  birth_date DATE DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  user_type VARCHAR(20) NOT NULL DEFAULT 'member',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_user_username (username),
  UNIQUE KEY uk_user_phone (phone),
  UNIQUE KEY uk_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_profile (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  bio VARCHAR(500) DEFAULT NULL,
  address VARCHAR(200) DEFAULT NULL,
  occupation VARCHAR(50) DEFAULT NULL,
  height INT DEFAULT NULL,
  weight INT DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_user_profile_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_name VARCHAR(50) NOT NULL,
  role_code VARCHAR(50) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  perm_name VARCHAR(100) NOT NULL,
  perm_code VARCHAR(100) NOT NULL,
  module VARCHAR(100) DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_perm_code (perm_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS role_permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  perm_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_role_perm (role_id, perm_id),
  KEY idx_role_perm_role (role_id),
  KEY idx_role_perm_perm (perm_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS role_course_category_scope (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_role_category_scope (role_id, category_id),
  KEY idx_role_category_role (role_id),
  KEY idx_role_category_category (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_user_role (user_id, role_id),
  KEY idx_user_role_user (user_id),
  KEY idx_user_role_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS coach_profile (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  bio VARCHAR(500) DEFAULT NULL,
  expertise VARCHAR(200) DEFAULT NULL,
  years INT DEFAULT 0,
  price DECIMAL(10,2) DEFAULT 0.00,
  rating DECIMAL(3,2) DEFAULT 0.00,
  cert_status TINYINT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_coach_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS course_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL,
  sort INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_course_category_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS course (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category_id BIGINT NOT NULL,
  title VARCHAR(100) NOT NULL,
  summary VARCHAR(500) DEFAULT NULL,
  cover_url VARCHAR(255) DEFAULT NULL,
  level VARCHAR(20) DEFAULT 'beginner',
  duration_min INT DEFAULT 0,
  price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_course_category (category_id),
  KEY idx_course_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS course_schedule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  course_id BIGINT NOT NULL,
  coach_id BIGINT NOT NULL,
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  capacity INT NOT NULL DEFAULT 1,
  booked_count INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_course_schedule_course (course_id),
  KEY idx_course_schedule_coach (coach_id),
  KEY idx_course_schedule_time (start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS course_enrollment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  schedule_id BIGINT DEFAULT NULL,
  order_id BIGINT DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  enroll_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_course_enrollment (user_id, course_id, schedule_id),
  KEY idx_course_enrollment_user (user_id),
  KEY idx_course_enrollment_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS training_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(100) NOT NULL,
  goal VARCHAR(50) DEFAULT NULL,
  level VARCHAR(20) DEFAULT 'beginner',
  duration_weeks INT DEFAULT 4,
  cover_url VARCHAR(255) DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS video_asset (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(100) NOT NULL,
  source_site VARCHAR(30) NOT NULL,
  source_url VARCHAR(500) NOT NULL,
  license_type VARCHAR(50) NOT NULL,
  attribution_required TINYINT NOT NULL DEFAULT 0,
  author_name VARCHAR(100) DEFAULT NULL,
  duration_sec INT DEFAULT 0,
  tags VARCHAR(200) DEFAULT NULL,
  minio_path VARCHAR(255) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS training_plan_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plan_id BIGINT NOT NULL,
  day_index INT NOT NULL,
  action_name VARCHAR(100) NOT NULL,
  `sets` INT DEFAULT 0,
  reps INT DEFAULT 0,
  duration_min INT DEFAULT 0,
  rest_sec INT DEFAULT 0,
  video_id BIGINT DEFAULT NULL,
  `sort` INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_plan_item_plan (plan_id),
  KEY idx_plan_item_day (plan_id, day_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS training_plan_subscribe (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plan_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  start_date DATE NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_plan_subscribe (plan_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS training_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  plan_id BIGINT DEFAULT NULL,
  plan_item_id BIGINT DEFAULT NULL,
  record_date DATE NOT NULL,
  duration_min INT DEFAULT 0,
  calories INT DEFAULT 0,
  feeling VARCHAR(100) DEFAULT NULL,
  record_images VARCHAR(500) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_training_record_user (user_id),
  KEY idx_training_record_date (record_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS coach_schedule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  coach_id BIGINT NOT NULL,
  schedule_date DATE NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  capacity INT NOT NULL DEFAULT 1,
  booked_count INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_coach_schedule_coach (coach_id),
  KEY idx_coach_schedule_date (schedule_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS coach_booking (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  coach_id BIGINT NOT NULL,
  schedule_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  booking_status VARCHAR(20) NOT NULL DEFAULT 'WAIT_PAY',
  pay_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
  checkin_time DATETIME DEFAULT NULL,
  finish_time DATETIME DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_coach_booking (user_id, schedule_id),
  KEY idx_coach_booking_coach (coach_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS coach_review (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  booking_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  coach_id BIGINT NOT NULL,
  score INT NOT NULL,
  content VARCHAR(500) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_coach_review (booking_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_info (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(50) NOT NULL,
  user_id BIGINT NOT NULL,
  total_amount DECIMAL(10,2) NOT NULL,
  order_status VARCHAR(20) NOT NULL DEFAULT 'NEW',
  pay_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
  biz_type VARCHAR(20) NOT NULL,
  biz_id BIGINT DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_order_user (user_id),
  KEY idx_order_status (order_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  item_type VARCHAR(20) NOT NULL,
  item_id BIGINT NOT NULL,
  item_name VARCHAR(100) NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  amount DECIMAL(10,2) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_order_item_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payment_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  pay_no VARCHAR(50) NOT NULL,
  callback_idempotency_key VARCHAR(64) DEFAULT NULL,
  pay_channel VARCHAR(20) NOT NULL,
  pay_amount DECIMAL(10,2) NOT NULL,
  pay_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
  pay_time DATETIME DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_pay_no (pay_no),
  UNIQUE KEY uk_payment_callback_key (callback_idempotency_key),
  KEY idx_payment_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS refund_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  refund_no VARCHAR(50) NOT NULL,
  refund_amount DECIMAL(10,2) NOT NULL,
  refund_status VARCHAR(20) NOT NULL DEFAULT 'REFUNDING',
  refund_time DATETIME DEFAULT NULL,
  reason VARCHAR(200) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_refund_no (refund_no),
  KEY idx_refund_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payment_callback_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  pay_no VARCHAR(50) NOT NULL,
  order_id BIGINT DEFAULT NULL,
  channel_trade_no VARCHAR(64) DEFAULT NULL,
  callback_status VARCHAR(20) NOT NULL,
  sign_valid TINYINT NOT NULL DEFAULT 0,
  callback_payload TEXT,
  process_result VARCHAR(30) NOT NULL,
  error_message VARCHAR(255) DEFAULT NULL,
  notified_at DATETIME DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_pay_callback_payno (pay_no),
  KEY idx_pay_callback_order (order_id),
  KEY idx_pay_callback_time (notified_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS banner (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(100) NOT NULL,
  image_url VARCHAR(255) NOT NULL,
  link_url VARCHAR(255) DEFAULT NULL,
  sort INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(100) NOT NULL,
  content TEXT NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  publish_time DATETIME DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS system_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  config_key VARCHAR(100) NOT NULL,
  config_value VARCHAR(500) NOT NULL,
  remark VARCHAR(200) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_sys_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  operator_id BIGINT DEFAULT NULL,
  module VARCHAR(50) NOT NULL,
  action VARCHAR(50) NOT NULL,
  detail VARCHAR(500) DEFAULT NULL,
  ip VARCHAR(50) DEFAULT NULL,
  success TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_operation_log_operator (operator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 02_seed.sql
-- Minimal seed data

USE fitness_platform;

INSERT INTO role (role_name, role_code, status) VALUES
('管理员', 'ADMIN', 1),
('教练', 'COACH', 1),
('学员', 'MEMBER', 1),
('运营管理员', 'OPS_ADMIN', 1),
('审核管理员', 'AUDIT_ADMIN', 1);

INSERT INTO permission (perm_name, perm_code, module, status) VALUES
('Banner Manage', 'banner:manage', 'content', 1),
('Notice Manage', 'notice:manage', 'content', 1),
('System Config', 'system:config', 'system', 1),
('Video Asset', 'video:asset', 'video', 1),
('Video Upload', 'video:upload', 'video', 1),
('Video Status', 'video:status', 'video', 1),
('Video Bind', 'video:bind', 'video', 1),
('Course Create', 'course:create', 'course', 1),
('Course Update', 'course:update', 'course', 1),
('Course Publish', 'course:publish', 'course', 1),
('Course Schedule', 'course:schedule', 'course', 1),
('Coach Apply Audit', 'coach:apply:audit', 'coach', 1),
('User Status', 'user:status', 'user', 1),
('User Role', 'user:role', 'user', 1),
('Refund Audit', 'refund:audit', 'payment', 1),
('Pay Callback Audit', 'pay:callback:audit', 'payment', 1),
('Operation Log Read', 'operation:log:read', 'system', 1);

INSERT IGNORE INTO role_permission (role_id, perm_id)
SELECT r.id, p.id FROM role r CROSS JOIN permission p WHERE r.role_code = 'ADMIN';

INSERT IGNORE INTO role_permission (role_id, perm_id)
SELECT r.id, p.id FROM role r
JOIN permission p ON p.perm_code IN (
  'banner:manage', 'notice:manage', 'system:config',
  'video:asset', 'video:upload', 'video:status', 'video:bind',
  'course:create', 'course:update', 'course:publish', 'course:schedule',
  'operation:log:read'
) WHERE r.role_code = 'OPS_ADMIN';

INSERT IGNORE INTO role_permission (role_id, perm_id)
SELECT r.id, p.id FROM role r
JOIN permission p ON p.perm_code IN (
  'coach:apply:audit', 'refund:audit', 'pay:callback:audit', 'operation:log:read'
) WHERE r.role_code = 'AUDIT_ADMIN';

INSERT IGNORE INTO role_course_category_scope (role_id, category_id)
SELECT id, 1 FROM role WHERE role_code = 'OPS_ADMIN';

INSERT INTO course_category (name, sort, status) VALUES
('燃脂', 1, 1),
('增肌', 2, 1),
('瑜伽', 3, 1);

INSERT INTO system_config (config_key, config_value, remark) VALUES
('site_name', '运动健康管理平台', '站点名称'),
('default_avatar', '/static/avatar/default.png', '默认头像');

-- Extended seed data for full business flow demo

INSERT IGNORE INTO user (id, username, password, nickname, email, phone, gender, birth_date, status, user_type) VALUES
(1, 'admin', '$2a$10$demoAdminPasswordHash', '平台管理员', 'admin@fitness.local', '13800000001', 1, '1995-01-01', 1, 'admin'),
(2, 'coach_lee', '$2a$10$demoCoachPasswordHash', '李教练', 'coach.lee@fitness.local', '13800000002', 1, '1992-05-12', 1, 'coach'),
(3, 'member_chen', '$2a$10$demoMemberPasswordHash', '陈同学', 'member.chen@fitness.local', '13800000003', 2, '2002-09-09', 1, 'member'),
(4, 'user123', '$2a$10$demoMemberPasswordHash', '回归测试用户', 'user123@fitness.local', '13800000004', 1, '2001-03-15', 1, 'member'),
(11, 'ops_admin', 'ops_admin_123', '运营管理员', 'ops.admin@fitness.local', '13800000011', 1, '1994-06-01', 1, 'admin'),
(12, 'audit_admin', 'audit_admin_123', '审核管理员', 'audit.admin@fitness.local', '13800000012', 2, '1993-08-18', 1, 'admin');

INSERT IGNORE INTO user (id, username, password, nickname, email, phone, gender, birth_date, status, user_type) VALUES
(21, 'root', 'root', '测试学员', 'root@fitness.local', '13800000101', 1, '2000-01-01', 1, 'member'),
(22, 'root_admin', 'root', '测试管理员', 'root.admin@fitness.local', '13800000102', 1, '1990-01-01', 1, 'admin');

INSERT IGNORE INTO user_role (id, user_id, role_id) VALUES
(1, 1, 1),
(2, 2, 2),
(3, 3, 3),
(4, 4, 3);

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT 11, id FROM role WHERE role_code = 'OPS_ADMIN';

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT 12, id FROM role WHERE role_code = 'AUDIT_ADMIN';

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT 21, id FROM role WHERE role_code = 'MEMBER';

INSERT IGNORE INTO user_role (user_id, role_id)
SELECT 22, id FROM role WHERE role_code = 'ADMIN';

INSERT IGNORE INTO coach_profile (id, user_id, bio, expertise, years, price, rating, cert_status, status) VALUES
(1, 2, '国家职业健身教练，擅长减脂增肌与动作矫正', '减脂,增肌,力量训练', 5, 199.00, 4.80, 1, 1);

INSERT IGNORE INTO course (id, category_id, title, summary, cover_url, level, duration_min, price, status) VALUES
(1, 1, '新手燃脂循环课', '45分钟中低强度燃脂课程，适合入门', '/minio/course/cover/fatburn-1.jpg', 'beginner', 45, 39.90, 1),
(2, 2, '基础力量训练课', '核心力量与全身复合动作训练', '/minio/course/cover/strength-1.jpg', 'beginner', 60, 59.90, 1);

INSERT IGNORE INTO course_schedule (id, course_id, coach_id, start_time, end_time, capacity, booked_count, status) VALUES
(1, 1, 2, '2026-03-01 19:00:00', '2026-03-01 19:45:00', 20, 1, 1),
(2, 2, 2, '2026-03-02 20:00:00', '2026-03-02 21:00:00', 15, 0, 1);

INSERT IGNORE INTO training_plan (id, title, goal, level, duration_weeks, cover_url, status) VALUES
(1, '4周减脂入门计划', 'fat_loss', 'beginner', 4, '/minio/plan/cover/plan-fatloss-1.jpg', 1);

INSERT IGNORE INTO video_asset (id, title, source_site, source_url, license_type, attribution_required, author_name, duration_sec, tags, minio_path, status) VALUES
(1, 'Bodyweight Squat Demo', 'pexels', 'https://www.pexels.com/video/placeholder-squat-demo/', 'Pexels License', 0, 'Pexels Author', 45, 'squat,legs,beginner', 'videos/pexels/squat-demo.mp4', 1),
(2, 'Push-up Demo', 'pexels', 'https://www.pexels.com/video/placeholder-pushup-demo/', 'Pexels License', 0, 'Pexels Author', 38, 'pushup,chest,beginner', 'videos/pexels/pushup-demo.mp4', 1);

INSERT IGNORE INTO training_plan_item (id, plan_id, day_index, action_name, `sets`, reps, duration_min, rest_sec, video_id, `sort`) VALUES
(1, 1, 1, '深蹲', 4, 12, 15, 60, 1, 1),
(2, 1, 1, '俯卧撑', 4, 10, 12, 60, 2, 2),
(3, 1, 2, '快走', 1, 1, 30, 0, NULL, 1);

INSERT IGNORE INTO training_plan_subscribe (id, plan_id, user_id, start_date, status) VALUES
(1, 1, 3, '2026-02-26', 1);

INSERT IGNORE INTO training_record (id, user_id, plan_id, plan_item_id, record_date, duration_min, calories, feeling, record_images) VALUES
(1, 3, 1, 1, '2026-02-26', 18, 160, '略吃力，但可完成', 'images/record/20260226-1.jpg');

INSERT IGNORE INTO coach_schedule (id, coach_id, schedule_date, start_time, end_time, price, capacity, booked_count, status) VALUES
(1, 2, '2026-03-03', '18:00:00', '19:00:00', 199.00, 1, 1, 1),
(2, 2, '2026-03-04', '18:00:00', '19:00:00', 199.00, 1, 0, 1);

INSERT IGNORE INTO order_info (id, order_no, user_id, total_amount, order_status, pay_status, biz_type, biz_id) VALUES
(1, 'ORD202602250001', 3, 199.00, 'PAID', 'PAID', 'coach_booking', 1),
(2, 'ORD202602250002', 3, 39.90, 'PAID', 'PAID', 'course', 1);

INSERT IGNORE INTO order_item (id, order_id, item_type, item_id, item_name, price, quantity, amount) VALUES
(1, 1, 'coach_booking', 1, '李教练线下私教课', 199.00, 1, 199.00),
(2, 2, 'course', 1, '新手燃脂循环课', 39.90, 1, 39.90);

INSERT IGNORE INTO payment_record (id, order_id, pay_no, pay_channel, pay_amount, pay_status, pay_time) VALUES
(1, 1, 'PAY202602250001', 'wechat', 199.00, 'PAID', '2026-02-25 10:30:00'),
(2, 2, 'PAY202602250002', 'alipay', 39.90, 'PAID', '2026-02-25 10:40:00');

INSERT IGNORE INTO coach_booking (id, user_id, coach_id, schedule_id, order_id, booking_status, pay_status, checkin_time, finish_time) VALUES
(1, 3, 2, 1, 1, 'COMPLETED', 'PAID', '2026-03-03 17:55:00', '2026-03-03 19:05:00');

INSERT IGNORE INTO coach_review (id, booking_id, user_id, coach_id, score, content) VALUES
(1, 1, 3, 2, 5, '动作讲解清晰，训练节奏把控很好');

INSERT IGNORE INTO course_enrollment (id, user_id, course_id, schedule_id, order_id, status) VALUES
(1, 3, 1, 1, 2, 1);

INSERT IGNORE INTO refund_record (id, order_id, refund_no, refund_amount, refund_status, refund_time, reason) VALUES
(1, 2, 'REF202602250001', 39.90, 'REFUNDED', '2026-02-25 16:00:00', '临时冲突，用户申请退款');

INSERT IGNORE INTO banner (id, title, image_url, link_url, sort, status) VALUES
(1, '开学季体能提升计划', '/minio/banner/back-to-school.jpg', '/app/plan/1', 1, 1);

INSERT IGNORE INTO notice (id, title, content, status, publish_time) VALUES
(1, '平台升级通知', '本周新增训练计划订阅与教练线下预约功能。', 1, '2026-02-25 09:00:00');

INSERT IGNORE INTO operation_log (id, operator_id, module, action, detail, ip, success) VALUES
(1, 1, 'course', 'create', '创建课程: 新手燃脂循环课', '127.0.0.1', 1);
