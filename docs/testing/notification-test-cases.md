# Notification Subsystem Test Case Specification

## Module Overview
This document specifies the P0/P1 testing requirements for the `NotificationService`, `EmailService`, and `EmailTemplateService` within the Notification subsystem.

## A. NotificationService

### TC-NOTIF-SVC-001: Create Notification - Success
- **Feature / module:** Notification (`NotificationService`)
- **Test type:** Unit Test
- **Tool:** Mockito/JUnit
- **Priority:** P0
- **Objective:** Verify that a notification is created successfully when given a valid recipient ID.
- **Preconditions:** Recipient user exists in DB.
- **Input data:** `recipientUserId` = valid UUID
- **Test steps:**
  1. Mock `userRepository.findById` to return a valid User.
  2. Call `notificationService.createNotification(...)`.
- **Expected result:** Notification is saved with status `UNREAD`. Returns valid `NotificationResponse`.
- **Automation target:** `NotificationServiceTest.should_CreateNotification_when_RecipientExists()`

### TC-NOTIF-SVC-002: Create Notification - User Not Found
- **Feature / module:** Notification (`NotificationService`)
- **Test type:** Unit Test
- **Tool:** Mockito/JUnit
- **Priority:** P0
- **Objective:** Reject notification creation if the recipient user does not exist.
- **Preconditions:** None.
- **Input data:** `recipientUserId` = invalid UUID
- **Test steps:** Mock `userRepository.findById` to return empty.
- **Expected result:** Throws `BusinessException`.
- **Expected error code:** `ErrorCode.ERR_ACC_002_USER_NOT_FOUND`
- **Automation target:** `NotificationServiceTest.should_ThrowException_when_RecipientNotFound()`

### TC-NOTIF-SVC-003: List My Notifications - Success
- **Feature / module:** Notification (`NotificationService`)
- **Test type:** Unit Test
- **Tool:** Mockito/JUnit
- **Priority:** P0
- **Objective:** Ensure the currently authenticated user can retrieve their notifications.
- **Preconditions:** Authenticated user with existing notifications.
- **Input data:** Valid `Pageable` request.
- **Test steps:**
  1. Mock `SecurityContextService.getCurrentUserId()` to return a valid UUID.
  2. Mock `notificationRepository.findByRecipientIdOrderByCreatedAtDesc` to return a Page of notifications.
- **Expected result:** Returns `PageResponse<NotificationResponse>`.
- **Automation target:** `NotificationServiceTest.should_ReturnPageResponse_when_UserIsAuthenticated()`

### TC-NOTIF-SVC-004: Unauthorized Access Detection
- **Feature / module:** Notification (`NotificationService`)
- **Test type:** Unit Test
- **Tool:** Mockito/JUnit
- **Priority:** P0
- **Objective:** Ensure all methods requiring the current user throw an error if unauthenticated.
- **Preconditions:** User is NOT authenticated.
- **Input data:** N/A
- **Test steps:**
  1. Mock `SecurityContextService.getCurrentUserId()` to return `null`.
  2. Call `listMyNotifications()`, `markRead()`, or `archive()`.
- **Expected result:** Throws `BusinessException`.
- **Expected error code:** `ErrorCode.ERR_401_UNAUTHORIZED`
- **Automation target:** `NotificationServiceTest.should_ThrowUnauthorized_when_NoUserInContext()`

### TC-NOTIF-SVC-005: Mark Read - Success
- **Feature / module:** Notification (`NotificationService`)
- **Test type:** Unit Test
- **Tool:** Mockito/JUnit
- **Priority:** P0
- **Objective:** Ensure a user can mark their own notification as read.
- **Preconditions:** Authenticated user, valid notification ID owned by the user.
- **Input data:** Valid `notificationId`
- **Test steps:**
  1. Mock `SecurityContextService` and `notificationRepository.findByIdAndRecipientId`.
  2. Call `markRead()`.
- **Expected result:** Notification status is set to `READ` and `readAt` is populated. Returns updated response.
- **Automation target:** `NotificationServiceTest.should_MarkNotificationAsRead_when_ValidIdAndOwner()`

### TC-NOTIF-SVC-006: Archive - Success
- **Feature / module:** Notification (`NotificationService`)
- **Test type:** Unit Test
- **Tool:** Mockito/JUnit
- **Priority:** P0
- **Objective:** Ensure a user can archive their notification. If not previously read, `readAt` is also populated.
- **Preconditions:** Authenticated user, valid notification ID.
- **Input data:** Valid `notificationId`
- **Test steps:**
  1. Mock `SecurityContextService` and `notificationRepository.findByIdAndRecipientId`.
  2. Call `archive()`.
- **Expected result:** Notification status is set to `ARCHIVED` and `readAt` is populated. Returns updated response.
- **Automation target:** `NotificationServiceTest.should_ArchiveNotification_when_ValidIdAndOwner()`

### TC-NOTIF-SVC-007: Find Mine - Not Found or Forbidden
- **Feature / module:** Notification (`NotificationService`)
- **Test type:** Unit Test
- **Tool:** Mockito/JUnit
- **Priority:** P0
- **Objective:** Ensure `markRead` and `archive` reject operations on invalid IDs or notifications owned by other users.
- **Preconditions:** Authenticated user, invalid or foreign notification ID.
- **Input data:** Foreign `notificationId`
- **Test steps:** Mock `notificationRepository.findByIdAndRecipientId` to return empty.
- **Expected result:** Throws `BusinessException`.
- **Expected error code:** `ErrorCode.ERR_NOTIFICATION_001_NOT_FOUND`
- **Automation target:** `NotificationServiceTest.should_ThrowNotFound_when_NotificationIsMissingOrForeign()`

---

## B. EmailService

### TC-NOTIF-EML-001: Send Account Created Email - Success
- **Feature / module:** Notification (`EmailService`)
- **Test type:** Unit Test
- **Tool:** Mockito/JUnit
- **Priority:** P0
- **Objective:** Verify `JavaMailSender` is invoked correctly when sending an account creation email.
- **Preconditions:** Valid email and password.
- **Input data:** `toEmail = "test@example.com"`, `tempPassword = "pass"`
- **Test steps:**
  1. Mock `EmailTemplateService` to return subject and content.
  2. Mock `MailProperties.getFrom()` to return `"admin@example.com"`.
  3. Call `sendAccountCreatedEmail()`.
- **Expected result:** `mailSender.send(SimpleMailMessage)` is invoked exactly once with the correct recipient, subject, content, and sender.
- **Automation target:** `EmailServiceTest.should_SendAccountCreatedEmail_when_Called()`

### TC-NOTIF-EML-002: Send Temporary Password Email - Success
- **Feature / module:** Notification (`EmailService`)
- **Test type:** Unit Test
- **Tool:** Mockito/JUnit
- **Priority:** P0
- **Objective:** Verify `JavaMailSender` is invoked correctly when sending a temp password email.
- **Preconditions:** Valid email and password.
- **Test steps:** Similar to TC-NOTIF-EML-001.
- **Expected result:** `mailSender.send()` is invoked with correct parameters.
- **Automation target:** `EmailServiceTest.should_SendTemporaryPasswordEmail_when_Called()`

### TC-NOTIF-EML-003: Send OTP Email - Success
- **Feature / module:** Notification (`EmailService`)
- **Test type:** Unit Test
- **Tool:** Mockito/JUnit
- **Priority:** P0
- **Objective:** Verify OTP email is dispatched.
- **Preconditions:** Valid email with `@` sign.
- **Input data:** `toEmail = "user@example.com"`, `purpose = "Login"`, `otp = "123456"`
- **Test steps:** Call `sendOtpEmail()`.
- **Expected result:** `mailSender.send()` is invoked with correct OTP body.
- **Automation target:** `EmailServiceTest.should_SendOtpEmail_when_ValidEmailProvided()`

### TC-NOTIF-EML-004: Send OTP Email - Invalid Email Skips Execution
- **Feature / module:** Notification (`EmailService`)
- **Test type:** Unit Test
- **Tool:** Mockito/JUnit
- **Priority:** P1
- **Objective:** Ensure emails without an `@` sign (like phone numbers) do not trigger `JavaMailSender`.
- **Preconditions:** Input is a phone number.
- **Input data:** `toEmail = "0901234567"`
- **Test steps:** Call `sendOtpEmail()`.
- **Expected result:** `mailSender.send()` is NEVER invoked.
- **Automation target:** `EmailServiceTest.should_SkipSendingOtp_when_EmailIsInvalid()`

### TC-NOTIF-EML-005: MailException Handling
- **Feature / module:** Notification (`EmailService`)
- **Test type:** Unit Test
- **Tool:** Mockito/JUnit
- **Priority:** P0
- **Objective:** Verify that when `JavaMailSender` throws a `MailException`, the application catches it and does not crash the async thread.
- **Preconditions:** `mailSender` is configured to throw.
- **Input data:** Standard email inputs.
- **Test steps:**
  1. Mock `mailSender.send()` to throw a mock `MailException`.
  2. Call `sendAccountCreatedEmail()`.
- **Expected result:** No exception bubbles up out of the method. The error is handled internally.
- **Automation target:** `EmailServiceTest.should_HandleMailExceptionGracefully_when_MailSenderFails()`

---

## C. EmailTemplateService

### TC-NOTIF-TPL-001: Build Account Created Content
- **Feature / module:** Notification (`EmailTemplateService`)
- **Test type:** Unit Test
- **Tool:** Plain JUnit / Mockito
- **Priority:** P1
- **Objective:** Verify string formatting correctly embeds the app name, email, temp password, and team name.
- **Preconditions:** `MailProperties` returns known values.
- **Test steps:** Call `buildAccountCreatedContent()`.
- **Expected result:** The returned string contains exactly the passed email and temporary password, along with configured properties.
- **Automation target:** `EmailTemplateServiceTest.should_FormatAccountCreatedContentCorrectly()`

### TC-NOTIF-TPL-002: Build Temporary Password Content
- **Feature / module:** Notification (`EmailTemplateService`)
- **Test type:** Unit Test
- **Tool:** Plain JUnit / Mockito
- **Priority:** P1
- **Objective:** Verify string formatting for the temporary password email.
- **Preconditions:** `MailProperties` returns known values.
- **Test steps:** Call `buildTemporaryPasswordContent()`.
- **Expected result:** Correctly formatted content string.
- **Automation target:** `EmailTemplateServiceTest.should_FormatTemporaryPasswordContentCorrectly()`
