package com.samsam55.trip.trip.service;

import com.samsam55.trip.auth.service.AuthService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InviteService {

    public static final String PARTICIPANT_ID_SESSION_ATTRIBUTE = "PARTICIPANT_ID";
    public static final String TRIP_ID_SESSION_ATTRIBUTE = "PARTICIPANT_TRIP_ID";
    public static final String RECOVERY_COOKIE_NAME = "PARTICIPANT_TOKEN";
    public static final int RECOVERY_COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 30;

    private final TripRepository tripRepository;
    private final ParticipantRepository participantRepository;
    private final ParticipantCookieSigner cookieSigner;

    /**
     * 초대 코드로 여행과 참여자 슬롯 선점 현황을 조회한다.
     *
     * @param inviteCode 초대 코드
     * @return 여행 정보와 역할별 선점 여부가 담긴 참여자 슬롯 목록
     * @throws ApplicationException 초대 코드에 해당하는 여행이 없을 때(INVITE_CODE_NOT_FOUND)
     */
    @Transactional(readOnly = true)
    public InviteVerifyResponseDto verify(String inviteCode) {
        Trip trip = tripRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new ApplicationException(TripErrorType.INVITE_CODE_NOT_FOUND));

        List<Participant> participants = participantRepository.findAllByTripOrderById(trip);
        return InviteVerifyResponseDto.of(trip, participants);
    }

    /**
     * 참여자 슬롯 하나를 선점하고 세션과 복구용 서명 쿠키를 발급한다.
     *
     * @param inviteCode 초대 코드
     * @param request 선점할 참여자 슬롯 ID
     * @param servletRequest 세션을 생성할 현재 HTTP 요청
     * @param servletResponse 복구용 쿠키를 내려줄 현재 HTTP 응답
     * @return 선점한 참여자 정보
     * @throws ApplicationException 현재 세션에 이미 참여자 정보가 있을 때(ALREADY_PARTICIPANT),
     * @throws ApplicationException 초대 코드에 해당하는 여행이 없을 때(INVITE_CODE_NOT_FOUND),
     *         participantId가 그 여행 소속이 아닐 때(PARTICIPANT_NOT_FOUND),
     *         이미 다른 사람이 선점한 슬롯일 때(PARTICIPANT_ALREADY_JOINED)
     */
    @Transactional
    public InviteJoinResponseDto join(
            String inviteCode,
            InviteJoinRequestDto request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        HttpSession session = servletRequest.getSession(false);
        if (session != null && session.getAttribute(PARTICIPANT_ID_SESSION_ATTRIBUTE) != null) {
            throw new ApplicationException(TripErrorType.ALREADY_PARTICIPANT);
        }

        Trip trip = tripRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new ApplicationException(TripErrorType.INVITE_CODE_NOT_FOUND));

        Participant participant = participantRepository.findByIdAndTrip(request.participantId(), trip)
                .orElseThrow(() -> new ApplicationException(TripErrorType.PARTICIPANT_NOT_FOUND));

        if (participant.getJoinedAt() != null) {
            throw new ApplicationException(TripErrorType.PARTICIPANT_ALREADY_JOINED);
        }

        participant.join(LocalDateTime.now());

        issueSession(servletRequest, participant);
        issueRecoveryCookie(servletResponse, participant);

        return InviteJoinResponseDto.from(participant);
    }

    private void issueSession(HttpServletRequest servletRequest, Participant participant) {
        HttpSession session = servletRequest.getSession(true);
        servletRequest.changeSessionId();
        session.removeAttribute(AuthService.LOGIN_USER_ID_SESSION_ATTRIBUTE);
        session.setAttribute(PARTICIPANT_ID_SESSION_ATTRIBUTE, participant.getId());
        session.setAttribute(TRIP_ID_SESSION_ATTRIBUTE, participant.getTrip().getId());
    }

    private void issueRecoveryCookie(HttpServletResponse servletResponse, Participant participant) {
        Cookie cookie = new Cookie(RECOVERY_COOKIE_NAME, cookieSigner.sign(participant.getId()));
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(RECOVERY_COOKIE_MAX_AGE_SECONDS);
        servletResponse.addCookie(cookie);
    }
}
