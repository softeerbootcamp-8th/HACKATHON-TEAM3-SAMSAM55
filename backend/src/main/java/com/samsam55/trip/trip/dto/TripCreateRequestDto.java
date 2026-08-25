package com.samsam55.trip.trip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record TripCreateRequestDto(
        @NotBlank(message = "여행 제목은 필수입니다.")
        @Size(max = 100, message = "여행 제목은 100자 이하여야 합니다.")
        String title,
        @NotNull(message = "여행 시작일은 필수입니다.")
        LocalDate startDate,
        @NotNull(message = "여행 종료일은 필수입니다.")
        LocalDate endDate,
        @NotNull(message = "동행자 목록은 필수입니다.")
        List<@NotBlank(message = "동행자 역할은 필수입니다.")
                @Size(max = 50, message = "동행자 역할은 50자 이하여야 합니다.") String> companions
) {
}
