package com.samsam55.trip.auth.argumentresolver;

import com.samsam55.trip.auth.annotation.CurrentParticipant;
import com.samsam55.trip.auth.dto.ParticipantPrincipal;
import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.trip.entity.Participant;
import com.samsam55.trip.trip.exception.TripErrorType;
import com.samsam55.trip.trip.repository.ParticipantRepository;
import com.samsam55.trip.trip.service.InviteService;
import com.samsam55.trip.trip.service.ParticipantCookieSigner;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 참여자 세션을 꺼내거나, 세션이 없으면 복구용 쿠키로 세션을 다시 발급한다.
 *
 * <p>평소에는 세션에 담긴 {@code participantId}/{@code tripId}를 그대로 써서
 * DB 조회 없이 빠르게 인증한다. 세션이 없을 때(서버 재시작, 세션 만료 등)만
 * {@link ParticipantCookieSigner}로 복구용 쿠키를 검증하고 DB에서 {@code participant}가
 * 실제로 존재하는지 확인한 뒤 세션을 다시 발급해 자동 복구한다.
 */
@Component
@RequiredArgsConstructor
public class ParticipantArgumentResolver implements HandlerMethodArgumentResolver {

    private final ParticipantCookieSigner cookieSigner;
    private final ParticipantRepository participantRepository;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentParticipant.class)
                && parameter.getParameterType() == ParticipantPrincipal.class;
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new ApplicationException(TripErrorType.PARTICIPANT_LOGIN_REQUIRED);
        }

        Optional<ParticipantPrincipal> fromSession = resolveFromSession(request);
        if (fromSession.isPresent()) {
            return fromSession.get();
        }

        return resolveFromRecoveryCookie(request)
                .orElseThrow(() -> new ApplicationException(TripErrorType.PARTICIPANT_LOGIN_REQUIRED));
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
