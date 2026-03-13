package com.huang.web.app.service.pay;

import com.huang.web.app.dto.pay.PayCallbackDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Component
@ConditionalOnProperty(name = "payment.sign.mode", havingValue = "hmac")
public class HmacPaymentSignVerifier implements PaymentSignVerifier {

    @Value("${payment.sign.secret:}")
    private String secret;

    @Override
    public boolean verify(PayCallbackDTO dto) {
        if (dto == null || secret == null || secret.isBlank()) {
            return false;
        }
        String payload = buildPayload(dto);
        String expected = sign(payload);
        return expected.equals(dto.getSign());
    }

    @Override
    public String generateSign(PayCallbackDTO dto) {
        if (dto == null) {
            return "";
        }
        if (secret == null || secret.isBlank()) {
            return "";
        }
        String payload = buildPayload(dto);
        return sign(payload);
    }

    private String buildPayload(PayCallbackDTO dto) {
        String amount = dto.getAmount() == null ? "" : dto.getAmount().toPlainString();
        return dto.getPayNo() + "|" + dto.getTradeNo() + "|" + dto.getStatus() + "|" + amount;
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception ex) {
            return "";
        }
    }
}
