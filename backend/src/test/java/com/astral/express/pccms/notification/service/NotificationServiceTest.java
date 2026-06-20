package com.astral.express.pccms.notification.service;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.notification.dto.response.NotificationResponse;
import com.astral.express.pccms.notification.entity.Notification;
import com.astral.express.pccms.notification.entity.NotificationStatus;
import com.astral.express.pccms.notification.repository.NotificationRepository;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContextService securityContextService;

    @Mock
    private NotificationPublisher notificationPublisher;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void should_CreateNotification_when_RecipientExists_TC_NOTIF_SVC_001() {
        // GIVEN
        UUID recipientId = UUID.randomUUID();
        Users mockUser = new Users();
        mockUser.setId(recipientId);
        
        Notification savedNotification = Notification.builder()
                .id(UUID.randomUUID())
                .recipient(mockUser)
                .sourceType("SYSTEM")
                .notificationType("ALERT")
                .title("Title")
                .body("Body")
                .statusCode(NotificationStatus.UNREAD)
                .build();

        given(userRepository.findById(recipientId)).willReturn(Optional.of(mockUser));
        given(notificationRepository.save(any(Notification.class))).willReturn(savedNotification);

        // WHEN
        NotificationResponse response = notificationService.createNotification(
                recipientId, "SYSTEM", null, "ALERT", "Title", "Body");

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.recipientUserId()).isEqualTo(recipientId);
        assertThat(response.statusCode()).isEqualTo(NotificationStatus.UNREAD);
        assertThat(response.title()).isEqualTo("Title");
        verify(notificationPublisher).publish(any(NotificationResponse.class));
    }

    @Test
    void should_ThrowException_when_RecipientNotFound_TC_NOTIF_SVC_002() {
        // GIVEN
        UUID recipientId = UUID.randomUUID();
        given(userRepository.findById(recipientId)).willReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> notificationService.createNotification(
                recipientId, "SYSTEM", null, "ALERT", "Title", "Body"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ACC_002_USER_NOT_FOUND);
    }

    @Test
    void should_ReturnPageResponse_when_UserIsAuthenticated_TC_NOTIF_SVC_003() {
        // GIVEN
        UUID currentUserId = UUID.randomUUID();
        Users mockUser = new Users();
        mockUser.setId(currentUserId);
        
        Notification notif = Notification.builder()
                .id(UUID.randomUUID())
                .recipient(mockUser)
                .statusCode(NotificationStatus.UNREAD)
                .build();
                
        Page<Notification> page = new PageImpl<>(List.of(notif));
        
        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
        given(notificationRepository.findByRecipientIdAndStatusCodeInOrderByCreatedAtDesc(any(UUID.class), any(Set.class), any(Pageable.class)))
                .willReturn(page);

        // WHEN
        PageResponse<NotificationResponse> response = notificationService.listMyNotifications(null, Pageable.unpaged());

        // THEN
        assertThat(response.data().content()).hasSize(1);
        assertThat(response.data().content().get(0).statusCode()).isEqualTo(NotificationStatus.UNREAD);
    }

    @Test
    void should_ThrowUnauthorized_when_NoUserInContext_TC_NOTIF_SVC_004() {
        // GIVEN
        given(securityContextService.getCurrentUserId()).willReturn(null);

        // WHEN & THEN
        assertThatThrownBy(() -> notificationService.listMyNotifications(null, Pageable.unpaged()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_401_UNAUTHORIZED);
    }

    @Test
    void should_MarkNotificationAsRead_when_ValidIdAndOwner_TC_NOTIF_SVC_005() {
        // GIVEN
        UUID currentUserId = UUID.randomUUID();
        UUID notifId = UUID.randomUUID();
        Users mockUser = new Users();
        mockUser.setId(currentUserId);
        
        Notification notif = Notification.builder()
                .id(notifId)
                .recipient(mockUser)
                .statusCode(NotificationStatus.UNREAD)
                .build();
                
        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
        given(notificationRepository.findByIdAndRecipientId(notifId, currentUserId)).willReturn(Optional.of(notif));
        given(notificationRepository.save(any(Notification.class))).willAnswer(inv -> inv.getArgument(0));

        // WHEN
        NotificationResponse response = notificationService.markRead(notifId);

        // THEN
        assertThat(response.statusCode()).isEqualTo(NotificationStatus.READ);
        assertThat(response.readAt()).isNotNull();
    }

    @Test
    void should_ArchiveNotification_when_ValidIdAndOwner_TC_NOTIF_SVC_006() {
        // GIVEN
        UUID currentUserId = UUID.randomUUID();
        UUID notifId = UUID.randomUUID();
        Users mockUser = new Users();
        mockUser.setId(currentUserId);
        
        Notification notif = Notification.builder()
                .id(notifId)
                .recipient(mockUser)
                .statusCode(NotificationStatus.READ)
                .readAt(OffsetDateTime.now())
                .build();
                
        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
        given(notificationRepository.findByIdAndRecipientId(notifId, currentUserId)).willReturn(Optional.of(notif));
        given(notificationRepository.save(any(Notification.class))).willAnswer(inv -> inv.getArgument(0));

        // WHEN
        NotificationResponse response = notificationService.archive(notifId);

        // THEN
        assertThat(response.statusCode()).isEqualTo(NotificationStatus.ARCHIVED);
    }

    @Test
    void should_ThrowNotFound_when_NotificationIsMissingOrForeign_TC_NOTIF_SVC_007() {
        // GIVEN
        UUID currentUserId = UUID.randomUUID();
        UUID foreignNotifId = UUID.randomUUID();
        
        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
        given(notificationRepository.findByIdAndRecipientId(foreignNotifId, currentUserId)).willReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> notificationService.markRead(foreignNotifId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_NOTIFICATION_001_NOT_FOUND);
    }

    @Test
    void archive_shouldSetReadAt_when_ReadAtIsNull() {
        UUID currentUserId = UUID.randomUUID();
        UUID notifId = UUID.randomUUID();
        Users mockUser = new Users();
        mockUser.setId(currentUserId);
        
        Notification notif = Notification.builder()
                .id(notifId)
                .recipient(mockUser)
                .statusCode(NotificationStatus.UNREAD)
                .readAt(null)
                .build();
                
        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
        given(notificationRepository.findByIdAndRecipientId(notifId, currentUserId)).willReturn(Optional.of(notif));
        given(notificationRepository.save(any(Notification.class))).willAnswer(inv -> inv.getArgument(0));

        NotificationResponse response = notificationService.archive(notifId);

        assertThat(response.statusCode()).isEqualTo(NotificationStatus.ARCHIVED);
        assertThat(response.readAt()).isNotNull();
    }

    @Test
    void listMyNotifications_shouldFilterByRequestedStatus() {
        UUID currentUserId = UUID.randomUUID();
        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
        given(notificationRepository.findByRecipientIdAndStatusCodeOrderByCreatedAtDesc(
                currentUserId, NotificationStatus.ARCHIVED, Pageable.unpaged()))
                .willReturn(Page.empty());

        notificationService.listMyNotifications(NotificationStatus.ARCHIVED, Pageable.unpaged());

        verify(notificationRepository).findByRecipientIdAndStatusCodeOrderByCreatedAtDesc(
                currentUserId, NotificationStatus.ARCHIVED, Pageable.unpaged());
    }

    @Test
    void unreadCount_shouldOnlyCountCurrentUsersUnreadNotifications() {
        UUID currentUserId = UUID.randomUUID();
        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
        given(notificationRepository.countByRecipientIdAndStatusCode(currentUserId, NotificationStatus.UNREAD))
                .willReturn(7L);

        assertThat(notificationService.getUnreadCount().unreadCount()).isEqualTo(7L);
    }

    @Test
    void markAllRead_shouldUpdateOnlyCurrentUsersUnreadNotifications() {
        UUID currentUserId = UUID.randomUUID();
        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
        given(notificationRepository.markAllRead(eq(currentUserId), any(OffsetDateTime.class))).willReturn(3);

        assertThat(notificationService.markAllRead().updatedCount()).isEqualTo(3);
    }

    @Test
    void markRead_shouldLeaveArchivedNotificationArchived() {
        UUID currentUserId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        Users recipient = new Users();
        recipient.setId(currentUserId);
        OffsetDateTime readAt = OffsetDateTime.now().minusDays(1);
        Notification notification = Notification.builder()
                .id(notificationId)
                .recipient(recipient)
                .statusCode(NotificationStatus.ARCHIVED)
                .readAt(readAt)
                .build();
        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
        given(notificationRepository.findByIdAndRecipientId(notificationId, currentUserId))
                .willReturn(Optional.of(notification));

        NotificationResponse response = notificationService.markRead(notificationId);

        assertThat(response.statusCode()).isEqualTo(NotificationStatus.ARCHIVED);
        assertThat(response.readAt()).isEqualTo(readAt);
    }

}
