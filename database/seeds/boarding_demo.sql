-- Seed demo lưu trú + nhật ký chăm sóc (UC008)
-- PowerShell (bắt buộc -Encoding UTF8): Get-Content database/seeds/boarding_demo.sql -Encoding UTF8 -Raw | docker exec -i pccms-postgres psql -U pccms -d pccms

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
    v_feed_good  TEXT := convert_from(decode('C4826E2074E1BB9174', 'hex'), 'UTF8');
    v_hyg_normal TEXT := convert_from(decode('56E1BB872073696E682062C3AC6E68207468C6B0E1BB9D6E67', 'hex'), 'UTF8');
    v_note_morn  TEXT := convert_from(decode('42C3A9207468C3A26E20746869E1BB876E2C20C4836E2068E1BABF74206B68E1BAA975207068E1BAA76E206275E1BB95692073C3A16E672E', 'hex'), 'UTF8');
    v_feed_mid   TEXT := convert_from(decode('C4826E2076E1BBAB6120C491E1BBA7', 'hex'), 'UTF8');
    v_hyg_walk   TEXT := convert_from(decode('C490692064E1BAA16F203135207068C3BA74', 'hex'), 'UTF8');
    v_note_aft   TEXT := convert_from(decode('C490C3A3206368C6A1692076E1BB9B6920C491E1BB93206368C6A169207175656E20746875E1BB99632E', 'hex'), 'UTF8');
    v_feed_low   TEXT := convert_from(decode('C4826E2068C6A16920C3AD74', 'hex'), 'UTF8');
    v_hyg_watch  TEXT := convert_from(decode('5468656F2064C3B569207468C3AA6D', 'hex'), 'UTF8');
    v_health_ok  TEXT := convert_from(decode('4B68C3B46E672073E1BB9174', 'hex'), 'UTF8');
    v_note_watch TEXT := convert_from(decode('4E68C3A26E207669C3AA6E207468656F2064C3B569207468C3AA6D206B68E1BAA975207068E1BAA76E206275E1BB95692073C3A16E672E', 'hex'), 'UTF8');
BEGIN
    SELECT u.id INTO v_owner_id
    FROM users u
    JOIN roles r ON r.id = u.role_id
    WHERE r.code = 'OWNER' AND u.status_code = 'ACTIVE'
    ORDER BY u.created_at
    LIMIT 1;

    IF v_owner_id IS NULL THEN
        RAISE NOTICE 'boarding_demo: khong tim thay OWNER';
        RETURN;
    END IF;

    SELECT p.id INTO v_pet_id
    FROM pets p
    WHERE p.owner_id = v_owner_id AND p.is_active = TRUE
    ORDER BY p.created_at
    LIMIT 1;

    IF v_pet_id IS NULL THEN
        RAISE NOTICE 'boarding_demo: chua co pet active';
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
         v_feed_good, v_hyg_normal, NULL, v_note_morn),
        (v_session_id, v_pet_id, v_staff_id, CURRENT_DATE, 'AFTERNOON',
         v_feed_mid, v_hyg_walk, NULL, v_note_aft),
        (v_session_id, v_pet_id, v_staff_id, CURRENT_DATE - 1, 'MORNING',
         v_feed_low, v_hyg_watch, v_health_ok, v_note_watch)
    ON CONFLICT (session_id, log_date, period_code) DO UPDATE SET
        feeding_status = EXCLUDED.feeding_status,
        hygiene_status = EXCLUDED.hygiene_status,
        health_note = EXCLUDED.health_note,
        staff_note = EXCLUDED.staff_note,
        updated_at = NOW();

    DELETE FROM care_logs cl
    WHERE cl.session_id = v_session_id
      AND NOT (
          (cl.log_date = CURRENT_DATE AND cl.period_code IN ('MORNING', 'AFTERNOON'))
          OR (cl.log_date = CURRENT_DATE - 1 AND cl.period_code = 'MORNING')
      );

    RAISE NOTICE 'boarding_demo: seeded owner % pet %', v_owner_id, v_pet_id;
END $$;
