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
import com.samsam55.trip.trip.dto.VoteOptionSummaryDto;
import com.samsam55.trip.trip.entity.ItineraryItem;
import com.samsam55.trip.trip.entity.ItineraryItemDecisionType;
import com.samsam55.trip.trip.entity.ItineraryItemStatus;
import com.samsam55.trip.trip.entity.Trip;
import com.samsam55.trip.trip.entity.TripDay;
import com.samsam55.trip.trip.entity.VoteOption;
import com.samsam55.trip.trip.exception.TripErrorType;
import com.samsam55.trip.trip.repository.ItineraryItemRepository;
import com.samsam55.trip.trip.repository.VoteOptionRepository;
import com.samsam55.trip.upload.service.S3PresignService;
import com.samsam55.trip.upload.service.UploadProperties;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VoteOptionServiceTest {

    @Mock
    private VoteOptionRepository voteOptionRepository;

    @Mock
    private ItineraryItemRepository itineraryItemRepository;

    @Mock
    private VoteOptionDescriptionGenerator descriptionGenerator;

    // toPublicUrl은 S3 호출 없이 key만으로 URL 문자열을 만드는 순수 로직이라, 목 대신 실제 인스턴스를 쓴다.
    private final S3PresignService s3PresignService =
            new S3PresignService(null, new UploadProperties("test-bucket", "ap-northeast-2"));

    private VoteOptionService voteOptionService;

    @BeforeEach
    void setUp() {
        voteOptionService = new VoteOptionService(
                voteOptionRepository, itineraryItemRepository, descriptionGenerator, s3PresignService);
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
        VoteOption voteOption = new VoteOption(pendingItem, "스시", "설명", "AI", null);
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
        VoteOption voteOption = new VoteOption(pendingItem, "스시", "설명", "AI", null);
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
        VoteOption voteOption = new VoteOption(votingItem, "스시", "설명", "AI", null);
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
        when(descriptionGenerator.getSource()).thenReturn("AI");
        when(voteOptionRepository.save(any(VoteOption.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VoteOptionCreateResponseDto response = voteOptionService.createVoteOption(
                1L, 10L, "스시", "uploads/vote-options/a-sushi.jpg");

        assertThat(response.name()).isEqualTo("스시");
        assertThat(response.description()).isEqualTo("AI가 생성한 설명");
        assertThat(response.descriptionSource()).isEqualTo("AI");
        assertThat(response.imageUrl()).contains("uploads/vote-options/a-sushi.jpg");
        assertThat(pendingItem.getStatus()).isEqualTo(ItineraryItemStatus.PENDING);
    }

    @Test
    @DisplayName("HOST_PICK 항목에 선택지를 추가해도 자동으로 확정되지 않는다")
    void HOST_PICK_항목에_선택지를_추가해도_자동으로_확정되지_않는다() {
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

        assertThat(hostPickItem.getStatus()).isEqualTo(ItineraryItemStatus.PENDING);
        assertThat(hostPickItem.getConfirmedOption()).isNull();
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

    @Test
    @DisplayName("선택지를 수정하면 이름·설명이 바뀌고 descriptionSource가 HOST로 바뀐다")
    void 선택지를_수정하면_이름_설명이_바뀌고_descriptionSource가_HOST로_바뀐다() {
        ItineraryItem pendingItem = pendingItineraryItem(1L);
        VoteOption voteOption = new VoteOption(pendingItem, "스시", "AI 설명", "AI", null);
        when(voteOptionRepository.findById(1L)).thenReturn(Optional.of(voteOption));

        VoteOptionSummaryDto response = voteOptionService.updateVoteOption(1L, 1L, "라멘", "직접 쓴 설명", null);

        assertThat(response.name()).isEqualTo("라멘");
        assertThat(response.description()).isEqualTo("직접 쓴 설명");
        assertThat(response.descriptionSource()).isEqualTo("HOST");
        assertThat(response.imageUrl()).isNull();
    }

    @Test
    @DisplayName("새 이미지를 안 보내면 기존 이미지를 유지한다")
    void 새_이미지를_안_보내면_기존_이미지를_유지한다() {
        ItineraryItem pendingItem = pendingItineraryItem(1L);
        VoteOption voteOption = new VoteOption(
                pendingItem, "스시", "설명", "AI", "uploads/vote-options/a-existing.jpg");
        when(voteOptionRepository.findById(1L)).thenReturn(Optional.of(voteOption));

        VoteOptionSummaryDto response = voteOptionService.updateVoteOption(1L, 1L, "라멘", "설명", null);

        assertThat(response.imageUrl()).contains("uploads/vote-options/a-existing.jpg");
        assertThat(voteOption.getImageKey()).isEqualTo("uploads/vote-options/a-existing.jpg");
    }

    @Test
    @DisplayName("새 이미지를 보내면 기존 이미지를 교체한다")
    void 새_이미지를_보내면_기존_이미지를_교체한다() {
        ItineraryItem pendingItem = pendingItineraryItem(1L);
        VoteOption voteOption = new VoteOption(
                pendingItem, "스시", "설명", "AI", "uploads/vote-options/a-old.png");
        when(voteOptionRepository.findById(1L)).thenReturn(Optional.of(voteOption));

        voteOptionService.updateVoteOption(1L, 1L, "라멘", "설명", "uploads/vote-options/b-new.jpg");

        assertThat(voteOption.getImageKey()).isEqualTo("uploads/vote-options/b-new.jpg");
    }

    @Test
    @DisplayName("선택지 수정 시 이름이 비어 있으면 예외가 발생한다")
    void 선택지_수정_시_이름이_비어_있으면_예외가_발생한다() {
        assertThatThrownBy(() -> voteOptionService.updateVoteOption(1L, 1L, "  ", "설명", null))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType().getCode()).isEqualTo("INVALID_INPUT_VALUE"));
        verify(voteOptionRepository, never()).findById(any());
    }

    @Test
    @DisplayName("선택지 수정 시 선택지를 찾을 수 없으면 예외가 발생한다")
    void 선택지_수정_시_선택지를_찾을_수_없으면_예외가_발생한다() {
        when(voteOptionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> voteOptionService.updateVoteOption(1L, 1L, "라멘", "설명", null))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.VOTE_OPTION_NOT_FOUND));
    }

    @Test
    @DisplayName("선택지 수정 시 방장이 아니면 예외가 발생한다")
    void 선택지_수정_시_방장이_아니면_예외가_발생한다() {
        ItineraryItem pendingItem = pendingItineraryItem(1L);
        VoteOption voteOption = new VoteOption(pendingItem, "스시", "설명", "AI", null);
        when(voteOptionRepository.findById(1L)).thenReturn(Optional.of(voteOption));

        assertThatThrownBy(() -> voteOptionService.updateVoteOption(999L, 1L, "라멘", "설명", null))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.NOT_TRIP_HOST));
    }

    @Test
    @DisplayName("선택지 수정 시 투표가 이미 시작된 일정 항목이면 예외가 발생한다")
    void 선택지_수정_시_투표가_이미_시작된_일정_항목이면_예외가_발생한다() {
        User hostUser = new User("host", "hashed-password");
        ReflectionTestUtils.setField(hostUser, "id", 1L);
        Trip trip = new Trip(hostUser, "제주 여행",
                LocalDateTime.of(2026, 9, 1, 9, 0), LocalDateTime.of(2026, 9, 3, 18, 0), 3, "invite-code");
        TripDay tripDay = new TripDay(trip, 1, LocalDate.of(2026, 9, 1));
        ItineraryItem votingItem = new ItineraryItem(
                tripDay, "점심 메뉴", "식사", ItineraryItemDecisionType.VOTE, ItineraryItemStatus.VOTING, 1, null);
        VoteOption voteOption = new VoteOption(votingItem, "스시", "설명", "AI", null);
        when(voteOptionRepository.findById(1L)).thenReturn(Optional.of(voteOption));

        assertThatThrownBy(() -> voteOptionService.updateVoteOption(1L, 1L, "라멘", "설명", null))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.VOTE_ALREADY_STARTED));
    }

    @Test
    @DisplayName("내가 결정 방식은 확정된 뒤에도 선택지를 수정할 수 있다")
    void 내가_결정_방식은_확정된_뒤에도_선택지를_수정할_수_있다() {
        User hostUser = new User("host", "hashed-password");
        ReflectionTestUtils.setField(hostUser, "id", 1L);
        Trip trip = new Trip(hostUser, "제주 여행",
                LocalDateTime.of(2026, 9, 1, 9, 0), LocalDateTime.of(2026, 9, 3, 18, 0), 3, "invite-code");
        TripDay tripDay = new TripDay(trip, 1, LocalDate.of(2026, 9, 1));
        ItineraryItem confirmedHostPickItem = new ItineraryItem(
                tripDay, "저녁 식사", "식사", ItineraryItemDecisionType.HOST_PICK, ItineraryItemStatus.CONFIRMED, 1, null);
        VoteOption voteOption = new VoteOption(confirmedHostPickItem, "스시집", "설명", "HOST", null);
        when(voteOptionRepository.findById(1L)).thenReturn(Optional.of(voteOption));

        VoteOptionSummaryDto response =
                voteOptionService.updateVoteOption(1L, 1L, "초밥집", "수정한 설명", null);

        assertThat(response.name()).isEqualTo("초밥집");
        assertThat(response.description()).isEqualTo("수정한 설명");
    }

    @Test
    @DisplayName("투표 방식은 확정된 뒤에는 선택지를 수정할 수 없다")
    void 투표_방식은_확정된_뒤에는_선택지를_수정할_수_없다() {
        User hostUser = new User("host", "hashed-password");
        ReflectionTestUtils.setField(hostUser, "id", 1L);
        Trip trip = new Trip(hostUser, "제주 여행",
                LocalDateTime.of(2026, 9, 1, 9, 0), LocalDateTime.of(2026, 9, 3, 18, 0), 3, "invite-code");
        TripDay tripDay = new TripDay(trip, 1, LocalDate.of(2026, 9, 1));
        ItineraryItem confirmedVoteItem = new ItineraryItem(
                tripDay, "점심 메뉴", "식사", ItineraryItemDecisionType.VOTE, ItineraryItemStatus.CONFIRMED, 1, null);
        VoteOption voteOption = new VoteOption(confirmedVoteItem, "스시", "설명", "AI", null);
        when(voteOptionRepository.findById(1L)).thenReturn(Optional.of(voteOption));

        assertThatThrownBy(() -> voteOptionService.updateVoteOption(1L, 1L, "라멘", "설명", null))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.VOTE_ALREADY_STARTED));
    }
}
