package com.astral.express.pccms.notification.controller;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.notification.dto.response.NotificationResponse;
import com.astral.express.pccms.notification.dto.response.ReadAllNotificationsResponse;
import com.astral.express.pccms.notification.dto.response.UnreadCountResponse;
import com.astral.express.pccms.notification.entity.NotificationStatus;
import com.astral.express.pccms.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listMyNotifications_success() throws Exception {
        PageResponse<NotificationResponse> page = PageResponse.of(new PageImpl<>(List.of()));
        given(notificationService.listMyNotifications(any(), any(Pageable.class))).willReturn(page);

        mockMvc.perform(get("/v1/notifications/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void listMyNotifications_withStatusFilter() throws Exception {
        PageResponse<NotificationResponse> page = PageResponse.of(new PageImpl<>(List.of()));
        given(notificationService.listMyNotifications(any(NotificationStatus.class), any(Pageable.class))).willReturn(page);

        mockMvc.perform(get("/v1/notifications/my").param("status", "UNREAD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void unreadCount_success() throws Exception {
        given(notificationService.getUnreadCount()).willReturn(new UnreadCountResponse(4));

        mockMvc.perform(get("/v1/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(4));
    }

    @Test
    void markAllRead_success() throws Exception {
        given(notificationService.markAllRead()).willReturn(new ReadAllNotificationsResponse(2));

        mockMvc.perform(patch("/v1/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updatedCount").value(2));
    }

    @Test
    void markRead_success() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(patch("/v1/notifications/{notificationId}/read", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void archive_success() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(patch("/v1/notifications/{notificationId}/archive", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
