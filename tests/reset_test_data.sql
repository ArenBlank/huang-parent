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
