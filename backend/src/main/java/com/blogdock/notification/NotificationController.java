package com.blogdock.notification;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.blogdock.auth.LoginMember;
import com.blogdock.notification.NotificationService.NotificationItem;
import com.blogdock.notification.NotificationService.NotificationPage;

/** NOTI-02 내 알림. 모두 로그인이 필요하다. */
@RestController
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** 위쪽 메뉴의 종 숫자. 화면이 60초마다 묻는다. */
    @GetMapping("/api/notifications/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal LoginMember login) {
        return Map.of("count", notificationService.unreadCount(LoginMember.require(login)));
    }

    @GetMapping("/api/notifications")
    public NotificationPage list(@AuthenticationPrincipal LoginMember login,
                                 @RequestParam(required = false) String cursor) {
        return notificationService.list(LoginMember.require(login), cursor);
    }

    @PostMapping("/api/notifications/{id}/read")
    public NotificationItem read(@PathVariable Long id, @AuthenticationPrincipal LoginMember login) {
        return notificationService.read(id, LoginMember.require(login));
    }

    @PostMapping("/api/notifications/read-all")
    public Map<String, Long> readAll(@AuthenticationPrincipal LoginMember login) {
        return Map.of("count", notificationService.readAll(LoginMember.require(login)));
    }
}
