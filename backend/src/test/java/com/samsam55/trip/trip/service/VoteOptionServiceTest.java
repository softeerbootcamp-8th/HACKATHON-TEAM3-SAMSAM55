package com.samsam55.trip.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.trip.dto.VoteOptionImageDto;
import com.samsam55.trip.trip.entity.ItineraryItem;
import com.samsam55.trip.trip.entity.VoteOption;
import com.samsam55.trip.trip.exception.TripErrorType;
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
    private ItineraryItem itineraryItem;

    private VoteOptionService voteOptionService;

    @BeforeEach
    void setUp() {
        voteOptionService = new VoteOptionService(voteOptionRepository);
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
}
