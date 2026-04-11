USE fitness_platform;

SET @target_schedule_id := 2;

SELECT id, course_id, capacity, booked_count, status
FROM course_schedule
WHERE id = @target_schedule_id;

DROP TEMPORARY TABLE IF EXISTS tmp_target_order_ids;
CREATE TEMPORARY TABLE tmp_target_order_ids AS
SELECT DISTINCT order_id
FROM course_enrollment
WHERE schedule_id = @target_schedule_id
  AND order_id IS NOT NULL;

DELETE FROM order_item
WHERE order_id IN (SELECT order_id FROM tmp_target_order_ids);

DELETE FROM payment_record
WHERE order_id IN (SELECT order_id FROM tmp_target_order_ids);

DELETE FROM refund_record
WHERE order_id IN (SELECT order_id FROM tmp_target_order_ids);

DELETE FROM order_info
WHERE id IN (SELECT order_id FROM tmp_target_order_ids);

DELETE FROM course_enrollment
WHERE schedule_id = @target_schedule_id;

DROP TEMPORARY TABLE IF EXISTS tmp_target_order_ids;

UPDATE course_schedule
SET capacity = 5,
    booked_count = 0,
    status = 1
WHERE id = @target_schedule_id;

SELECT id, course_id, capacity, booked_count, status
FROM course_schedule
WHERE id = @target_schedule_id;
