-- Demo veterinarians and work schedules for appointment slot availability.
-- Run after schema init when testing appointment flows locally.

DO $$
DECLARE
    v_vet_role_id UUID;
    v_morning_shift_id UUID;
    v_vet1_id UUID := '11111111-1111-1111-1111-000000000001';
    v_vet2_id UUID := '11111111-1111-1111-1111-000000000002';
    v_room1_id UUID;
BEGIN
    SELECT id INTO v_vet_role_id FROM roles WHERE code = 'VETERINARIAN';
    SELECT id INTO v_morning_shift_id FROM shifts WHERE code = 'MORNING';
    SELECT id INTO v_room1_id FROM exam_rooms WHERE room_code = 'PKB-01';

    INSERT INTO users (id, email, phone, password_hash, full_name, role_id, status_code)
    VALUES
        (v_vet1_id, 'vet.an@pccms.vn', '0901000001', crypt('vet123', gen_salt('bf')), 'Trần Văn An', v_vet_role_id, 'ACTIVE'),
        (v_vet2_id, 'vet.huong@pccms.vn', '0901000002', crypt('vet123', gen_salt('bf')), 'Lê Thị Hương', v_vet_role_id, 'ACTIVE')
    ON CONFLICT (email) DO NOTHING;

    INSERT INTO staff_profiles (user_id, staff_code, professional_title, is_service_provider)
    VALUES
        (v_vet1_id, 'VET-001', 'Bác sĩ thú y', TRUE),
        (v_vet2_id, 'VET-002', 'Bác sĩ thú y', TRUE)
    ON CONFLICT (user_id) DO NOTHING;

    INSERT INTO work_schedules (staff_id, work_date, shift_id, exam_room_id, capacity, status_code)
    SELECT v.staff_id, d.work_date, v_morning_shift_id, v_room1_id, 4, 'ASSIGNED'
    FROM (VALUES (v_vet1_id), (v_vet2_id)) AS v(staff_id)
    CROSS JOIN generate_series(CURRENT_DATE, CURRENT_DATE + 30, INTERVAL '1 day') AS d(work_date)
    ON CONFLICT (staff_id, work_date, shift_id) DO NOTHING;
END $$;
