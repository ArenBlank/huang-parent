-- Reset occupancy and pending records for local regression reruns
USE fitness_platform;

-- reset schedule occupancy
UPDATE coach_schedule SET booked_count = 0 WHERE is_deleted = 0;
UPDATE course_schedule SET booked_count = 0 WHERE is_deleted = 0;

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

-- cleanup autotest operation data
DELETE FROM banner WHERE title LIKE 'AutoTest Banner%';
DELETE FROM notice WHERE title LIKE 'AutoTest Notice%';
DELETE FROM system_config WHERE config_key LIKE 'autotest.%';

-- restore demo user-role relations for repeatable RBAC regression
DELETE FROM user_role WHERE user_id IN (1,2,3,11,12);
INSERT IGNORE INTO user_role (user_id, role_id) SELECT 1, id FROM role WHERE role_code = 'ADMIN';
INSERT IGNORE INTO user_role (user_id, role_id) SELECT 2, id FROM role WHERE role_code = 'COACH';
INSERT IGNORE INTO user_role (user_id, role_id) SELECT 3, id FROM role WHERE role_code = 'MEMBER';
INSERT IGNORE INTO user_role (user_id, role_id) SELECT 11, id FROM role WHERE role_code = 'OPS_ADMIN';
INSERT IGNORE INTO user_role (user_id, role_id) SELECT 12, id FROM role WHERE role_code = 'AUDIT_ADMIN';
