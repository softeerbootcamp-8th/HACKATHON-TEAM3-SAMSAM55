package com.samsam55.trip.trip.dto;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * 일정 항목 생성 요청의 멀티파트 폼 필드를 담는다. {@code request}를 String이 아니라
 * DTO로 직접 받으면(@RequestPart) 브라우저가 보내는 문자열 파트에 Content-Type이
 * 없어 415가 나고, {@code @RequestParam}으로 따로 받으면 springdoc이 이를 쿼리
 * 파라미터로 잘못 문서화해 Orval 생성 결과가 불안정해진다. {@code @ModelAttribute}로
 * optionImages와 함께 한 객체로 받으면 두 문제 모두 피할 수 있다.
 */
public record ItineraryItemCreateForm(
        String request,
        List<MultipartFile> optionImages
) {
}
