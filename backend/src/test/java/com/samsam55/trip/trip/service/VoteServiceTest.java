package com.samsam55.trip.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.samsam55.trip.auth.dto.ParticipantPrincipal;
import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.member.entity.User;
import com.samsam55.trip.trip.dto.ItineraryItemConfirmationResponseDto;
import com.samsam55.trip.trip.dto.ItineraryItemStatusDto;
import com.samsam55.trip.trip.dto.MyVoteBatchResponseDto;
import com.samsam55.trip.trip.dto.MyVoteItemRequestDto;
import com.samsam55.trip.trip.dto.VoteStartResponseDto;
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
import com.samsam55.trip.trip.repository.VoteOptionRepository;
import com.samsam55.trip.trip.repository.VoteRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest {

    private static final Long TRIP_ID = 5L;
    private static final Long HOST_USER_ID = 1L;

    @Mock
    private ItineraryItemRepository itineraryItemRepository;

    @Mock
    private VoteOptionRepository voteOptionRepository;

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private ParticipantRepository participantRepository;

    private VoteService voteService;

    private TripDay tripDay;

    @BeforeEach
    void setUp() {
        voteService = new VoteService(itineraryItemRepository, voteOptionRepository, voteRepository, participantRepository);

        User hostUser = new User("host", "hashed-password");
        ReflectionTestUtils.setField(hostUser, "id", HOST_USER_ID);

        Trip trip = new Trip(hostUser, "제주 여행",
                LocalDateTime.of(2026, 9, 1, 9, 0), LocalDateTime.of(2026, 9, 3, 18, 0), 3, "invite-code");
        ReflectionTestUtils.setField(trip, "id", TRIP_ID);

        tripDay = new TripDay(trip, 1, LocalDate.of(2026, 9, 1));
    }

    private ItineraryItem itineraryItem(Long id, ItineraryItemDecisionType decisionType, ItineraryItemStatus status) {
        ItineraryItem item = new ItineraryItem(tripDay, "점심 메뉴", "식사", decisionType, status, 1, null);
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    private VoteOption voteOption(ItineraryItem item, Long id) {
        VoteOption option = new VoteOption(item, "스시", "설명", "AI", null, null);
        ReflectionTestUtils.setField(option, "id", id);
        return option;
    }

    private Participant participant(Long id) {
        Participant participant = new Participant(tripDay.getTrip(), "부모", LocalDateTime.now());
        ReflectionTestUtils.setField(participant, "id", id);
        return participant;
    }

    // ===== 투표 시작 =====

    @Test
    @DisplayName("여러 일정 항목을 한 번에 부모 투표로 올린다")
    void 여러_일정_항목을_한_번에_부모_투표로_올린다() {
        ItineraryItem item1 = itineraryItem(101L, ItineraryItemDecisionType.VOTE, ItineraryItemStatus.PENDING);
        ItineraryItem item2 = itineraryItem(102L, ItineraryItemDecisionType.VOTE, ItineraryItemStatus.PENDING);
        when(itineraryItemRepository.findById(101L)).thenReturn(Optional.of(item1));
        when(itineraryItemRepository.findById(102L)).thenReturn(Optional.of(item2));
        when(voteOptionRepository.countByItineraryItemId(anyLong())).thenReturn(2);

        VoteStartResponseDto response = voteService.startVote(HOST_USER_ID, List.of(101L, 102L));

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).status()).isEqualTo("VOTING");
        assertThat(item1.getStatus()).isEqualTo(ItineraryItemStatus.VOTING);
        assertThat(item2.getStatus()).isEqualTo(ItineraryItemStatus.VOTING);
    }

    @Test
    @DisplayName("일정 항목을 찾을 수 없으면 예외 메시지에 itemId가 포함된다")
    void 투표_시작_일정_항목을_찾을_수_없으면_itemId가_포함된_예외가_발생한다() {
        when(itineraryItemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> voteService.startVote(HOST_USER_ID, List.of(999L)))
                .isInstanceOfSatisfying(ApplicationException.class, exception -> {
                    assertThat(exception.getErrorType()).isEqualTo(TripErrorType.ITINERARY_ITEM_NOT_FOUND);
                    assertThat(exception.getMessage()).contains("itemId: 999");
                });
    }

    @Test
    @DisplayName("요청자가 여행 방장이 아니면 예외가 발생한다")
    void 투표_시작_요청자가_방장이_아니면_예외가_발생한다() {
        ItineraryItem item = itineraryItem(101L, ItineraryItemDecisionType.VOTE, ItineraryItemStatus.PENDING);
        when(itineraryItemRepository.findById(101L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> voteService.startVote(999L, List.of(101L)))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.NOT_TRIP_HOST));
    }

    @Test
    @DisplayName("결정 방식이 투표가 아니면 예외가 발생한다")
    void 투표_시작_결정_방식이_투표가_아니면_예외가_발생한다() {
        ItineraryItem item = itineraryItem(101L, ItineraryItemDecisionType.HOST_PICK, ItineraryItemStatus.PENDING);
        when(itineraryItemRepository.findById(101L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> voteService.startVote(HOST_USER_ID, List.of(101L)))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.ITINERARY_ITEM_NOT_VOTE_TYPE));
    }

    @Test
    @DisplayName("이미 투표가 시작된 일정이면 예외가 발생한다")
    void 투표_시작_이미_시작된_일정이면_예외가_발생한다() {
        ItineraryItem item = itineraryItem(101L, ItineraryItemDecisionType.VOTE, ItineraryItemStatus.VOTING);
        when(itineraryItemRepository.findById(101L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> voteService.startVote(HOST_USER_ID, List.of(101L)))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.ITINERARY_ITEM_ALREADY_OPENED));
    }

    @Test
    @DisplayName("선택지가 2개 미만이면 예외가 발생한다")
    void 투표_시작_선택지가_2개_미만이면_예외가_발생한다() {
        ItineraryItem item = itineraryItem(101L, ItineraryItemDecisionType.VOTE, ItineraryItemStatus.PENDING);
        when(itineraryItemRepository.findById(101L)).thenReturn(Optional.of(item));
        when(voteOptionRepository.countByItineraryItemId(101L)).thenReturn(1);

        assertThatThrownBy(() -> voteService.startVote(HOST_USER_ID, List.of(101L)))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.VOTE_OPTION_COUNT_INSUFFICIENT));
    }

    // ===== 내 투표 제출·변경 =====

    @Test
    @DisplayName("처음 투표하면 새 투표를 저장한다")
    void 내_투표_처음_투표하면_새_투표를_저장한다() {
        ItineraryItem item = itineraryItem(101L, ItineraryItemDecisionType.VOTE, ItineraryItemStatus.VOTING);
        VoteOption option = voteOption(item, 1001L);
        Participant participant = participant(11L);
        ParticipantPrincipal principal = new ParticipantPrincipal(11L, TRIP_ID);

        when(participantRepository.findById(11L)).thenReturn(Optional.of(participant));
        when(itineraryItemRepository.findById(101L)).thenReturn(Optional.of(item));
        when(voteOptionRepository.findByIdAndItineraryItemId(1001L, 101L)).thenReturn(Optional.of(option));
        when(voteRepository.findByItineraryItemIdAndParticipantId(101L, 11L)).thenReturn(Optional.empty());
        when(participantRepository.countByTripId(TRIP_ID)).thenReturn(2L);
        when(voteRepository.countByItineraryItemId(101L)).thenReturn(1L);
        when(itineraryItemRepository.findUnvotedVotingItemsOrderByDayAndSortOrder(TRIP_ID, 11L)).thenReturn(List.of());

        MyVoteBatchResponseDto response = voteService.castVotes(principal, List.of(new MyVoteItemRequestDto(101L, 1001L)));

        assertThat(response.votes()).hasSize(1);
        assertThat(response.votes().get(0).voteOptionId()).isEqualTo(1001L);
        assertThat(response.nextItemId()).isNull();
        assertThat(item.getStatus()).isEqualTo(ItineraryItemStatus.VOTING);
        verify(voteRepository).save(any(Vote.class));
    }

    @Test
    @DisplayName("이미 투표한 참여자가 다시 투표하면 기존 투표의 옵션만 바꾼다")
    void 내_투표_다시_투표하면_기존_투표의_옵션만_바꾼다() {
        ItineraryItem item = itineraryItem(101L, ItineraryItemDecisionType.VOTE, ItineraryItemStatus.VOTING);
        VoteOption oldOption = voteOption(item, 1001L);
        VoteOption newOption = voteOption(item, 1002L);
        Participant participant = participant(11L);
        Vote existingVote = new Vote(oldOption, item, participant);
        ParticipantPrincipal principal = new ParticipantPrincipal(11L, TRIP_ID);

        when(participantRepository.findById(11L)).thenReturn(Optional.of(participant));
        when(itineraryItemRepository.findById(101L)).thenReturn(Optional.of(item));
        when(voteOptionRepository.findByIdAndItineraryItemId(1002L, 101L)).thenReturn(Optional.of(newOption));
        when(voteRepository.findByItineraryItemIdAndParticipantId(101L, 11L)).thenReturn(Optional.of(existingVote));
        when(participantRepository.countByTripId(TRIP_ID)).thenReturn(2L);
        when(voteRepository.countByItineraryItemId(101L)).thenReturn(1L);
        when(itineraryItemRepository.findUnvotedVotingItemsOrderByDayAndSortOrder(TRIP_ID, 11L)).thenReturn(List.of());

        voteService.castVotes(principal, List.of(new MyVoteItemRequestDto(101L, 1002L)));

        assertThat(existingVote.getOption()).isEqualTo(newOption);
        verify(voteRepository, never()).save(any());
    }

    @Test
    @DisplayName("마지막 참여자까지 투표를 마치면 VOTED로 자동 전환된다")
    void 내_투표_전원_투표를_마치면_VOTED로_전환된다() {
        ItineraryItem item = itineraryItem(101L, ItineraryItemDecisionType.VOTE, ItineraryItemStatus.VOTING);
        VoteOption option = voteOption(item, 1001L);
        Participant participant = participant(11L);
        ParticipantPrincipal principal = new ParticipantPrincipal(11L, TRIP_ID);

        when(participantRepository.findById(11L)).thenReturn(Optional.of(participant));
        when(itineraryItemRepository.findById(101L)).thenReturn(Optional.of(item));
        when(voteOptionRepository.findByIdAndItineraryItemId(1001L, 101L)).thenReturn(Optional.of(option));
        when(voteRepository.findByItineraryItemIdAndParticipantId(101L, 11L)).thenReturn(Optional.empty());
        when(participantRepository.countByTripId(TRIP_ID)).thenReturn(2L);
        when(voteRepository.countByItineraryItemId(101L)).thenReturn(2L);
        when(itineraryItemRepository.findUnvotedVotingItemsOrderByDayAndSortOrder(TRIP_ID, 11L)).thenReturn(List.of());

        voteService.castVotes(principal, List.of(new MyVoteItemRequestDto(101L, 1001L)));

        assertThat(item.getStatus()).isEqualTo(ItineraryItemStatus.VOTED);
    }

    @Test
    @DisplayName("이미 VOTED 상태면 전원 투표 완료 여부를 다시 계산하지 않는다")
    void 내_투표_이미_VOTED_상태면_완료_여부를_다시_계산하지_않는다() {
        ItineraryItem item = itineraryItem(101L, ItineraryItemDecisionType.VOTE, ItineraryItemStatus.VOTED);
        VoteOption oldOption = voteOption(item, 1001L);
        VoteOption newOption = voteOption(item, 1002L);
        Participant participant = participant(11L);
        Vote existingVote = new Vote(oldOption, item, participant);
        ParticipantPrincipal principal = new ParticipantPrincipal(11L, TRIP_ID);

        when(participantRepository.findById(11L)).thenReturn(Optional.of(participant));
        when(itineraryItemRepository.findById(101L)).thenReturn(Optional.of(item));
        when(voteOptionRepository.findByIdAndItineraryItemId(1002L, 101L)).thenReturn(Optional.of(newOption));
        when(voteRepository.findByItineraryItemIdAndParticipantId(101L, 11L)).thenReturn(Optional.of(existingVote));
        when(itineraryItemRepository.findUnvotedVotingItemsOrderByDayAndSortOrder(TRIP_ID, 11L)).thenReturn(List.of());

        voteService.castVotes(principal, List.of(new MyVoteItemRequestDto(101L, 1002L)));

        assertThat(item.getStatus()).isEqualTo(ItineraryItemStatus.VOTED);
        verify(participantRepository, never()).countByTripId(anyLong());
    }

    @Test
    @DisplayName("다음 미투표 VOTING 항목이 있으면 nextItemId로 응답한다")
    void 내_투표_다음_미투표_항목이_있으면_nextItemId를_응답한다() {
        ItineraryItem item = itineraryItem(101L, ItineraryItemDecisionType.VOTE, ItineraryItemStatus.VOTING);
        ItineraryItem nextItem = itineraryItem(105L, ItineraryItemDecisionType.VOTE, ItineraryItemStatus.VOTING);
        VoteOption option = voteOption(item, 1001L);
        Participant participant = participant(11L);
        ParticipantPrincipal principal = new ParticipantPrincipal(11L, TRIP_ID);

        when(participantRepository.findById(11L)).thenReturn(Optional.of(participant));
        when(itineraryItemRepository.findById(101L)).thenReturn(Optional.of(item));
        when(voteOptionRepository.findByIdAndItineraryItemId(1001L, 101L)).thenReturn(Optional.of(option));
        when(voteRepository.findByItineraryItemIdAndParticipantId(101L, 11L)).thenReturn(Optional.empty());
        when(participantRepository.countByTripId(TRIP_ID)).thenReturn(2L);
        when(voteRepository.countByItineraryItemId(101L)).thenReturn(1L);
        when(itineraryItemRepository.findUnvotedVotingItemsOrderByDayAndSortOrder(TRIP_ID, 11L)).thenReturn(List.of(nextItem));

        MyVoteBatchResponseDto response = voteService.castVotes(principal, List.of(new MyVoteItemRequestDto(101L, 1001L)));

        assertThat(response.nextItemId()).isEqualTo(105L);
    }

    @Test
    @DisplayName("일정 항목을 찾을 수 없으면 예외 메시지에 itemId가 포함된다")
    void 내_투표_일정_항목을_찾을_수_없으면_itemId가_포함된_예외가_발생한다() {
        Participant participant = participant(11L);
        ParticipantPrincipal principal = new ParticipantPrincipal(11L, TRIP_ID);
        when(participantRepository.findById(11L)).thenReturn(Optional.of(participant));
        when(itineraryItemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> voteService.castVotes(principal, List.of(new MyVoteItemRequestDto(999L, 1L))))
                .isInstanceOfSatisfying(ApplicationException.class, exception -> {
                    assertThat(exception.getErrorType()).isEqualTo(TripErrorType.ITINERARY_ITEM_NOT_FOUND);
                    assertThat(exception.getMessage()).contains("itemId: 999");
                });
    }

    @Test
    @DisplayName("다른 여행의 참여자면 예외가 발생한다")
    void 내_투표_다른_여행의_참여자면_예외가_발생한다() {
        ItineraryItem item = itineraryItem(101L, ItineraryItemDecisionType.VOTE, ItineraryItemStatus.VOTING);
        Participant participant = participant(11L);
        ParticipantPrincipal principal = new ParticipantPrincipal(11L, 999L);
        when(participantRepository.findById(11L)).thenReturn(Optional.of(participant));
        when(itineraryItemRepository.findById(101L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> voteService.castVotes(principal, List.of(new MyVoteItemRequestDto(101L, 1001L))))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.TRIP_PARTICIPANT_MISMATCH));
    }

    @Test
    @DisplayName("투표 가능한 상태가 아니면 예외가 발생한다")
    void 내_투표_투표_가능한_상태가_아니면_예외가_발생한다() {
        ItineraryItem item = itineraryItem(101L, ItineraryItemDecisionType.VOTE, ItineraryItemStatus.PENDING);
        Participant participant = participant(11L);
        ParticipantPrincipal principal = new ParticipantPrincipal(11L, TRIP_ID);
        when(participantRepository.findById(11L)).thenReturn(Optional.of(participant));
        when(itineraryItemRepository.findById(101L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> voteService.castVotes(principal, List.of(new MyVoteItemRequestDto(101L, 1001L))))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.ITINERARY_ITEM_NOT_VOTABLE));
    }

    @Test
    @DisplayName("선택지가 해당 일정 항목의 것이 아니면 예외가 발생한다")
    void 내_투표_선택지가_다른_항목의_것이면_예외가_발생한다() {
        ItineraryItem item = itineraryItem(101L, ItineraryItemDecisionType.VOTE, ItineraryItemStatus.VOTING);
        Participant participant = participant(11L);
        ParticipantPrincipal principal = new ParticipantPrincipal(11L, TRIP_ID);
        when(participantRepository.findById(11L)).thenReturn(Optional.of(participant));
        when(itineraryItemRepository.findById(101L)).thenReturn(Optional.of(item));
        when(voteOptionRepository.findByIdAndItineraryItemId(9999L, 101L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> voteService.castVotes(principal, List.of(new MyVoteItemRequestDto(101L, 9999L))))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.VOTE_OPTION_NOT_FOUND));
    }

    // ===== 일정 항목 확정 =====

    @Test
    @DisplayName("방장이 선택지를 직접 골라 확정한다")
    void 확정_방장이_선택지를_직접_골라_확정한다() {
        ItineraryItem item = itineraryItem(101L, ItineraryItemDecisionType.VOTE, ItineraryItemStatus.VOTING);
        VoteOption option = voteOption(item, 1001L);
        when(itineraryItemRepository.findById(101L)).thenReturn(Optional.of(item));
        when(voteOptionRepository.findByIdAndItineraryItemId(1001L, 101L)).thenReturn(Optional.of(option));

        ItineraryItemConfirmationResponseDto response = voteService.confirm(HOST_USER_ID, 101L, 1001L);

        assertThat(response.status()).isEqualTo("CONFIRMED");
        assertThat(response.confirmedOptionId()).isEqualTo(1001L);
        assertThat(item.getStatus()).isEqualTo(ItineraryItemStatus.CONFIRMED);
    }

    @Test
    @DisplayName("전원 투표가 끝나지 않은 VOTING 상태에서도 확정할 수 있다")
    void 확정_VOTED_상태에서도_확정할_수_있다() {
        ItineraryItem item = itineraryItem(101L, ItineraryItemDecisionType.VOTE, ItineraryItemStatus.VOTED);
        VoteOption option = voteOption(item, 1001L);
        when(itineraryItemRepository.findById(101L)).thenReturn(Optional.of(item));
        when(voteOptionRepository.findByIdAndItineraryItemId(1001L, 101L)).thenReturn(Optional.of(option));

        voteService.confirm(HOST_USER_ID, 101L, 1001L);

        assertThat(item.getStatus()).isEqualTo(ItineraryItemStatus.CONFIRMED);
    }

    @Test
    @DisplayName("일정 항목을 찾을 수 없으면 예외가 발생한다")
    void 확정_일정_항목을_찾을_수_없으면_예외가_발생한다() {
        when(itineraryItemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> voteService.confirm(HOST_USER_ID, 999L, 1001L))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.ITINERARY_ITEM_NOT_FOUND));
    }

    @Test
    @DisplayName("요청자가 여행 방장이 아니면 예외가 발생한다")
    void 확정_요청자가_방장이_아니면_예외가_발생한다() {
        ItineraryItem item = itineraryItem(101L, ItineraryItemDecisionType.VOTE, ItineraryItemStatus.VOTING);
        when(itineraryItemRepository.findById(101L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> voteService.confirm(999L, 101L, 1001L))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.NOT_TRIP_HOST));
    }

    @Test
    @DisplayName("투표 중이거나 투표가 끝난 상태가 아니면 예외가 발생한다")
    void 확정_투표_가능한_상태가_아니면_예외가_발생한다() {
        ItineraryItem item = itineraryItem(101L, ItineraryItemDecisionType.VOTE, ItineraryItemStatus.PENDING);
        when(itineraryItemRepository.findById(101L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> voteService.confirm(HOST_USER_ID, 101L, 1001L))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.ITINERARY_ITEM_NOT_VOTABLE));
    }

    @Test
    @DisplayName("선택지가 해당 일정 항목의 것이 아니면 예외가 발생한다")
    void 확정_선택지가_다른_항목의_것이면_예외가_발생한다() {
        ItineraryItem item = itineraryItem(101L, ItineraryItemDecisionType.VOTE, ItineraryItemStatus.VOTING);
        when(itineraryItemRepository.findById(101L)).thenReturn(Optional.of(item));
        when(voteOptionRepository.findByIdAndItineraryItemId(9999L, 101L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> voteService.confirm(HOST_USER_ID, 101L, 9999L))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.VOTE_OPTION_NOT_FOUND));
    }

    // ===== 일정 확정 해제 =====

    @Test
    @DisplayName("확정된 일정을 다시 투표 상태로 되돌린다")
    void 확정_해제_확정된_일정을_다시_투표_상태로_되돌린다() {
        ItineraryItem item = itineraryItem(101L, ItineraryItemDecisionType.VOTE, ItineraryItemStatus.CONFIRMED);
        VoteOption option = voteOption(item, 1001L);
        item.confirm(option);
        when(itineraryItemRepository.findById(101L)).thenReturn(Optional.of(item));

        ItineraryItemStatusDto response = voteService.unconfirm(HOST_USER_ID, 101L);

        assertThat(response.status()).isEqualTo("VOTING");
        assertThat(item.getStatus()).isEqualTo(ItineraryItemStatus.VOTING);
        assertThat(item.getConfirmedOption()).isNull();
        verify(voteRepository).deleteAllByItineraryItemId(101L);
        verify(voteOptionRepository, never()).deleteAllByItineraryItemId(any());
    }

    @Test
    @DisplayName("HOST_PICK 일정은 확정을 해제하면 PENDING으로 되돌아가고 기존 선택지도 지워진다")
    void 확정_해제_HOST_PICK_일정은_PENDING으로_되돌아가고_선택지도_지워진다() {
        ItineraryItem item = itineraryItem(101L, ItineraryItemDecisionType.HOST_PICK, ItineraryItemStatus.CONFIRMED);
        VoteOption option = voteOption(item, 1001L);
        item.confirm(option);
        when(itineraryItemRepository.findById(101L)).thenReturn(Optional.of(item));

        ItineraryItemStatusDto response = voteService.unconfirm(HOST_USER_ID, 101L);

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(item.getStatus()).isEqualTo(ItineraryItemStatus.PENDING);
        assertThat(item.getConfirmedOption()).isNull();
        verify(voteOptionRepository).deleteAllByItineraryItemId(101L);
    }

    @Test
    @DisplayName("일정 항목을 찾을 수 없으면 예외가 발생한다")
    void 확정_해제_일정_항목을_찾을_수_없으면_예외가_발생한다() {
        when(itineraryItemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> voteService.unconfirm(HOST_USER_ID, 999L))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.ITINERARY_ITEM_NOT_FOUND));
    }

    @Test
    @DisplayName("요청자가 여행 방장이 아니면 예외가 발생한다")
    void 확정_해제_요청자가_방장이_아니면_예외가_발생한다() {
        ItineraryItem item = itineraryItem(101L, ItineraryItemDecisionType.VOTE, ItineraryItemStatus.CONFIRMED);
        when(itineraryItemRepository.findById(101L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> voteService.unconfirm(999L, 101L))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.NOT_TRIP_HOST));
    }

    @Test
    @DisplayName("확정된 일정이 아니면 예외가 발생한다")
    void 확정_해제_확정된_일정이_아니면_예외가_발생한다() {
        ItineraryItem item = itineraryItem(101L, ItineraryItemDecisionType.VOTE, ItineraryItemStatus.VOTING);
        when(itineraryItemRepository.findById(101L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> voteService.unconfirm(HOST_USER_ID, 101L))
                .isInstanceOfSatisfying(ApplicationException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(TripErrorType.ITINERARY_ITEM_NOT_CONFIRMED));
    }
}
