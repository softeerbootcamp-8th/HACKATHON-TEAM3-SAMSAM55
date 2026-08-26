package com.samsam55.trip.trip.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record MyVoteBatchRequestDto(
        @NotEmpty(message = "투표할 일정 항목을 1개 이상 보내야 합니다.")
        @Valid
        List<MyVoteItemRequestDto> votes
) {
}
