package com.samsam55.trip.trip.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 참여자 복구용 쿠키 값을 서명하고 검증한다.
 *
 * <p>참여자는 비밀번호가 없어 재로그인 수단이 없으므로, 세션이 없을 때(서버 재시작,
 * 세션 만료 등) 이 쿠키로 세션을 다시 발급해 자동 복구한다. 쿠키 값은
 * {@code "{participantId}.{서명}"} 형식이며, 서명 없이 participantId를 그대로
 * 담으면 다른 사람이 값을 바꿔서 남의 역할을 훔쳐볼 수 있기 때문에 반드시
 * 서버 비밀키로 서명한다.
 *
 * <p><b>알려진 제약</b>: 쿠키가 삭제되면(브라우저 초기화, 기기 변경 등) 해당
 * participant의 역할 슬롯은 재인증 수단이 없어 영구히 선점된 채로 남는다.
 * 참여자는 비밀번호가 없어 "내가 진짜 그 역할이다"를 증명할 방법이 없으므로,
 * 이 제약은 해결하지 않고 감수하기로 팀에서 결정했다 (해커톤 범위).
 */
@Component
public class ParticipantCookieSigner {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final SecretKeySpec secretKey;

    public ParticipantCookieSigner(
            @Value("${samsam55.participant-cookie.secret:dev-secret-please-change}") String secret
    ) {
        this.secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
    }

    /**
     * participantId를 서버 비밀키로 서명한 쿠키 값을 만든다.
     *
     * @param participantId 서명할 참여자 ID
     * @return {@code "{participantId}.{서명}"} 형식의 쿠키 값
     */
    public String sign(Long participantId) {
        String payload = String.valueOf(participantId);
        return payload + "." + hmac(payload);
    }

    /**
     * 쿠키 값의 서명을 검증하고 participantId를 꺼낸다.
     *
     * @param cookieValue 쿠키에 담긴 값
     * @return 서명이 유효하면 participantId, 위변조됐거나 형식이 잘못됐으면 빈 값
     */
    public Optional<Long> verify(String cookieValue) {
        int separatorIndex = cookieValue.lastIndexOf('.');
        if (separatorIndex <= 0 || separatorIndex == cookieValue.length() - 1) {
            return Optional.empty();
        }

        String payload = cookieValue.substring(0, separatorIndex);
        String signature = cookieValue.substring(separatorIndex + 1);
        boolean signatureMatches = MessageDigest.isEqual(
                signature.getBytes(StandardCharsets.UTF_8),
                hmac(payload).getBytes(StandardCharsets.UTF_8)
        );
        if (!signatureMatches) {
            return Optional.empty();
        }

        try {
            return Optional.of(Long.parseLong(payload));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private String hmac(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(secretKey);
            byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new IllegalStateException("쿠키 서명 처리 중 오류가 발생했습니다.", e);
        }
    }
}
