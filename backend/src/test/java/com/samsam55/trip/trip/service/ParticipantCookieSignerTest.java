package com.samsam55.trip.trip.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ParticipantCookieSignerTest {

    private ParticipantCookieSigner cookieSigner;

    @BeforeEach
    void setUp() {
        cookieSigner = new ParticipantCookieSigner("test-secret");
    }

    @Test
    @DisplayName("서명한 값은 검증에 성공하고 participantId를 그대로 돌려준다")
    void 서명한_값은_검증에_성공하고_participantId를_돌려준다() {
        String signed = cookieSigner.sign(12L);

        assertThat(cookieSigner.verify(signed)).contains(12L);
    }

    @Test
    @DisplayName("participantId 부분을 위조하면 검증에 실패한다")
    void participantId_부분을_위조하면_검증에_실패한다() {
        String signed = cookieSigner.sign(12L);
        String tampered = signed.replaceFirst("^12", "13");

        assertThat(cookieSigner.verify(tampered)).isEmpty();
    }

    @Test
    @DisplayName("다른 비밀키로 서명한 값은 검증에 실패한다")
    void 다른_비밀키로_서명한_값은_검증에_실패한다() {
        String signed = cookieSigner.sign(12L);
        ParticipantCookieSigner otherSigner = new ParticipantCookieSigner("other-secret");

        assertThat(otherSigner.verify(signed)).isEmpty();
    }

    @Test
    @DisplayName("형식이 잘못된 쿠키 값은 검증에 실패한다")
    void 형식이_잘못된_쿠키_값은_검증에_실패한다() {
        assertThat(cookieSigner.verify("no-separator")).isEmpty();
        assertThat(cookieSigner.verify(".no-payload")).isEmpty();
        assertThat(cookieSigner.verify("12.")).isEmpty();
    }
}
