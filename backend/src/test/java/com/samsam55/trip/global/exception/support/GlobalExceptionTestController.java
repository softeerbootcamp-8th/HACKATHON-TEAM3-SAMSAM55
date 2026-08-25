package com.samsam55.trip.global.exception.support;

import com.samsam55.trip.global.exception.ApplicationException;
import com.samsam55.trip.global.exception.GlobalErrorType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@link com.samsam55.trip.global.exception.GlobalExceptionHandlerIntegrationTest}
 * 전용 테스트 컨트롤러. 실제 도메인 API가 아니다.
 */
@RestController
public class GlobalExceptionTestController {

    @PostMapping("/test/echo")
    public String echo(@Valid @RequestBody EchoRequest request) {
        return request.name();
    }

    @GetMapping("/test/not-found")
    public String notFound() {
        throw new ApplicationException(GlobalErrorType.NOT_FOUND);
    }

    public record EchoRequest(@NotBlank(message = "이름은 필수입니다.") String name) {
    }
}
