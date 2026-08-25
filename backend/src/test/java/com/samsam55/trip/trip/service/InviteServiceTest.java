package com.samsam55.trip.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.trip.dto.InviteJoinRequestDto;
import com.samsam55.trip.trip.dto.InviteJoinResponseDto;
import com.samsam55.trip.trip.dto.InviteVerifyResponseDto;
import com.samsam55.trip.trip.entity.Participant;
import com.samsam55.trip.trip.entity.Trip;
import com.samsam55.trip.trip.exception.TripErrorType;
import com.samsam55.trip.trip.repository.ParticipantRepository;
import com.samsam55.trip.trip.repository.TripRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InviteServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private ParticipantCookieSigner cookieSigner;

    @Mock
    private HttpServletRequest servletRequest;

    @Mock
    private HttpServletResponse servletResponse;

    @Mock
    private HttpSession session;

    @Mock
    private Trip trip;

    @Mock
    private Participant participant;

    private InviteService inviteService;

    private InviteService newInviteService() {
        return new InviteService(tripRepository, participantRepository, cookieSigner);
    }

    @Test
    @DisplayName("존재하지 않는 초대 코드로 조회하면 INVITE_CODE_NOT_FOUND를 던진다")
    void 존재하지_않는_초대_코드로_조회하면_에러를_던진다() {
        inviteService = newInviteService();
        when(tripRepository.findByInviteCode("invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inviteService.verify("invalid"))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.INVITE_CODE_NOT_FOUND));
    }

    @Test
    @DisplayName("유효한 초대 코드는 여행 정보와 참여자 슬롯 목록을 반환한다")
    void 유효한_초대_코드는_여행과_참여자_슬롯_목록을_반환한다() {
        inviteService = newInviteService();
        when(tripRepository.findByInviteCode("valid")).thenReturn(Optional.of(trip));
        when(trip.getId()).thenReturn(1L);
        when(trip.getTitle()).thenReturn("제주 가족 여행");
        when(participant.getId()).thenReturn(12L);
        when(participant.getRoleName()).thenReturn("외할머니");
        when(participant.getJoinedAt()).thenReturn(null);
        when(participantRepository.findAllByTripOrderById(trip)).thenReturn(List.of(participant));

        InviteVerifyResponseDto response = inviteService.verify("valid");

        assertThat(response.tripId()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("제주 가족 여행");
        assertThat(response.participants()).hasSize(1);
        assertThat(response.participants().get(0).participantId()).isEqualTo(12L);
        assertThat(response.participants().get(0).roleName()).isEqualTo("외할머니");
        assertThat(response.participants().get(0).joined()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 초대 코드로 입장하면 INVITE_CODE_NOT_FOUND를 던진다")
    void 존재하지_않는_초대_코드로_입장하면_에러를_던진다() {
        inviteService = newInviteService();
        when(tripRepository.findByInviteCode("invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inviteService.join(
                "invalid", new InviteJoinRequestDto(12L), servletRequest, servletResponse
        ))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.INVITE_CODE_NOT_FOUND));
    }

    @Test
    @DisplayName("그 여행 소속이 아닌 participantId로 입장하면 PARTICIPANT_NOT_FOUND를 던진다")
    void 여행_소속이_아닌_참여자로_입장하면_에러를_던진다() {
        inviteService = newInviteService();
        when(tripRepository.findByInviteCode("valid")).thenReturn(Optional.of(trip));
        when(participantRepository.findByIdAndTrip(99L, trip)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inviteService.join(
                "valid", new InviteJoinRequestDto(99L), servletRequest, servletResponse
        ))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.PARTICIPANT_NOT_FOUND));
    }

    @Test
    @DisplayName("이미 선점된 슬롯을 다시 선택하면 PARTICIPANT_ALREADY_JOINED를 던진다")
    void 이미_선점된_슬롯을_다시_선택하면_에러를_던진다() {
        inviteService = newInviteService();
        when(tripRepository.findByInviteCode("valid")).thenReturn(Optional.of(trip));
        when(participantRepository.findByIdAndTrip(12L, trip)).thenReturn(Optional.of(participant));
        when(participant.getJoinedAt()).thenReturn(LocalDateTime.now());

        assertThatThrownBy(() -> inviteService.join(
                "valid", new InviteJoinRequestDto(12L), servletRequest, servletResponse
        ))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.PARTICIPANT_ALREADY_JOINED));

        verify(participant, never()).join(any());
    }

    @Test
    @DisplayName("빈 슬롯 입장에 성공하면 슬롯을 선점하고 세션과 복구용 쿠키를 발급한다")
    void 빈_슬롯_입장에_성공하면_선점하고_세션과_쿠키를_발급한다() {
        inviteService = newInviteService();
        when(tripRepository.findByInviteCode("valid")).thenReturn(Optional.of(trip));
        when(participantRepository.findByIdAndTrip(12L, trip)).thenReturn(Optional.of(participant));
        when(participant.getJoinedAt()).thenReturn(null);
        when(participant.getId()).thenReturn(12L);
        when(participant.getTrip()).thenReturn(trip);
        when(participant.getRoleName()).thenReturn("외할머니");
        when(trip.getId()).thenReturn(1L);
        when(servletRequest.getSession(true)).thenReturn(session);
        when(cookieSigner.sign(12L)).thenReturn("12.signature");

        InviteJoinResponseDto response = inviteService.join(
                "valid", new InviteJoinRequestDto(12L), servletRequest, servletResponse
        );

        assertThat(response.participantId()).isEqualTo(12L);
        assertThat(response.tripId()).isEqualTo(1L);
        assertThat(response.roleName()).isEqualTo("외할머니");

        verify(participant, times(1)).join(any(LocalDateTime.class));
        verify(servletRequest).changeSessionId();
        verify(session).setAttribute(InviteService.PARTICIPANT_ID_SESSION_ATTRIBUTE, 12L);
        verify(session).setAttribute(InviteService.TRIP_ID_SESSION_ATTRIBUTE, 1L);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(servletResponse).addCookie(cookieCaptor.capture());
        Cookie cookie = cookieCaptor.getValue();
        assertThat(cookie.getName()).isEqualTo(InviteService.RECOVERY_COOKIE_NAME);
        assertThat(cookie.getValue()).isEqualTo("12.signature");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getMaxAge()).isEqualTo(InviteService.RECOVERY_COOKIE_MAX_AGE_SECONDS);
    }
}
