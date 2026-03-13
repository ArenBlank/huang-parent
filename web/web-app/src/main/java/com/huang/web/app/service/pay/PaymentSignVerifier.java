package com.huang.web.app.service.pay;

import com.huang.web.app.dto.pay.PayCallbackDTO;

public interface PaymentSignVerifier {

    boolean verify(PayCallbackDTO dto);

    String generateSign(PayCallbackDTO dto);
}
