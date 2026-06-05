-- Seed demo lưu trú + nhật ký chăm sóc (UC008)
-- Chạy: Get-Content database/seeds/boarding_demo.sql -Raw | docker exec -i pccms-postgres psql -U pccms -d pccms

DO $$
DECLARE
    v_owner_id   UUID;
    v_pet_id     UUID;
    v_service_id UUID;
    v_room_type  UUID;
    v_staff_id   UUID;
    v_order_id   UUID := 'b1000001-0000-4000-8000-000000000001';
    v_booking_id UUID := 'b1000002-0000-4000-8000-000000000002';
    v_session_id UUID := 'b1000003-0000-4000-8000-000000000003';
BEGIN
    SELECT u.id INTO v_owner_id
    FROM users u
    JOIN roles r ON r.id = u.role_id
    WHERE r.code = 'OWNER' AND u.status_code = 'ACTIVE'
    ORDER BY u.created_at
    LIMIT 1;

    IF v_owner_id IS NULL THEN
        RAISE NOTICE 'boarding_demo: không tìm thấy chủ nuôi OWNER — bỏ qua seed';
        RETURN;
    END IF;

    SELECT p.id INTO v_pet_id
    FROM pets p
    WHERE p.owner_id = v_owner_id AND p.is_active = TRUE
    ORDER BY p.created_at
    LIMIT 1;

    IF v_pet_id IS NULL THEN
        RAISE NOTICE 'boarding_demo: chủ nuôi chưa có thú cưng active — bỏ qua seed';
        RETURN;
    END IF;

    SELECT id INTO v_service_id FROM service_catalog WHERE service_code = 'BRD-STAY' LIMIT 1;
    SELECT id INTO v_room_type FROM room_types WHERE code = 'STANDARD' LIMIT 1;

    SELECT u.id INTO v_staff_id
    FROM users u
    JOIN roles r ON r.id = u.role_id
    WHERE r.code IN ('STAFF', 'VETERINARIAN', 'ADMIN')
    LIMIT 1;

    IF v_staff_id IS NULL THEN
        v_staff_id := v_owner_id;
    END IF;

    INSERT INTO service_orders (
        id, order_code, owner_id, pet_id, service_id, category_code,
        status_code, base_amount_vnd, planned_start_at, planned_end_at
    ) VALUES (
        v_order_id,
        'SO-BOARD-DEMO-001',
        v_owner_id,
        v_pet_id,
        v_service_id,
        'BOARDING',
        'IN_PROGRESS',
        150000,
        NOW() - INTERVAL '2 days',
        NOW() + INTERVAL '3 days'
    ) ON CONFLICT (order_code) DO UPDATE SET updated_at = NOW();

    SELECT id INTO v_order_id FROM service_orders WHERE order_code = 'SO-BOARD-DEMO-001';

    INSERT INTO boarding_bookings (
        id, booking_code, service_order_id, owner_id, pet_id,
        requested_room_type_id, expected_checkin_at, expected_checkout_at,
        status_code, estimated_price_vnd
    ) VALUES (
        v_booking_id,
        'BRD-DEMO-001',
        v_order_id,
        v_owner_id,
        v_pet_id,
        v_room_type,
        NOW() - INTERVAL '2 days',
        NOW() + INTERVAL '3 days',
        'CHECKED_IN',
        450000
    ) ON CONFLICT (booking_code) DO UPDATE SET updated_at = NOW();

    SELECT id INTO v_booking_id FROM boarding_bookings WHERE booking_code = 'BRD-DEMO-001';

    INSERT INTO boarding_sessions (
        id, booking_id, actual_checkin_at, checked_in_by, status_code
    ) VALUES (
        v_session_id,
        v_booking_id,
        NOW() - INTERVAL '2 days',
        v_staff_id,
        'IN_STAY'
    ) ON CONFLICT (booking_id) DO UPDATE
        SET status_code = 'IN_STAY', updated_at = NOW();

    SELECT id INTO v_session_id FROM boarding_sessions WHERE booking_id = v_booking_id;

    INSERT INTO care_logs (
        session_id, pet_id, staff_id, log_date, period_code,
        feeding_status, hygiene_status, health_note, staff_note
    ) VALUES
        (v_session_id, v_pet_id, v_staff_id, CURRENT_DATE, 'MORNING',
         'Ăn tốt', 'Vệ sinh bình thường', NULL,
         'Bé thân thiện, ăn hết khẩu phần buổi sáng.'),
        (v_session_id, v_pet_id, v_staff_id, CURRENT_DATE, 'AFTERNOON',
         'Ăn vừa đủ', 'Đi dạo 15 phút', NULL,
         'Đã chơi với đồ chơi quen thuộc.'),
        (v_session_id, v_pet_id, v_staff_id, CURRENT_DATE - 1, 'MORNING',
         'Ăn hơi ít', 'Theo dõi thêm', 'Không sốt',
         'Nhân viên theo dõi thêm khẩu phần buổi sáng.')
    ON CONFLICT (session_id, log_date, period_code) DO NOTHING;

    RAISE NOTICE 'boarding_demo: seeded for owner % pet %', v_owner_id, v_pet_id;
END $$;
