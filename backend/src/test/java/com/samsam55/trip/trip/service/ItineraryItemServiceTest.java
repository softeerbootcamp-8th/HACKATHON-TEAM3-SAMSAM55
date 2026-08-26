package com.samsam55.trip.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.member.entity.User;
import com.samsam55.trip.trip.ai.VoteOptionDescriptionGenerator;
import com.samsam55.trip.trip.dto.ItineraryItemCreateRequestDto;
import com.samsam55.trip.trip.dto.ItineraryItemCreateResponseDto;
import com.samsam55.trip.trip.dto.ItineraryItemDetailResponseDto;
import com.samsam55.trip.trip.dto.ItineraryItemUpdateRequestDto;
import com.samsam55.trip.trip.dto.VoteOptionCreateItemDto;
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
import com.samsam55.trip.upload.service.S3PresignService;
import com.samsam55.trip.upload.service.UploadProperties;
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

    // toPublicUrl은 S3 호출 없이 key만으로 URL 문자열을 만드는 순수 로직이라, 목 대신 실제 인스턴스를 쓴다.
    private final S3PresignService s3PresignService =
            new S3PresignService(null, new UploadProperties("test-bucket", "ap-northeast-2"));

    private ItineraryItemService itineraryItemService;

    private User hostUser;
    private TripDay tripDay;

    @BeforeEach
    void setUp() {
        itineraryItemService = new ItineraryItemService(
                tripDayRepository, itineraryItemRepository, voteOptionRepository, descriptionGenerator,
                participantRepository, voteRepository, s3PresignService);

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
                new ItineraryItemCreateRequestDto("점심 메뉴", "식사", "VOTE", List.of(
                        new VoteOptionCreateItemDto("스시", null),
                        new VoteOptionCreateItemDto("라멘", null))));

        assertThat(response.name()).isEqualTo("점심 메뉴");
        assertThat(response.decisionType()).isEqualTo("VOTE");
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.sortOrder()).isEqualTo(4);
        assertThat(response.voteOptions()).hasSize(2);
        assertThat(response.voteOptions().get(0).name()).isEqualTo("스시");
        assertThat(response.voteOptions().get(0).description()).isEqualTo("AI가 생성한 설명");
        assertThat(response.voteOptions().get(0).descriptionSource()).isEqualTo("AI");
        assertThat(response.voteOptions().get(0).imageUrl()).isNull();
    }

    @Test
    @DisplayName("선택지에 이미지 key가 있으면 함께 저장하고 공개 URL을 응답에 담는다")
    void 선택지에_이미지_key가_있으면_함께_저장하고_공개_URL을_응답에_담는다() {
        when(tripDayRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(tripDay));
        when(itineraryItemRepository.findMaxSortOrderByTripDayId(10L)).thenReturn(0);
        when(itineraryItemRepository.save(any(ItineraryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(voteOptionRepository.save(any(VoteOption.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(descriptionGenerator.generate(anyString())).thenReturn("AI가 생성한 설명");

        ItineraryItemCreateResponseDto response = itineraryItemService.createItineraryItem(
                1L, 10L,
                new ItineraryItemCreateRequestDto("점심 메뉴", "식사", "VOTE", List.of(
                        new VoteOptionCreateItemDto("스시", "uploads/vote-options/a-sushi.jpg"),
                        new VoteOptionCreateItemDto("라멘", null))));

        assertThat(response.voteOptions().get(0).imageUrl()).contains("uploads/vote-options/a-sushi.jpg");
        assertThat(response.voteOptions().get(1).imageUrl()).isNull();

        ArgumentCaptor<VoteOption> captor = ArgumentCaptor.forClass(VoteOption.class);
        verify(voteOptionRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getImageKey()).isEqualTo("uploads/vote-options/a-sushi.jpg");
        assertThat(captor.getAllValues().get(1).getImageKey()).isNull();
    }

    @Test
    @DisplayName("HOST_PICK 방식이면 선택지를 무시하고 옵션 없이 생성한다")
    void HOST_PICK_방식이면_선택지를_무시하고_옵션_없이_생성한다() {
        when(tripDayRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(tripDay));
        when(itineraryItemRepository.findMaxSortOrderByTripDayId(10L)).thenReturn(0);
        when(itineraryItemRepository.save(any(ItineraryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ItineraryItemCreateResponseDto response = itineraryItemService.createItineraryItem(
                1L, 10L,
                new ItineraryItemCreateRequestDto("숙소", "숙박", "HOST_PICK",
                        List.of(new VoteOptionCreateItemDto("무시될 옵션", null))));

        assertThat(response.voteOptions()).isEmpty();
        verify(voteOptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("일차를 찾을 수 없으면 예외가 발생한다")
    void 일차를_찾을_수_없으면_예외가_발생한다() {
        when(tripDayRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itineraryItemService.createItineraryItem(
                1L, 10L, new ItineraryItemCreateRequestDto("점심 메뉴", "식사", "VOTE", List.of())))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.TRIP_DAY_NOT_FOUND));
    }

    @Test
    @DisplayName("요청자가 여행 방장이 아니면 예외가 발생한다")
    void 요청자가_여행_방장이_아니면_예외가_발생한다() {
        when(tripDayRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(tripDay));

        assertThatThrownBy(() -> itineraryItemService.createItineraryItem(
                999L, 10L, new ItineraryItemCreateRequestDto("점심 메뉴", "식사", "VOTE", List.of())))
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
                        List.of(
                                new VoteOptionCreateItemDto("1", null),
                                new VoteOptionCreateItemDto("2", null),
                                new VoteOptionCreateItemDto("3", null),
                                new VoteOptionCreateItemDto("4", null),
                                new VoteOptionCreateItemDto("5", null)))))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.VOTE_OPTION_COUNT_EXCEEDED));
    }

    @Test
    @DisplayName("PENDING 상태 일정 항목의 이름·카테고리·결정 방식을 수정한다")
    void PENDING_상태_일정_항목의_이름_카테고리_결정_방식을_수정한다() {
        ItineraryItem itineraryItem = new ItineraryItem(
                tripDay, "점심 메뉴", "식사", ItineraryItemDecisionType.VOTE, ItineraryItemStatus.PENDING, 1, null);
        ReflectionTestUtils.setField(itineraryItem, "id", 100L);
        when(itineraryItemRepository.findById(100L)).thenReturn(Optional.of(itineraryItem));
        when(voteOptionRepository.findByItineraryItem(itineraryItem)).thenReturn(List.of());

        ItineraryItemDetailResponseDto response = itineraryItemService.updateItineraryItem(
                1L, 100L, new ItineraryItemUpdateRequestDto("저녁 메뉴", "관광", "HOST_PICK", null));

        assertThat(response.name()).isEqualTo("저녁 메뉴");
        assertThat(response.category()).isEqualTo("관광");
        assertThat(response.decisionType()).isEqualTo("HOST_PICK");
        assertThat(itineraryItem.getName()).isEqualTo("저녁 메뉴");
        assertThat(itineraryItem.getDecisionType()).isEqualTo(ItineraryItemDecisionType.HOST_PICK);
    }

    @Test
    @DisplayName("수정할 일정 항목을 찾을 수 없으면 예외가 발생한다")
    void 수정할_일정_항목을_찾을_수_없으면_예외가_발생한다() {
        when(itineraryItemRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itineraryItemService.updateItineraryItem(
                1L, 100L, new ItineraryItemUpdateRequestDto("저녁 메뉴", "관광", "HOST_PICK", null)))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.ITINERARY_ITEM_NOT_FOUND));
    }

    @Test
    @DisplayName("수정 요청자가 여행 방장이 아니면 예외가 발생한다")
    void 수정_요청자가_여행_방장이_아니면_예외가_발생한다() {
        ItineraryItem itineraryItem = new ItineraryItem(
                tripDay, "점심 메뉴", "식사", ItineraryItemDecisionType.VOTE, ItineraryItemStatus.PENDING, 1, null);
        ReflectionTestUtils.setField(itineraryItem, "id", 100L);
        when(itineraryItemRepository.findById(100L)).thenReturn(Optional.of(itineraryItem));

        assertThatThrownBy(() -> itineraryItemService.updateItineraryItem(
                999L, 100L, new ItineraryItemUpdateRequestDto("저녁 메뉴", "관광", "HOST_PICK", null)))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.NOT_TRIP_HOST));
    }

    @Test
    @DisplayName("투표가 이미 시작된 일정 항목은 수정할 수 없다")
    void 투표가_이미_시작된_일정_항목은_수정할_수_없다() {
        ItineraryItem itineraryItem = new ItineraryItem(
                tripDay, "점심 메뉴", "식사", ItineraryItemDecisionType.VOTE, ItineraryItemStatus.VOTING, 1, null);
        ReflectionTestUtils.setField(itineraryItem, "id", 100L);
        when(itineraryItemRepository.findById(100L)).thenReturn(Optional.of(itineraryItem));

        assertThatThrownBy(() -> itineraryItemService.updateItineraryItem(
                1L, 100L, new ItineraryItemUpdateRequestDto("저녁 메뉴", "관광", "HOST_PICK", null)))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.VOTE_ALREADY_STARTED));
    }

    @Test
    @DisplayName("VOTE에서 HOST_PICK으로 바꾸는데 기존 선택지가 2개 이상이고 selectedOptionId가 없으면 예외가 발생한다")
    void VOTE에서_HOST_PICK으로_바꾸는데_기존_선택지가_2개_이상이고_selectedOptionId가_없으면_예외가_발생한다() {
        ItineraryItem itineraryItem = new ItineraryItem(
                tripDay, "점심 메뉴", "식사", ItineraryItemDecisionType.VOTE, ItineraryItemStatus.PENDING, 1, null);
        ReflectionTestUtils.setField(itineraryItem, "id", 100L);
        VoteOption sushi = new VoteOption(itineraryItem, "스시", "설명", "AI", null);
        ReflectionTestUtils.setField(sushi, "id", 1L);
        VoteOption ramen = new VoteOption(itineraryItem, "라멘", "설명", "AI", null);
        ReflectionTestUtils.setField(ramen, "id", 2L);
        when(itineraryItemRepository.findById(100L)).thenReturn(Optional.of(itineraryItem));
        when(voteOptionRepository.findByItineraryItem(itineraryItem)).thenReturn(List.of(sushi, ramen));

        assertThatThrownBy(() -> itineraryItemService.updateItineraryItem(
                1L, 100L, new ItineraryItemUpdateRequestDto("저녁 메뉴", "관광", "HOST_PICK", null)))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.VOTE_OPTION_SELECTION_REQUIRED));
    }

    @Test
    @DisplayName("VOTE에서 HOST_PICK으로 바꾸는데 selectedOptionId가 이 일정의 선택지가 아니면 예외가 발생한다")
    void VOTE에서_HOST_PICK으로_바꾸는데_selectedOptionId가_이_일정의_선택지가_아니면_예외가_발생한다() {
        ItineraryItem itineraryItem = new ItineraryItem(
                tripDay, "점심 메뉴", "식사", ItineraryItemDecisionType.VOTE, ItineraryItemStatus.PENDING, 1, null);
        ReflectionTestUtils.setField(itineraryItem, "id", 100L);
        VoteOption sushi = new VoteOption(itineraryItem, "스시", "설명", "AI", null);
        ReflectionTestUtils.setField(sushi, "id", 1L);
        VoteOption ramen = new VoteOption(itineraryItem, "라멘", "설명", "AI", null);
        ReflectionTestUtils.setField(ramen, "id", 2L);
        when(itineraryItemRepository.findById(100L)).thenReturn(Optional.of(itineraryItem));
        when(voteOptionRepository.findByItineraryItem(itineraryItem)).thenReturn(List.of(sushi, ramen));

        assertThatThrownBy(() -> itineraryItemService.updateItineraryItem(
                1L, 100L, new ItineraryItemUpdateRequestDto("저녁 메뉴", "관광", "HOST_PICK", 999L)))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.VOTE_OPTION_NOT_FOUND));
    }

    @Test
    @DisplayName("VOTE에서 HOST_PICK으로 바꾸는데 기존 선택지가 2개 이상이면 선택한 선택지만 남기고 나머지는 삭제한다")
    void VOTE에서_HOST_PICK으로_바꾸는데_기존_선택지가_2개_이상이면_선택한_선택지만_남기고_나머지는_삭제한다() {
        ItineraryItem itineraryItem = new ItineraryItem(
                tripDay, "점심 메뉴", "식사", ItineraryItemDecisionType.VOTE, ItineraryItemStatus.PENDING, 1, null);
        ReflectionTestUtils.setField(itineraryItem, "id", 100L);
        VoteOption sushi = new VoteOption(itineraryItem, "스시", "설명", "AI", null);
        ReflectionTestUtils.setField(sushi, "id", 1L);
        VoteOption ramen = new VoteOption(itineraryItem, "라멘", "설명", "AI", null);
        ReflectionTestUtils.setField(ramen, "id", 2L);
        when(itineraryItemRepository.findById(100L)).thenReturn(Optional.of(itineraryItem));
        when(voteOptionRepository.findByItineraryItem(itineraryItem))
                .thenReturn(List.of(sushi, ramen))
                .thenReturn(List.of(sushi));

        ItineraryItemDetailResponseDto response = itineraryItemService.updateItineraryItem(
                1L, 100L, new ItineraryItemUpdateRequestDto("저녁 메뉴", "관광", "HOST_PICK", 1L));

        assertThat(response.decisionType()).isEqualTo("HOST_PICK");
        assertThat(response.voteOptions()).hasSize(1);
        assertThat(response.voteOptions().get(0).name()).isEqualTo("스시");
        assertThat(itineraryItem.getStatus()).isEqualTo(ItineraryItemStatus.PENDING);
        verify(voteOptionRepository).deleteAll(List.of(ramen));
    }

    @Test
    @DisplayName("VOTE에서 HOST_PICK으로 바꿔도 기존 선택지가 1개 이하면 selectedOptionId 없이도 그대로 수정된다")
    void VOTE에서_HOST_PICK으로_바꿔도_기존_선택지가_1개_이하면_selectedOptionId_없이도_그대로_수정된다() {
        ItineraryItem itineraryItem = new ItineraryItem(
                tripDay, "점심 메뉴", "식사", ItineraryItemDecisionType.VOTE, ItineraryItemStatus.PENDING, 1, null);
        ReflectionTestUtils.setField(itineraryItem, "id", 100L);
        VoteOption sushi = new VoteOption(itineraryItem, "스시", "설명", "AI", null);
        ReflectionTestUtils.setField(sushi, "id", 1L);
        when(itineraryItemRepository.findById(100L)).thenReturn(Optional.of(itineraryItem));
        when(voteOptionRepository.findByItineraryItem(itineraryItem)).thenReturn(List.of(sushi));

        ItineraryItemDetailResponseDto response = itineraryItemService.updateItineraryItem(
                1L, 100L, new ItineraryItemUpdateRequestDto("저녁 메뉴", "관광", "HOST_PICK", null));

        assertThat(response.decisionType()).isEqualTo("HOST_PICK");
        assertThat(response.voteOptions()).hasSize(1);
        verify(voteOptionRepository, never()).deleteAll(any());
    }

    @Test
    @DisplayName("일정 항목 상세를 조회하면 선택지 목록도 함께 반환한다")
    void 일정_항목_상세를_조회하면_선택지_목록도_함께_반환한다() {
        ItineraryItem itineraryItem = new ItineraryItem(
                tripDay, "점심 메뉴", "식사", ItineraryItemDecisionType.VOTE, ItineraryItemStatus.PENDING, 1, null);
        ReflectionTestUtils.setField(itineraryItem, "id", 100L);
        VoteOption voteOption = new VoteOption(itineraryItem, "스시", "설명", "AI", null);
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
        VoteOption sushi = new VoteOption(itineraryItem, "스시", "설명", "AI", null);
        ReflectionTestUtils.setField(sushi, "id", 1L);
        VoteOption ramen = new VoteOption(itineraryItem, "라멘", "설명", "AI", null);
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
