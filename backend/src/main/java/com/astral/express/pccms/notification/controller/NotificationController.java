package com.astral.express.pccms.notification.controller;

import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.notification.dto.response.NotificationResponse;
import com.astral.express.pccms.notification.dto.response.ReadAllNotificationsResponse;
import com.astral.express.pccms.notification.dto.response.UnreadCountResponse;
import com.astral.express.pccms.notification.entity.NotificationStatus;
import com.astral.express.pccms.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<NotificationResponse>> listMyNotifications(
            @RequestParam(required = false) NotificationStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(notificationService.listMyNotifications(status, pageable));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UnreadCountResponse> getUnreadCount() {
        return ApiResponse.success(notificationService.getUnreadCount());
    }

    @PatchMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ReadAllNotificationsResponse> markAllRead() {
        return ApiResponse.success(notificationService.markAllRead());
    }

    @PatchMapping("/{notificationId}/read")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<NotificationResponse> markRead(@PathVariable UUID notificationId) {
        return ApiResponse.success(notificationService.markRead(notificationId));
    }

    @PatchMapping("/{notificationId}/archive")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<NotificationResponse> archive(@PathVariable UUID notificationId) {
        return ApiResponse.success(notificationService.archive(notificationId));
    }
}
