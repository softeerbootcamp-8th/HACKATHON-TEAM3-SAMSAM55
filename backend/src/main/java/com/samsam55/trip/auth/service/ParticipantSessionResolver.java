package com.samsam55.trip.auth.service;

import com.samsam55.trip.auth.dto.ParticipantPrincipal;
import com.samsam55.trip.trip.entity.Participant;
import com.samsam55.trip.trip.repository.ParticipantRepository;
import com.samsam55.trip.trip.service.InviteService;
import com.samsam55.trip.trip.service.ParticipantCookieSigner;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 참여자 세션을 조회하고, 세션이 없으면 복구용 쿠키로 세션을 다시 발급한다.
 *
 * <p>평소에는 세션에 담긴 {@code participantId}/{@code tripId}로 빠르게 인증하고,
 * 세션이 없을 때(서버 재시작, 세션 만료 등)만 서명된 복구 쿠키를 검증해 DB에서
 * {@code participant} 존재를 확인한 뒤 세션을 다시 발급한다.
 * {@code ParticipantArgumentResolver}와 {@link AuthService#me}가 함께 사용한다.
 */
@Component
@RequiredArgsConstructor
public class ParticipantSessionResolver {

    private final ParticipantCookieSigner cookieSigner;
    private final ParticipantRepository participantRepository;

    /**
     * 세션 또는 복구용 쿠키로 참여자를 식별한다.
     *
     * @param request 현재 HTTP 요청
     * @return 식별에 성공하면 참여자 정보, 세션도 쿠키도 유효하지 않으면 빈 값
     */
    public Optional<ParticipantPrincipal> resolve(HttpServletRequest request) {
        Optional<ParticipantPrincipal> fromSession = resolveFromSession(request);
        if (fromSession.isPresent()) {
            return fromSession;
        }
        return resolveFromRecoveryCookie(request);
    }

    private Optional<ParticipantPrincipal> resolveFromSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Optional.empty();
        }

        Object participantId = session.getAttribute(InviteService.PARTICIPANT_ID_SESSION_ATTRIBUTE);
        Object tripId = session.getAttribute(InviteService.TRIP_ID_SESSION_ATTRIBUTE);
        if (!(participantId instanceof Number) || !(tripId instanceof Number)) {
            return Optional.empty();
        }

        return Optional.of(new ParticipantPrincipal(
                ((Number) participantId).longValue(),
                ((Number) tripId).longValue()
        ));
    }

    private Optional<ParticipantPrincipal> resolveFromRecoveryCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        for (Cookie cookie : cookies) {
            if (!InviteService.RECOVERY_COOKIE_NAME.equals(cookie.getName())) {
                continue;
            }

            Optional<ParticipantPrincipal> principal = cookieSigner.verify(cookie.getValue())
                    .flatMap(participantRepository::findById)
                    .map(participant -> reissueSession(request, participant));
            if (principal.isPresent()) {
                return principal;
            }
        }
        return Optional.empty();
    }

    private ParticipantPrincipal reissueSession(HttpServletRequest request, Participant participant) {
        HttpSession session = request.getSession(true);
        session.setAttribute(InviteService.PARTICIPANT_ID_SESSION_ATTRIBUTE, participant.getId());
        session.setAttribute(InviteService.TRIP_ID_SESSION_ATTRIBUTE, participant.getTrip().getId());
        return new ParticipantPrincipal(participant.getId(), participant.getTrip().getId());
    }
}
