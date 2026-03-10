package com.huang.web.app.service.pay;

import com.huang.web.app.dto.pay.PayCallbackDTO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "payment.sign.mode", havingValue = "mock", matchIfMissing = true)
public class MockPaymentSignVerifier implements PaymentSignVerifier {

    private static final String MOCK_SIGN_PREFIX = "MOCK:";

    @Override
    public boolean verify(PayCallbackDTO dto) {
        return generateMockSign(dto.getPayNo()).equals(dto.getSign());
    }

    @Override
    public String generateMockSign(String payNo) {
        return MOCK_SIGN_PREFIX + payNo;
    }
}
