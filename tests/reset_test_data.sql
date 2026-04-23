-- Reset occupancy and pending records for local regression reruns
USE fitness_platform;

SET @token_version_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'user'
    AND COLUMN_NAME = 'token_version'
);
SET @token_version_ddl := IF(
  @token_version_exists = 0,
  'ALTER TABLE user ADD COLUMN token_version INT NOT NULL DEFAULT 0',
  'SELECT 1'
);
PREPARE stmt FROM @token_version_ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS task_run_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_code VARCHAR(64) NOT NULL,
  task_name VARCHAR(128) NOT NULL,
  trigger_mode VARCHAR(32) NOT NULL,
  run_status VARCHAR(32) NOT NULL,
  instance_id VARCHAR(64) DEFAULT NULL,
  started_at DATETIME NOT NULL,
  finished_at DATETIME DEFAULT NULL,
  duration_ms BIGINT DEFAULT NULL,
  affected_count INT DEFAULT NULL,
  message VARCHAR(500) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_task_run_code_started (task_code, started_at),
  KEY idx_task_run_status_started (run_status, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ensure user_profile table exists (for app profile fields)
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

-- ensure core roles exist (idempotent, stable role IDs for regression)
INSERT IGNORE INTO role (id, role_name, role_code, status) VALUES
(1, 'Admin', 'ADMIN', 1),
(2, 'Coach', 'COACH', 1),
(3, 'Member', 'MEMBER', 1),
(4, 'OpsAdmin', 'OPS_ADMIN', 1),
(5, 'AuditAdmin', 'AUDIT_ADMIN', 1);

-- close stale unpaid booking/order/payment to avoid blocking future runs
UPDATE coach_booking
SET booking_status = 'CANCELLED', pay_status = 'CLOSED'
WHERE is_deleted = 0 AND pay_status = 'UNPAID' AND booking_status = 'WAIT_PAY';

UPDATE order_info
SET order_status = 'CLOSED', pay_status = 'CLOSED'
WHERE is_deleted = 0 AND pay_status = 'UNPAID';

UPDATE payment_record
SET pay_status = 'CLOSED'
WHERE is_deleted = 0 AND pay_status = 'UNPAID';

-- physically purge logic-deleted enroll/booking rows to avoid unique-key blockers in reruns
DELETE FROM coach_booking WHERE is_deleted = 1;
DELETE FROM course_enrollment WHERE is_deleted = 1;

-- rebuild schedule occupancy from current active business rows
UPDATE coach_schedule cs
LEFT JOIN (
  SELECT schedule_id, COUNT(*) AS cnt
  FROM coach_booking
  WHERE is_deleted = 0
    AND booking_status IN ('WAIT_PAY', 'PAID', 'COMPLETED')
    AND pay_status IN ('UNPAID', 'PAID')
  GROUP BY schedule_id
) b ON b.schedule_id = cs.id
SET cs.booked_count = IFNULL(b.cnt, 0)
WHERE cs.is_deleted = 0;

UPDATE course_schedule cs
LEFT JOIN (
  SELECT schedule_id, COUNT(*) AS cnt
  FROM course_enrollment
  WHERE is_deleted = 0
    AND status IN (1, 2)
  GROUP BY schedule_id
) e ON e.schedule_id = cs.id
SET cs.booked_count = IFNULL(e.cnt, 0)
WHERE cs.is_deleted = 0;

-- cleanup autotest operation data
DELETE FROM banner WHERE title LIKE 'AutoTest Banner%';
DELETE FROM notice WHERE title LIKE 'AutoTest Notice%';
DELETE FROM system_config WHERE config_key LIKE 'autotest.%';
DELETE FROM task_run_log;

-- normalize stale course covers and placeholder URLs to a local stable fallback image
UPDATE course
SET cover_url = '/test.png'
WHERE cover_url IS NULL
   OR TRIM(cover_url) = ''
   OR cover_url = '/test.png'
   OR cover_url LIKE '%127.0.0.1:9000%'
   OR cover_url LIKE '%localhost:9000%'
   OR cover_url LIKE '/minio/%'
   OR cover_url LIKE 'https://example.com/%'
   OR cover_url LIKE 'http://example.com/%';

-- restore deterministic course enrollment smoke fixture
DROP TEMPORARY TABLE IF EXISTS tmp_smoke_course_order_ids;
CREATE TEMPORARY TABLE tmp_smoke_course_order_ids AS
SELECT DISTINCT order_id
FROM course_enrollment
WHERE schedule_id = 2
  AND order_id IS NOT NULL;

DELETE FROM order_item
WHERE order_id IN (SELECT order_id FROM tmp_smoke_course_order_ids);

DELETE FROM payment_record
WHERE order_id IN (SELECT order_id FROM tmp_smoke_course_order_ids);

DELETE FROM refund_record
WHERE order_id IN (SELECT order_id FROM tmp_smoke_course_order_ids);

DELETE FROM order_info
WHERE id IN (SELECT order_id FROM tmp_smoke_course_order_ids);

DELETE FROM course_enrollment
WHERE schedule_id = 2;

DROP TEMPORARY TABLE IF EXISTS tmp_smoke_course_order_ids;

INSERT INTO course (id, category_id, title, summary, cover_url, level, duration_min, price, status)
VALUES (1, 1, '基础体能体验课', '用于基础课程报名与并发烟雾验证的最小课程夹具。', '/test.png', 'beginner', 60, 99.00, 1)
ON DUPLICATE KEY UPDATE
  category_id = VALUES(category_id),
  title = VALUES(title),
  summary = VALUES(summary),
  cover_url = VALUES(cover_url),
  level = VALUES(level),
  duration_min = VALUES(duration_min),
  price = VALUES(price),
  status = VALUES(status),
  is_deleted = 0;

INSERT INTO course_schedule (id, course_id, coach_id, start_time, end_time, capacity, booked_count, status)
VALUES (2, 1, 2, '2030-03-04 18:00:00', '2030-03-04 19:00:00', 5, 0, 1)
ON DUPLICATE KEY UPDATE
  course_id = VALUES(course_id),
  coach_id = VALUES(coach_id),
  start_time = VALUES(start_time),
  end_time = VALUES(end_time),
  capacity = VALUES(capacity),
  booked_count = VALUES(booked_count),
  status = VALUES(status),
  is_deleted = 0;

-- restore the regression member accounts used by tests and UI walkthroughs
INSERT INTO user (id, username, password, nickname, email, phone, gender, birth_date, status, user_type)
VALUES (4, 'user123', '$2a$10$demoMemberPasswordHash', 'Regression User', 'user123@fitness.local', '13800000004', 1, '2001-03-15', 1, 'member')
ON DUPLICATE KEY UPDATE
  username = VALUES(username),
  password = VALUES(password),
  nickname = VALUES(nickname),
  email = VALUES(email),
  phone = VALUES(phone),
  gender = VALUES(gender),
  birth_date = VALUES(birth_date),
  status = VALUES(status),
  user_type = VALUES(user_type),
  token_version = 0;

INSERT INTO user (id, username, password, nickname, email, phone, gender, birth_date, status, user_type)
VALUES (3, 'member_chen', '$2a$10$demoMemberPasswordHash', '陈同学', 'member.chen@fitness.local', '13800000003', 2, '2002-09-09', 1, 'member')
ON DUPLICATE KEY UPDATE
  username = VALUES(username),
  password = VALUES(password),
  nickname = VALUES(nickname),
  email = VALUES(email),
  phone = VALUES(phone),
  gender = VALUES(gender),
  birth_date = VALUES(birth_date),
  status = VALUES(status),
  user_type = VALUES(user_type),
  token_version = 0;

-- simple login accounts for local dev
INSERT INTO user (id, username, password, nickname, email, phone, gender, birth_date, status, user_type, token_version)
VALUES (21, 'root', 'root', 'Test Member', 'root@fitness.local', '13800000101', 1, '2000-01-01', 1, 'member', 0)
ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  nickname = VALUES(nickname),
  email = VALUES(email),
  phone = VALUES(phone),
  gender = VALUES(gender),
  birth_date = VALUES(birth_date),
  status = VALUES(status),
  user_type = VALUES(user_type),
  token_version = 0;

INSERT INTO user (id, username, password, nickname, email, phone, gender, birth_date, status, user_type, token_version)
VALUES (22, 'root_admin', 'root', 'Test Admin', 'root.admin@fitness.local', '13800000102', 1, '1990-01-01', 1, 'admin', 0)
ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  nickname = VALUES(nickname),
  email = VALUES(email),
  phone = VALUES(phone),
  gender = VALUES(gender),
  birth_date = VALUES(birth_date),
  status = VALUES(status),
  user_type = VALUES(user_type),
  token_version = 0;

INSERT INTO user (id, username, password, nickname, email, phone, gender, birth_date, status, user_type, token_version)
VALUES (101, 'root_member', 'root', 'Regression Member', 'root.member@fitness.local', '13800000103', 1, '2000-06-01', 1, 'member', 0)
ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  nickname = VALUES(nickname),
  email = VALUES(email),
  phone = VALUES(phone),
  gender = VALUES(gender),
  birth_date = VALUES(birth_date),
  status = VALUES(status),
  user_type = VALUES(user_type),
  token_version = 0;

-- restore demo user-role relations for repeatable RBAC regression
DELETE FROM user_role WHERE user_id IN (1,2,3,4,11,12,21,22,101);
INSERT IGNORE INTO user_role (user_id, role_id) SELECT 1, id FROM role WHERE role_code = 'ADMIN';
INSERT IGNORE INTO user_role (user_id, role_id) SELECT 2, id FROM role WHERE role_code = 'COACH';
INSERT IGNORE INTO user_role (user_id, role_id) SELECT 3, id FROM role WHERE role_code = 'MEMBER';
INSERT IGNORE INTO user_role (user_id, role_id) SELECT 4, id FROM role WHERE role_code = 'MEMBER';
INSERT IGNORE INTO user_role (user_id, role_id) SELECT 11, id FROM role WHERE role_code = 'OPS_ADMIN';
INSERT IGNORE INTO user_role (user_id, role_id) SELECT 12, id FROM role WHERE role_code = 'AUDIT_ADMIN';
INSERT IGNORE INTO user_role (user_id, role_id) SELECT 21, id FROM role WHERE role_code = 'MEMBER';
INSERT IGNORE INTO user_role (user_id, role_id) SELECT 22, id FROM role WHERE role_code = 'ADMIN';
INSERT IGNORE INTO user_role (user_id, role_id) SELECT 101, id FROM role WHERE role_code = 'MEMBER';

-- ensure permission data for regression (idempotent)
INSERT IGNORE INTO permission (permission_name, permission_code, module, status) VALUES
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
('Operation Log Read', 'operation:log:read', 'system', 1),
('Task Run Read', 'task:run:read', 'system', 1),
('Task Run Trigger', 'task:run:trigger', 'system', 1);

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r CROSS JOIN permission p WHERE r.role_code = 'ADMIN';

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r
JOIN permission p ON p.permission_code IN (
  'banner:manage', 'notice:manage', 'system:config',
  'video:asset', 'video:upload', 'video:status', 'video:bind',
  'course:create', 'course:update', 'course:publish', 'course:schedule',
  'operation:log:read', 'task:run:read', 'task:run:trigger'
) WHERE r.role_code = 'OPS_ADMIN';

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r
JOIN permission p ON p.permission_code IN (
  'coach:apply:audit', 'refund:audit', 'pay:callback:audit', 'operation:log:read', 'task:run:read'
) WHERE r.role_code = 'AUDIT_ADMIN';

-- ensure role 1 course category scope exists (category 1) for regression
DELETE FROM role_course_category_scope WHERE role_id = 1;
INSERT IGNORE INTO role_course_category_scope (role_id, category_id)
VALUES (1, 1);
