-- Reset occupancy and pending records for local regression reruns
USE fitness_platform;

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
  user_type = VALUES(user_type);

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
  user_type = VALUES(user_type);

-- simple login accounts for local dev
INSERT IGNORE INTO user (id, username, password, nickname, email, phone, gender, birth_date, status, user_type) VALUES
(21, 'root', 'root', 'Test Member', 'root@fitness.local', '13800000101', 1, '2000-01-01', 1, 'member'),
(22, 'root_admin', 'root', 'Test Admin', 'root.admin@fitness.local', '13800000102', 1, '1990-01-01', 1, 'admin');

-- restore demo user-role relations for repeatable RBAC regression
DELETE FROM user_role WHERE user_id IN (1,2,3,4,11,12,21,22);
INSERT IGNORE INTO user_role (user_id, role_id) SELECT 1, id FROM role WHERE role_code = 'ADMIN';
INSERT IGNORE INTO user_role (user_id, role_id) SELECT 2, id FROM role WHERE role_code = 'COACH';
INSERT IGNORE INTO user_role (user_id, role_id) SELECT 3, id FROM role WHERE role_code = 'MEMBER';
INSERT IGNORE INTO user_role (user_id, role_id) SELECT 4, id FROM role WHERE role_code = 'MEMBER';
INSERT IGNORE INTO user_role (user_id, role_id) SELECT 11, id FROM role WHERE role_code = 'OPS_ADMIN';
INSERT IGNORE INTO user_role (user_id, role_id) SELECT 12, id FROM role WHERE role_code = 'AUDIT_ADMIN';
INSERT IGNORE INTO user_role (user_id, role_id) SELECT 21, id FROM role WHERE role_code = 'MEMBER';
INSERT IGNORE INTO user_role (user_id, role_id) SELECT 22, id FROM role WHERE role_code = 'ADMIN';

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
('Operation Log Read', 'operation:log:read', 'system', 1);

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r CROSS JOIN permission p WHERE r.role_code = 'ADMIN';

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r
JOIN permission p ON p.permission_code IN (
  'banner:manage', 'notice:manage', 'system:config',
  'video:asset', 'video:upload', 'video:status', 'video:bind',
  'course:create', 'course:update', 'course:publish', 'course:schedule',
  'operation:log:read'
) WHERE r.role_code = 'OPS_ADMIN';

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r
JOIN permission p ON p.permission_code IN (
  'coach:apply:audit', 'refund:audit', 'pay:callback:audit', 'operation:log:read'
) WHERE r.role_code = 'AUDIT_ADMIN';

-- ensure role 1 course category scope exists (category 1) for regression
DELETE FROM role_course_category_scope WHERE role_id = 1;
INSERT IGNORE INTO role_course_category_scope (role_id, category_id)
VALUES (1, 1);
