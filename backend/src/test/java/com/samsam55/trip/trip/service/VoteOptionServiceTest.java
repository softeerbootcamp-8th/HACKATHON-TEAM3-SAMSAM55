package com.samsam55.trip.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.member.entity.User;
import com.samsam55.trip.trip.ai.VoteOptionDescriptionGenerator;
import com.samsam55.trip.trip.dto.VoteOptionCreateResponseDto;
import com.samsam55.trip.trip.dto.VoteOptionImageDto;
import com.samsam55.trip.trip.entity.ItineraryItem;
import com.samsam55.trip.trip.entity.ItineraryItemDecisionType;
import com.samsam55.trip.trip.entity.ItineraryItemStatus;
import com.samsam55.trip.trip.entity.Trip;
import com.samsam55.trip.trip.entity.TripDay;
import com.samsam55.trip.trip.entity.VoteOption;
import com.samsam55.trip.trip.exception.TripErrorType;
import com.samsam55.trip.trip.repository.ItineraryItemRepository;
import com.samsam55.trip.trip.repository.VoteOptionRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VoteOptionServiceTest {

    @Mock
    private VoteOptionRepository voteOptionRepository;

    @Mock
    private ItineraryItemRepository itineraryItemRepository;

    @Mock
    private VoteOptionDescriptionGenerator descriptionGenerator;

    @Mock
    private ItineraryItem itineraryItem;

    private VoteOptionService voteOptionService;

    @BeforeEach
    void setUp() {
        voteOptionService = new VoteOptionService(
                voteOptionRepository, itineraryItemRepository, descriptionGenerator);
    }

    @Test
    @DisplayName("이미지가 있는 선택지는 바이트와 콘텐츠 타입을 반환한다")
    void 이미지가_있는_선택지는_바이트와_콘텐츠_타입을_반환한다() {
        byte[] bytes = "image-bytes".getBytes(StandardCharsets.UTF_8);
        VoteOption voteOption = new VoteOption(itineraryItem, "스시", "설명", "AI", bytes, "image/jpeg");
        when(voteOptionRepository.findById(1L)).thenReturn(Optional.of(voteOption));

        VoteOptionImageDto image = voteOptionService.getImage(1L);

        assertThat(image.data()).isEqualTo(bytes);
        assertThat(image.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("선택지를 찾을 수 없으면 예외가 발생한다")
    void 선택지를_찾을_수_없으면_예외가_발생한다() {
        when(voteOptionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> voteOptionService.getImage(1L))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.VOTE_OPTION_NOT_FOUND));
    }

    @Test
    @DisplayName("선택지에 이미지가 없으면 예외가 발생한다")
    void 선택지에_이미지가_없으면_예외가_발생한다() {
        VoteOption voteOption = new VoteOption(itineraryItem, "스시", "설명", "AI", null, null);
        when(voteOptionRepository.findById(1L)).thenReturn(Optional.of(voteOption));

        assertThatThrownBy(() -> voteOptionService.getImage(1L))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.VOTE_OPTION_IMAGE_NOT_FOUND));
    }

    private ItineraryItem pendingItineraryItem(Long hostUserId) {
        User hostUser = new User("host", "hashed-password");
        ReflectionTestUtils.setField(hostUser, "id", hostUserId);
        Trip trip = new Trip(hostUser, "제주 여행",
                LocalDateTime.of(2026, 9, 1, 9, 0), LocalDateTime.of(2026, 9, 3, 18, 0), 3, "invite-code");
        TripDay tripDay = new TripDay(trip, 1, LocalDate.of(2026, 9, 1));
        return new ItineraryItem(
                tripDay, "점심 메뉴", "식사", ItineraryItemDecisionType.VOTE, ItineraryItemStatus.PENDING, 1, null);
    }

    @Test
    @DisplayName("방장이 PENDING 항목의 선택지를 삭제할 수 있다")
    void 방장이_PENDING_항목의_선택지를_삭제할_수_있다() {
        ItineraryItem pendingItem = pendingItineraryItem(1L);
        VoteOption voteOption = new VoteOption(pendingItem, "스시", "설명", "AI", null, null);
        when(voteOptionRepository.findById(1L)).thenReturn(Optional.of(voteOption));

        voteOptionService.deleteVoteOption(1L, 1L);

        verify(voteOptionRepository).delete(voteOption);
    }

    @Test
    @DisplayName("선택지 삭제 시 선택지를 찾을 수 없으면 예외가 발생한다")
    void 선택지_삭제_시_선택지를_찾을_수_없으면_예외가_발생한다() {
        when(voteOptionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> voteOptionService.deleteVoteOption(1L, 1L))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.VOTE_OPTION_NOT_FOUND));
        verify(voteOptionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("방장이 아니면 선택지를 삭제할 수 없다")
    void 방장이_아니면_선택지를_삭제할_수_없다() {
        ItineraryItem pendingItem = pendingItineraryItem(1L);
        VoteOption voteOption = new VoteOption(pendingItem, "스시", "설명", "AI", null, null);
        when(voteOptionRepository.findById(1L)).thenReturn(Optional.of(voteOption));

        assertThatThrownBy(() -> voteOptionService.deleteVoteOption(999L, 1L))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.NOT_TRIP_HOST));
        verify(voteOptionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("투표가 이미 시작된 일정 항목의 선택지는 삭제할 수 없다")
    void 투표가_이미_시작된_일정_항목의_선택지는_삭제할_수_없다() {
        User hostUser = new User("host", "hashed-password");
        ReflectionTestUtils.setField(hostUser, "id", 1L);
        Trip trip = new Trip(hostUser, "제주 여행",
                LocalDateTime.of(2026, 9, 1, 9, 0), LocalDateTime.of(2026, 9, 3, 18, 0), 3, "invite-code");
        TripDay tripDay = new TripDay(trip, 1, LocalDate.of(2026, 9, 1));
        ItineraryItem votingItem = new ItineraryItem(
                tripDay, "점심 메뉴", "식사", ItineraryItemDecisionType.VOTE, ItineraryItemStatus.VOTING, 1, null);
        VoteOption voteOption = new VoteOption(votingItem, "스시", "설명", "AI", null, null);
        when(voteOptionRepository.findById(1L)).thenReturn(Optional.of(voteOption));

        assertThatThrownBy(() -> voteOptionService.deleteVoteOption(1L, 1L))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.VOTE_ALREADY_STARTED));
        verify(voteOptionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("VOTE 항목에 선택지를 추가하면 AI 설명과 함께 저장된다")
    void VOTE_항목에_선택지를_추가하면_AI_설명과_함께_저장된다() {
        ItineraryItem pendingItem = pendingItineraryItem(1L);
        when(itineraryItemRepository.findById(10L)).thenReturn(Optional.of(pendingItem));
        when(voteOptionRepository.countByItineraryItem(pendingItem)).thenReturn(1L);
        when(descriptionGenerator.generate(anyString())).thenReturn("AI가 생성한 설명");
        when(voteOptionRepository.save(any(VoteOption.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile image = new MockMultipartFile(
                "image", "sushi.jpg", "image/jpeg", "image-bytes".getBytes(StandardCharsets.UTF_8));

        VoteOptionCreateResponseDto response = voteOptionService.createVoteOption(1L, 10L, "스시", image);

        assertThat(response.name()).isEqualTo("스시");
        assertThat(response.description()).isEqualTo("AI가 생성한 설명");
        assertThat(response.descriptionSource()).isEqualTo("AI");
        assertThat(response.hasImage()).isTrue();
        assertThat(pendingItem.getStatus()).isEqualTo(ItineraryItemStatus.PENDING);
    }

    @Test
    @DisplayName("HOST_PICK 항목에 선택지를 추가하면 즉시 확정된다")
    void HOST_PICK_항목에_선택지를_추가하면_즉시_확정된다() {
        User hostUser = new User("host", "hashed-password");
        ReflectionTestUtils.setField(hostUser, "id", 1L);
        Trip trip = new Trip(hostUser, "제주 여행",
                LocalDateTime.of(2026, 9, 1, 9, 0), LocalDateTime.of(2026, 9, 3, 18, 0), 3, "invite-code");
        TripDay tripDay = new TripDay(trip, 1, LocalDate.of(2026, 9, 1));
        ItineraryItem hostPickItem = new ItineraryItem(
                tripDay, "저녁 식사", "식사", ItineraryItemDecisionType.HOST_PICK, ItineraryItemStatus.PENDING, 1, null);
        when(itineraryItemRepository.findById(10L)).thenReturn(Optional.of(hostPickItem));
        when(voteOptionRepository.countByItineraryItem(hostPickItem)).thenReturn(0L);
        when(descriptionGenerator.generate(anyString())).thenReturn("AI가 생성한 설명");
        when(voteOptionRepository.save(any(VoteOption.class))).thenAnswer(invocation -> invocation.getArgument(0));

        voteOptionService.createVoteOption(1L, 10L, "스시", null);

        assertThat(hostPickItem.getStatus()).isEqualTo(ItineraryItemStatus.CONFIRMED);
        assertThat(hostPickItem.getConfirmedOption()).isNotNull();
        assertThat(hostPickItem.getConfirmedOption().getName()).isEqualTo("스시");
    }

    @Test
    @DisplayName("이름이 비어 있으면 선택지를 추가할 수 없다")
    void 이름이_비어_있으면_선택지를_추가할_수_없다() {
        assertThatThrownBy(() -> voteOptionService.createVoteOption(1L, 10L, "  ", null))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType().getCode()).isEqualTo("INVALID_INPUT_VALUE"));
        verify(itineraryItemRepository, never()).findById(any());
    }

    @Test
    @DisplayName("선택지가 이미 4개면 추가할 수 없다")
    void 선택지가_이미_4개면_추가할_수_없다() {
        ItineraryItem pendingItem = pendingItineraryItem(1L);
        when(itineraryItemRepository.findById(10L)).thenReturn(Optional.of(pendingItem));
        when(voteOptionRepository.countByItineraryItem(pendingItem)).thenReturn(4L);

        assertThatThrownBy(() -> voteOptionService.createVoteOption(1L, 10L, "스시", null))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.VOTE_OPTION_COUNT_EXCEEDED));
        verify(voteOptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("투표가 이미 시작된 항목에는 선택지를 추가할 수 없다")
    void 투표가_이미_시작된_항목에는_선택지를_추가할_수_없다() {
        User hostUser = new User("host", "hashed-password");
        ReflectionTestUtils.setField(hostUser, "id", 1L);
        Trip trip = new Trip(hostUser, "제주 여행",
                LocalDateTime.of(2026, 9, 1, 9, 0), LocalDateTime.of(2026, 9, 3, 18, 0), 3, "invite-code");
        TripDay tripDay = new TripDay(trip, 1, LocalDate.of(2026, 9, 1));
        ItineraryItem votingItem = new ItineraryItem(
                tripDay, "점심 메뉴", "식사", ItineraryItemDecisionType.VOTE, ItineraryItemStatus.VOTING, 1, null);
        when(itineraryItemRepository.findById(10L)).thenReturn(Optional.of(votingItem));

        assertThatThrownBy(() -> voteOptionService.createVoteOption(1L, 10L, "스시", null))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.VOTE_ALREADY_STARTED));
        verify(voteOptionRepository, never()).save(any());
    }
}
