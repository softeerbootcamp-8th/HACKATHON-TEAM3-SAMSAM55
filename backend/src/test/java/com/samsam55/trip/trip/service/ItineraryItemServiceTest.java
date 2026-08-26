package com.samsam55.trip.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.samsam55.trip.member.entity.User;
import com.samsam55.trip.trip.ai.VoteOptionDescriptionGenerator;
import com.samsam55.trip.trip.dto.ItineraryItemCreateRequestDto;
import com.samsam55.trip.trip.dto.ItineraryItemCreateResponseDto;
import com.samsam55.trip.trip.dto.ItineraryItemDetailResponseDto;
import com.samsam55.trip.trip.dto.VoteStatusResponseDto;
import com.samsam55.trip.trip.entity.ItineraryItem;
import com.samsam55.trip.trip.entity.ItineraryItemDecisionType;
import com.samsam55.trip.trip.entity.ItineraryItemStatus;
import com.samsam55.trip.trip.entity.Participant;
import com.samsam55.trip.trip.entity.Trip;
import com.samsam55.trip.trip.entity.TripDay;
import com.samsam55.trip.trip.entity.Vote;
import com.samsam55.trip.trip.entity.VoteOption;
import com.samsam55.trip.trip.exception.TripErrorType;
import com.samsam55.trip.trip.repository.ItineraryItemRepository;
import com.samsam55.trip.trip.repository.ParticipantRepository;
import com.samsam55.trip.trip.repository.TripDayRepository;
import com.samsam55.trip.trip.repository.VoteOptionRepository;
import com.samsam55.trip.trip.repository.VoteRepository;
import com.samsam55.trip.global.exception.ApplicationException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ItineraryItemServiceTest {

    @Mock
    private TripDayRepository tripDayRepository;

    @Mock
    private ItineraryItemRepository itineraryItemRepository;

    @Mock
    private VoteOptionRepository voteOptionRepository;

    @Mock
    private VoteOptionDescriptionGenerator descriptionGenerator;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private VoteRepository voteRepository;

    private ItineraryItemService itineraryItemService;

    private User hostUser;
    private TripDay tripDay;

    @BeforeEach
    void setUp() {
        itineraryItemService = new ItineraryItemService(
                tripDayRepository, itineraryItemRepository, voteOptionRepository, descriptionGenerator,
                participantRepository, voteRepository);

        hostUser = new User("host", "hashed-password");
        ReflectionTestUtils.setField(hostUser, "id", 1L);

        Trip trip = new Trip(hostUser, "제주 여행",
                LocalDateTime.of(2026, 9, 1, 9, 0), LocalDateTime.of(2026, 9, 3, 18, 0), 3, "invite-code");
        tripDay = new TripDay(trip, 1, LocalDate.of(2026, 9, 1));
    }

    @Test
    @DisplayName("VOTE 방식이면 선택지별로 AI 설명을 생성해 함께 저장한다")
    void VOTE_방식이면_선택지별로_AI_설명을_생성해_함께_저장한다() {
        when(tripDayRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(tripDay));
        when(itineraryItemRepository.findMaxSortOrderByTripDayId(10L)).thenReturn(3);
        when(itineraryItemRepository.save(any(ItineraryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(voteOptionRepository.save(any(VoteOption.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(descriptionGenerator.generate(anyString())).thenReturn("AI가 생성한 설명");
        when(descriptionGenerator.getSource()).thenReturn("AI");

        ItineraryItemCreateResponseDto response = itineraryItemService.createItineraryItem(
                1L, 10L,
                new ItineraryItemCreateRequestDto("점심 메뉴", "식사", "VOTE", List.of("스시", "라멘")),
                null);

        assertThat(response.name()).isEqualTo("점심 메뉴");
        assertThat(response.decisionType()).isEqualTo("VOTE");
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.sortOrder()).isEqualTo(4);
        assertThat(response.voteOptions()).hasSize(2);
        assertThat(response.voteOptions().get(0).name()).isEqualTo("스시");
        assertThat(response.voteOptions().get(0).description()).isEqualTo("AI가 생성한 설명");
        assertThat(response.voteOptions().get(0).descriptionSource()).isEqualTo("AI");
        assertThat(response.voteOptions().get(0).hasImage()).isFalse();
    }

    @Test
    @DisplayName("선택지에 이미지가 있으면 바이트와 콘텐츠 타입을 함께 저장한다")
    void 선택지에_이미지가_있으면_바이트와_콘텐츠_타입을_함께_저장한다() {
        when(tripDayRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(tripDay));
        when(itineraryItemRepository.findMaxSortOrderByTripDayId(10L)).thenReturn(0);
        when(itineraryItemRepository.save(any(ItineraryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(voteOptionRepository.save(any(VoteOption.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(descriptionGenerator.generate(anyString())).thenReturn("AI가 생성한 설명");

        MockMultipartFile image = new MockMultipartFile(
                "optionImages", "sushi.jpg", "image/jpeg", "image-bytes".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile noImage = new MockMultipartFile("optionImages", new byte[0]);

        ItineraryItemCreateResponseDto response = itineraryItemService.createItineraryItem(
                1L, 10L,
                new ItineraryItemCreateRequestDto("점심 메뉴", "식사", "VOTE", List.of("스시", "라멘")),
                List.of(image, noImage));

        assertThat(response.voteOptions().get(0).hasImage()).isTrue();
        assertThat(response.voteOptions().get(1).hasImage()).isFalse();

        ArgumentCaptor<VoteOption> captor = ArgumentCaptor.forClass(VoteOption.class);
        verify(voteOptionRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getImage()).isEqualTo("image-bytes".getBytes(StandardCharsets.UTF_8));
        assertThat(captor.getAllValues().get(0).getImageContentType()).isEqualTo("image/jpeg");
        assertThat(captor.getAllValues().get(1).getImage()).isNull();
    }

    @Test
    @DisplayName("HOST_PICK 방식이면 선택지를 무시하고 옵션 없이 생성한다")
    void HOST_PICK_방식이면_선택지를_무시하고_옵션_없이_생성한다() {
        when(tripDayRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(tripDay));
        when(itineraryItemRepository.findMaxSortOrderByTripDayId(10L)).thenReturn(0);
        when(itineraryItemRepository.save(any(ItineraryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ItineraryItemCreateResponseDto response = itineraryItemService.createItineraryItem(
                1L, 10L,
                new ItineraryItemCreateRequestDto("숙소", "숙박", "HOST_PICK", List.of("무시될 옵션")),
                null);

        assertThat(response.voteOptions()).isEmpty();
        verify(voteOptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("일차를 찾을 수 없으면 예외가 발생한다")
    void 일차를_찾을_수_없으면_예외가_발생한다() {
        when(tripDayRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itineraryItemService.createItineraryItem(
                1L, 10L, new ItineraryItemCreateRequestDto("점심 메뉴", "식사", "VOTE", List.of()), null))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.TRIP_DAY_NOT_FOUND));
    }

    @Test
    @DisplayName("요청자가 여행 방장이 아니면 예외가 발생한다")
    void 요청자가_여행_방장이_아니면_예외가_발생한다() {
        when(tripDayRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(tripDay));

        assertThatThrownBy(() -> itineraryItemService.createItineraryItem(
                999L, 10L, new ItineraryItemCreateRequestDto("점심 메뉴", "식사", "VOTE", List.of()), null))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.NOT_TRIP_HOST));
    }

    @Test
    @DisplayName("VOTE 선택지가 4개를 초과하면 예외가 발생한다")
    void VOTE_선택지가_4개를_초과하면_예외가_발생한다() {
        when(tripDayRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(tripDay));

        assertThatThrownBy(() -> itineraryItemService.createItineraryItem(
                1L, 10L,
                new ItineraryItemCreateRequestDto("점심 메뉴", "식사", "VOTE",
                        List.of("1", "2", "3", "4", "5")),
                null))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.VOTE_OPTION_COUNT_EXCEEDED));
    }

    @Test
    @DisplayName("일정 항목 상세를 조회하면 선택지 목록도 함께 반환한다")
    void 일정_항목_상세를_조회하면_선택지_목록도_함께_반환한다() {
        ItineraryItem itineraryItem = new ItineraryItem(
                tripDay, "점심 메뉴", "식사", ItineraryItemDecisionType.VOTE, ItineraryItemStatus.PENDING, 1, null);
        ReflectionTestUtils.setField(itineraryItem, "id", 100L);
        VoteOption voteOption = new VoteOption(itineraryItem, "스시", "설명", "AI", null, null);
        when(itineraryItemRepository.findById(100L)).thenReturn(Optional.of(itineraryItem));
        when(voteOptionRepository.findByItineraryItem(itineraryItem)).thenReturn(List.of(voteOption));

        ItineraryItemDetailResponseDto response = itineraryItemService.getItineraryItem(1L, 100L);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.name()).isEqualTo("점심 메뉴");
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.voteOptions()).hasSize(1);
        assertThat(response.voteOptions().get(0).name()).isEqualTo("스시");
    }

    @Test
    @DisplayName("일정 항목 상세 조회 시 항목을 찾을 수 없으면 예외가 발생한다")
    void 일정_항목_상세_조회_시_항목을_찾을_수_없으면_예외가_발생한다() {
        when(itineraryItemRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itineraryItemService.getItineraryItem(1L, 100L))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.ITINERARY_ITEM_NOT_FOUND));
    }

    @Test
    @DisplayName("투표 현황을 조회하면 참여자별 투표 여부와 선택지별 득표수를 반환한다")
    void 투표_현황을_조회하면_참여자별_투표_여부와_선택지별_득표수를_반환한다() {
        ItineraryItem itineraryItem = new ItineraryItem(
                tripDay, "점심 메뉴", "식사", ItineraryItemDecisionType.VOTE, ItineraryItemStatus.VOTING, 1, null);
        ReflectionTestUtils.setField(itineraryItem, "id", 100L);
        VoteOption sushi = new VoteOption(itineraryItem, "스시", "설명", "AI", null, null);
        ReflectionTestUtils.setField(sushi, "id", 1L);
        VoteOption ramen = new VoteOption(itineraryItem, "라멘", "설명", "AI", null, null);
        ReflectionTestUtils.setField(ramen, "id", 2L);

        Participant mom = new Participant(tripDay.getTrip(), "엄마", LocalDateTime.now());
        ReflectionTestUtils.setField(mom, "id", 10L);
        Participant dad = new Participant(tripDay.getTrip(), "아빠", LocalDateTime.now());
        ReflectionTestUtils.setField(dad, "id", 11L);
        Vote vote = new Vote(sushi, itineraryItem, mom);

        when(itineraryItemRepository.findById(100L)).thenReturn(Optional.of(itineraryItem));
        when(participantRepository.findAllByTripOrderById(tripDay.getTrip())).thenReturn(List.of(mom, dad));
        when(voteRepository.findAllByItineraryItemIdWithOptionAndParticipant(100L)).thenReturn(List.of(vote));
        when(voteOptionRepository.findByItineraryItem(itineraryItem)).thenReturn(List.of(sushi, ramen));

        VoteStatusResponseDto response = itineraryItemService.getVoteStatus(1L, 100L);

        assertThat(response.votedCount()).isEqualTo(1);
        assertThat(response.totalParticipants()).isEqualTo(2);
        assertThat(response.participants()).hasSize(2);
        assertThat(response.participants().get(0).voted()).isTrue();
        assertThat(response.participants().get(1).voted()).isFalse();
        assertThat(response.options()).hasSize(2);
        assertThat(response.options().get(0).voteCount()).isEqualTo(1);
        assertThat(response.options().get(0).voters()).extracting("roleName").containsExactly("엄마");
        assertThat(response.options().get(1).voteCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("투표 현황 조회 시 방장이 아니면 예외가 발생한다")
    void 투표_현황_조회_시_방장이_아니면_예외가_발생한다() {
        ItineraryItem itineraryItem = new ItineraryItem(
                tripDay, "점심 메뉴", "식사", ItineraryItemDecisionType.VOTE, ItineraryItemStatus.VOTING, 1, null);
        ReflectionTestUtils.setField(itineraryItem, "id", 100L);
        when(itineraryItemRepository.findById(100L)).thenReturn(Optional.of(itineraryItem));

        assertThatThrownBy(() -> itineraryItemService.getVoteStatus(999L, 100L))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.NOT_TRIP_HOST));
    }

    @Test
    @DisplayName("투표 현황 조회 시 항목을 찾을 수 없으면 예외가 발생한다")
    void 투표_현황_조회_시_항목을_찾을_수_없으면_예외가_발생한다() {
        when(itineraryItemRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itineraryItemService.getVoteStatus(1L, 100L))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.ITINERARY_ITEM_NOT_FOUND));
    }

    @Test
    @DisplayName("일정 항목을 삭제하면 투표 기록과 선택지도 함께 삭제된다")
    void 일정_항목을_삭제하면_투표_기록과_선택지도_함께_삭제된다() {
        ItineraryItem itineraryItem = new ItineraryItem(
                tripDay, "점심 메뉴", "식사", ItineraryItemDecisionType.VOTE, ItineraryItemStatus.CONFIRMED, 1, null);
        ReflectionTestUtils.setField(itineraryItem, "id", 100L);
        when(itineraryItemRepository.findByIdWithTripAndConfirmedOption(100L))
                .thenReturn(Optional.of(itineraryItem));

        itineraryItemService.deleteItineraryItem(1L, 100L);

        verify(voteRepository).deleteAllByItineraryItemId(100L);
        verify(itineraryItemRepository).clearConfirmedOptionByItemId(100L);
        verify(voteOptionRepository).deleteAllByItineraryItemId(100L);
        verify(itineraryItemRepository).delete(itineraryItem);
    }

    @Test
    @DisplayName("일정 항목 삭제 시 항목을 찾을 수 없으면 예외가 발생한다")
    void 일정_항목_삭제_시_항목을_찾을_수_없으면_예외가_발생한다() {
        when(itineraryItemRepository.findByIdWithTripAndConfirmedOption(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itineraryItemService.deleteItineraryItem(1L, 100L))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.ITINERARY_ITEM_NOT_FOUND));
        verify(itineraryItemRepository, never()).delete(any());
    }

    @Test
    @DisplayName("일정 항목 삭제 시 방장이 아니면 예외가 발생한다")
    void 일정_항목_삭제_시_방장이_아니면_예외가_발생한다() {
        ItineraryItem itineraryItem = new ItineraryItem(
                tripDay, "점심 메뉴", "식사", ItineraryItemDecisionType.VOTE, ItineraryItemStatus.PENDING, 1, null);
        ReflectionTestUtils.setField(itineraryItem, "id", 100L);
        when(itineraryItemRepository.findByIdWithTripAndConfirmedOption(100L))
                .thenReturn(Optional.of(itineraryItem));

        assertThatThrownBy(() -> itineraryItemService.deleteItineraryItem(999L, 100L))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.NOT_TRIP_HOST));
        verify(itineraryItemRepository, never()).delete(any());
    }

}
