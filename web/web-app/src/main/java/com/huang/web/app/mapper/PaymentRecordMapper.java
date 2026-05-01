package com.huang.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huang.model.entity.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * PaymentRecordMapper接口
 * @author system
 * @since 2026-02-25
 */
@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {

    @Update("""
            UPDATE payment_record
            SET pay_status = 'PAID',
                pay_time = #{paidAt},
                callback_idempotency_key = #{callbackIdempotencyKey}
            WHERE id = #{paymentRecordId}
              AND is_deleted = 0
              AND pay_status <> 'PAID'
            """)
    int markPaidIfUnpaid(@Param("paymentRecordId") Long paymentRecordId,
                         @Param("callbackIdempotencyKey") String callbackIdempotencyKey,
                         @Param("paidAt") LocalDateTime paidAt);
}
