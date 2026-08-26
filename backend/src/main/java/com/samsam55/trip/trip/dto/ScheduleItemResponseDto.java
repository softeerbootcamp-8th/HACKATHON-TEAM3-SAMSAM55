package com.samsam55.trip.trip.dto;

import com.samsam55.trip.trip.entity.ItineraryItem;

public record ScheduleItemResponseDto(
        Long id,
        String name,
        String category,
        String decisionType,
        String status,
        Integer sortOrder,
        int votedCount,
        long totalParticipants,
        // 이 항목이 VOTING 상태이고, 조회하는 참여자 본인이 아직 투표하지 않았으면 true다.
        // 다른 참여자의 투표 여부와는 무관하다 — status만으로는 이 참여자가 투표를 마쳤는지 알 수 없다.
        boolean needsVote,
        ScheduleConfirmedOptionResponseDto confirmedOption
) {

    public static ScheduleItemResponseDto of(
            ItineraryItem item,
            int votedCount,
            long totalParticipants,
            boolean needsVote,
            String confirmedOptionImageUrl
    ) {
        return new ScheduleItemResponseDto(
                item.getId(),
                item.getName(),
                item.getCategory(),
                item.getDecisionType().name(),
                item.getStatus().name(),
                item.getSortOrder(),
                votedCount,
                totalParticipants,
                needsVote,
                ScheduleConfirmedOptionResponseDto.from(item.getConfirmedOption(), confirmedOptionImageUrl)
        );
    }
}
