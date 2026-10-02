package com.ieltsaitutor.learning.plan;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;

@RestController
@RequestMapping("/api/me/today")
public class TodaysPlanController {
    private final TodaysPlanService service;
    public TodaysPlanController(TodaysPlanService service) { this.service = service; }

    @GetMapping
    public TodaysPlanService.TodaysPlanResponse get(@RequestParam(required = false) Integer minutes,
            @RequestAttribute(value = AuthInterceptor.PRINCIPAL_ATTRIBUTE, required = false) AuthPrincipal principal) {
        if (minutes != null && (minutes < 5 || minutes > 240)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minutes must be 5..240");
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Đăng nhập để xem kế hoạch học.");
        UUID userId = principal.userId();
        return service.plan(userId, minutes);
    }
}
