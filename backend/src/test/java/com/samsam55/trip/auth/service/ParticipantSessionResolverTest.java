package com.samsam55.trip.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.samsam55.trip.auth.dto.ParticipantPrincipal;
import com.samsam55.trip.trip.entity.Participant;
import com.samsam55.trip.trip.entity.Trip;
import com.samsam55.trip.trip.repository.ParticipantRepository;
import com.samsam55.trip.trip.service.InviteService;
import com.samsam55.trip.trip.service.ParticipantCookieSigner;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParticipantSessionResolverTest {

    @Mock
    private ParticipantCookieSigner cookieSigner;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private HttpServletRequest servletRequest;

    @Mock
    private HttpSession session;

    @Mock
    private Trip trip;

    @Mock
    private Participant participant;

    private ParticipantSessionResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ParticipantSessionResolver(cookieSigner, participantRepository);
    }

    @Test
    @DisplayName("세션에 값이 있으면 쿠키를 확인하지 않고 세션 값을 그대로 반환한다")
    void 세션에_값이_있으면_쿠키를_확인하지_않는다() {
        when(servletRequest.getSession(false)).thenReturn(session);
        when(session.getAttribute(InviteService.PARTICIPANT_ID_SESSION_ATTRIBUTE)).thenReturn(12L);
        when(session.getAttribute(InviteService.TRIP_ID_SESSION_ATTRIBUTE)).thenReturn(1L);

        Optional<ParticipantPrincipal> result = resolver.resolve(servletRequest);

        assertThat(result).contains(new ParticipantPrincipal(12L, 1L));
        verify(servletRequest, never()).getCookies();
    }

    @Test
    @DisplayName("세션도 쿠키도 없으면 빈 값을 반환한다")
    void 세션도_쿠키도_없으면_빈_값을_반환한다() {
        when(servletRequest.getSession(false)).thenReturn(null);
        when(servletRequest.getCookies()).thenReturn(null);

        assertThat(resolver.resolve(servletRequest)).isEmpty();
    }

    @Test
    @DisplayName("복구 쿠키 서명이 유효하지 않으면 빈 값을 반환한다")
    void 복구_쿠키_서명이_유효하지_않으면_빈_값을_반환한다() {
        when(servletRequest.getSession(false)).thenReturn(null);
        when(servletRequest.getCookies())
                .thenReturn(new Cookie[]{new Cookie(InviteService.RECOVERY_COOKIE_NAME, "tampered")});
        when(cookieSigner.verify("tampered")).thenReturn(Optional.empty());

        assertThat(resolver.resolve(servletRequest)).isEmpty();
    }

    @Test
    @DisplayName("서명은 유효하지만 참여자가 DB에 없으면 빈 값을 반환한다")
    void 서명은_유효하지만_참여자가_없으면_빈_값을_반환한다() {
        when(servletRequest.getSession(false)).thenReturn(null);
        when(servletRequest.getCookies())
                .thenReturn(new Cookie[]{new Cookie(InviteService.RECOVERY_COOKIE_NAME, "12.sig")});
        when(cookieSigner.verify("12.sig")).thenReturn(Optional.of(12L));
        when(participantRepository.findById(12L)).thenReturn(Optional.empty());

        assertThat(resolver.resolve(servletRequest)).isEmpty();
    }

    @Test
    @DisplayName("복구 쿠키가 유효하면 세션을 다시 발급하고 참여자 정보를 반환한다")
    void 복구_쿠키가_유효하면_세션을_재발급하고_참여자_정보를_반환한다() {
        when(servletRequest.getSession(false)).thenReturn(null);
        when(servletRequest.getCookies())
                .thenReturn(new Cookie[]{new Cookie(InviteService.RECOVERY_COOKIE_NAME, "12.sig")});
        when(cookieSigner.verify("12.sig")).thenReturn(Optional.of(12L));
        when(participantRepository.findById(12L)).thenReturn(Optional.of(participant));
        when(participant.getId()).thenReturn(12L);
        when(participant.getTrip()).thenReturn(trip);
        when(trip.getId()).thenReturn(1L);
        when(servletRequest.getSession(true)).thenReturn(session);

        Optional<ParticipantPrincipal> result = resolver.resolve(servletRequest);

        assertThat(result).contains(new ParticipantPrincipal(12L, 1L));
        verify(session).setAttribute(InviteService.PARTICIPANT_ID_SESSION_ATTRIBUTE, 12L);
        verify(session).setAttribute(InviteService.TRIP_ID_SESSION_ATTRIBUTE, 1L);
    }
}
