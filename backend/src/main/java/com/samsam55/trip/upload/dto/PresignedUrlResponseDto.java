package com.samsam55.trip.upload.dto;

/**
 * presigned URL 발급 응답. {@code uploadUrl}로 클라이언트가 S3에 직접 PUT하고,
 * {@code key}는 이후 선택지 생성·수정 요청에 그대로 실어 보낸다.
 * {@code imageUrl}은 업로드가 끝난 뒤 그 사진을 보여줄 때 쓰는 공개 URL이다.
 */
public record PresignedUrlResponseDto(
        String uploadUrl,
        String key,
        String imageUrl
) {
}
