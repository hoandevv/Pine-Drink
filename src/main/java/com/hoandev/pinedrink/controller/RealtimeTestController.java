package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import com.hoandev.pinedrink.realtime.RealtimeDestination;
import com.hoandev.pinedrink.realtime.RealtimeEvent;
import com.hoandev.pinedrink.realtime.RealtimeEventFactory;
import com.hoandev.pinedrink.realtime.RealtimeEventPublisher;
import com.hoandev.pinedrink.realtime.RealtimeEventType;
import com.hoandev.pinedrink.realtime.RealtimePublishService;
import com.hoandev.pinedrink.realtime.payload.NotificationPayload;
import com.hoandev.pinedrink.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**/
@RestController
@RequestMapping("/api/v1/realtime/test")
public class RealtimeTestController {

    private final RealtimeEventFactory eventFactory;
    private final RealtimePublishService realtimePublishService;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public RealtimeTestController(RealtimeEventFactory eventFactory,
                                  RealtimePublishService realtimePublishService,
                                  RealtimeEventPublisher realtimeEventPublisher) {
        this.eventFactory = eventFactory;
        this.realtimePublishService = realtimePublishService;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    @PostMapping("/notification")
    public BaseResponse<Void> publishTestNotification(@AuthenticationPrincipal UserPrincipal user) {
        NotificationPayload payload = new NotificationPayload(
                "Realtime test",
                "Realtime foundation is working",
                "INFO",
                null
        );
        RealtimeEvent<NotificationPayload> event = eventFactory.create(
                RealtimeEventType.NOTIFICATION_CREATED,
                user.getId(),
                "ACCOUNT",
                user.getId(),
                payload
        );

        realtimePublishService.publishToUser(user.getId(), RealtimeDestination.USER_NOTIFICATIONS, event);
        realtimeEventPublisher.publish("notification.created", event);

        return BaseResponse.success(null, "Realtime test notification published");
    }
}
