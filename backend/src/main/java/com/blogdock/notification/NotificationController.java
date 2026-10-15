package com.blogdock.notification;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.blogdock.auth.LoginMember;
import com.blogdock.notification.NotificationService.NotificationItem;
import com.blogdock.notification.NotificationService.NotificationPage;
import com.blogdock.subscription.SubscriptionService;

/** NOTI-02 내 알림. 모두 로그인이 필요하다. */
@RestController
public class NotificationController {

    private final NotificationService notificationService;
    private final SubscriptionService subscriptionService;

    public NotificationController(NotificationService notificationService, SubscriptionService subscriptionService) {
        this.notificationService = notificationService;
        this.subscriptionService = subscriptionService;
    }

    /**
     * SUB-07 구독 알림에서 맞구독. 상대가 블로그가 있으면 바로 구독, 없으면 예약해 두고
     * 상대가 블로그를 만들 때 자동으로 구독한다. 알림은 읽음으로 바꾼다.
     */
    @PostMapping("/api/notifications/{id}/subscribe-back")
    public NotificationItem subscribeBack(@PathVariable Long id, @AuthenticationPrincipal LoginMember login) {
        Long memberId = LoginMember.require(login);
        subscriptionService.reserve(memberId, notificationService.subscriberOf(id, memberId));
        return notificationService.read(id, memberId);
    }

    @DeleteMapping("/api/notifications/{id}/subscribe-back")
    public NotificationItem cancelReservation(@PathVariable Long id, @AuthenticationPrincipal LoginMember login) {
        Long memberId = LoginMember.require(login);
        subscriptionService.cancelReservation(memberId, notificationService.subscriberOf(id, memberId));
        return notificationService.read(id, memberId);
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
