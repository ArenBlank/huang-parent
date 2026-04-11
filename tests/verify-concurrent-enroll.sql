USE fitness_platform;

SET @target_schedule_id := 2;
SET @expected_count := 1;

SELECT
    cs.id,
    cs.course_id,
    cs.capacity,
    cs.booked_count,
    COUNT(ce.id) AS real_enrollment_count
FROM course_schedule cs
LEFT JOIN course_enrollment ce
       ON ce.schedule_id = cs.id
      AND ce.is_deleted = 0
WHERE cs.id = @target_schedule_id
GROUP BY cs.id, cs.course_id, cs.capacity, cs.booked_count;

SELECT
    CASE
        WHEN cs.booked_count = COUNT(ce.id)
         AND cs.booked_count = @expected_count
         AND cs.booked_count <= cs.capacity
        THEN 'PASS'
        ELSE 'FAIL'
    END AS assertion_result,
    cs.id,
    cs.capacity,
    cs.booked_count,
    COUNT(ce.id) AS real_enrollment_count,
    @expected_count AS expected_enrollment_count
FROM course_schedule cs
LEFT JOIN course_enrollment ce
       ON ce.schedule_id = cs.id
      AND ce.is_deleted = 0
WHERE cs.id = @target_schedule_id
GROUP BY cs.id, cs.capacity, cs.booked_count;

SELECT
    user_id,
    course_id,
    schedule_id,
    COUNT(*) AS duplicate_count
FROM course_enrollment
WHERE schedule_id = @target_schedule_id
  AND is_deleted = 0
GROUP BY user_id, course_id, schedule_id
HAVING COUNT(*) > 1;

SELECT
    id,
    user_id,
    course_id,
    schedule_id,
    order_id,
    status,
    enroll_time
FROM course_enrollment
WHERE schedule_id = @target_schedule_id
  AND is_deleted = 0
ORDER BY id ASC;
