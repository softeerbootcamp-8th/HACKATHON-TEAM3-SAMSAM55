package com.samsam55.trip.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.samsam55.trip.auth.dto.ActorPrincipal;
import com.samsam55.trip.auth.dto.ParticipantPrincipal;
import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.member.entity.User;
import com.samsam55.trip.trip.dto.VoteOptionImageDto;
import com.samsam55.trip.trip.entity.ItineraryItem;
import com.samsam55.trip.trip.entity.ItineraryItemStatus;
import com.samsam55.trip.trip.entity.Participant;
import com.samsam55.trip.trip.entity.Trip;
import com.samsam55.trip.trip.entity.TripDay;
import com.samsam55.trip.trip.entity.VoteOption;
import com.samsam55.trip.trip.exception.TripErrorType;
import com.samsam55.trip.trip.repository.ParticipantRepository;
import com.samsam55.trip.trip.repository.VoteOptionRepository;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VoteOptionServiceTest {

    @Mock
    private VoteOptionRepository voteOptionRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private ItineraryItem itineraryItem;

    @Mock
    private TripDay tripDay;

    @Mock
    private Trip trip;

    @Mock
    private User hostUser;

    @Mock
    private Participant participant;

    private VoteOptionService voteOptionService;

    @BeforeEach
    void setUp() {
        voteOptionService = new VoteOptionService(voteOptionRepository, participantRepository);
    }

    @Test
    @DisplayName("이미지가 있는 선택지는 바이트와 콘텐츠 타입을 반환한다")
    void 이미지가_있는_선택지는_바이트와_콘텐츠_타입을_반환한다() {
        byte[] bytes = "image-bytes".getBytes(StandardCharsets.UTF_8);
        VoteOption voteOption = hostVoteOption(bytes, "image/jpeg");
        when(voteOptionRepository.findByIdWithTrip(1L)).thenReturn(Optional.of(voteOption));

        VoteOptionImageDto image = voteOptionService.getImage(ActorPrincipal.ofHost(1L), 1L);

        assertThat(image.data()).isEqualTo(bytes);
        assertThat(image.contentType()).isEqualTo("image/jpeg");
        verifyNoInteractions(participantRepository);
    }

    @Test
    @DisplayName("선택지를 찾을 수 없으면 예외가 발생한다")
    void 선택지를_찾을_수_없으면_예외가_발생한다() {
        when(voteOptionRepository.findByIdWithTrip(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> voteOptionService.getImage(ActorPrincipal.ofHost(1L), 1L))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.VOTE_OPTION_NOT_FOUND));
    }

    @Test
    @DisplayName("선택지에 이미지가 없으면 예외가 발생한다")
    void 선택지에_이미지가_없으면_예외가_발생한다() {
        VoteOption voteOption = hostVoteOption(null, null);
        when(voteOptionRepository.findByIdWithTrip(1L)).thenReturn(Optional.of(voteOption));

        assertThatThrownBy(() -> voteOptionService.getImage(ActorPrincipal.ofHost(1L), 1L))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.VOTE_OPTION_IMAGE_NOT_FOUND));
    }

    @Test
    @DisplayName("다른 HOST가 소유한 선택지는 조회할 수 없다")
    void 다른_HOST가_소유한_선택지는_조회할_수_없다() {
        VoteOption voteOption = hostVoteOption(new byte[]{1}, "image/png");
        when(voteOptionRepository.findByIdWithTrip(1L)).thenReturn(Optional.of(voteOption));

        assertThatThrownBy(() -> voteOptionService.getImage(ActorPrincipal.ofHost(99L), 1L))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.VOTE_OPTION_NOT_FOUND));
        verifyNoInteractions(participantRepository);
    }

    @Test
    @DisplayName("PARTICIPANT는 같은 여행의 공개된 선택지 이미지를 조회할 수 있다")
    void PARTICIPANT는_같은_여행의_공개된_선택지_이미지를_조회할_수_있다() {
        VoteOption voteOption = participantVoteOption(ItineraryItemStatus.VOTING, new byte[]{1}, "image/png");
        when(voteOptionRepository.findByIdWithTrip(1L)).thenReturn(Optional.of(voteOption));
        when(participantRepository.findByIdAndTrip(12L, trip)).thenReturn(Optional.of(participant));

        VoteOptionImageDto image = voteOptionService.getImage(
                ActorPrincipal.ofParticipant(new ParticipantPrincipal(12L, 10L)), 1L);

        assertThat(image.data()).containsExactly(1);
    }

    @Test
    @DisplayName("PARTICIPANT는 다른 여행의 선택지를 조회할 수 없다")
    void PARTICIPANT는_다른_여행의_선택지를_조회할_수_없다() {
        VoteOption voteOption = participantVoteOption(ItineraryItemStatus.VOTING, new byte[]{1}, "image/png");
        when(voteOptionRepository.findByIdWithTrip(1L)).thenReturn(Optional.of(voteOption));

        assertThatThrownBy(() -> voteOptionService.getImage(
                ActorPrincipal.ofParticipant(new ParticipantPrincipal(12L, 99L)), 1L))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.VOTE_OPTION_NOT_FOUND));
        verifyNoInteractions(participantRepository);
    }

    @Test
    @DisplayName("PARTICIPANT는 준비 중인 선택지를 조회할 수 없다")
    void PARTICIPANT는_준비_중인_선택지를_조회할_수_없다() {
        VoteOption voteOption = pendingVoteOption(new byte[]{1}, "image/png");
        when(voteOptionRepository.findByIdWithTrip(1L)).thenReturn(Optional.of(voteOption));

        assertThatThrownBy(() -> voteOptionService.getImage(
                ActorPrincipal.ofParticipant(new ParticipantPrincipal(12L, 10L)), 1L))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.VOTE_OPTION_NOT_FOUND));
        verifyNoInteractions(participantRepository);
    }

    private VoteOption voteOption(byte[] image, String contentType) {
        when(itineraryItem.getTripDay()).thenReturn(tripDay);
        when(tripDay.getTrip()).thenReturn(trip);
        return new VoteOption(itineraryItem, "스시", "설명", "AI", image, contentType);
    }

    private VoteOption hostVoteOption(byte[] image, String contentType) {
        VoteOption voteOption = voteOption(image, contentType);
        when(trip.getHostUser()).thenReturn(hostUser);
        when(hostUser.getId()).thenReturn(1L);
        return voteOption;
    }

    private VoteOption participantVoteOption(ItineraryItemStatus status, byte[] image, String contentType) {
        VoteOption voteOption = voteOption(image, contentType);
        when(itineraryItem.getStatus()).thenReturn(status);
        when(trip.getId()).thenReturn(10L);
        return voteOption;
    }

    private VoteOption pendingVoteOption(byte[] image, String contentType) {
        VoteOption voteOption = voteOption(image, contentType);
        when(itineraryItem.getStatus()).thenReturn(ItineraryItemStatus.PENDING);
        return voteOption;
    }
}
