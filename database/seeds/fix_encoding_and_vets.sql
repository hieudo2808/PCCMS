-- Sửa dữ liệu UTF-8 bị hỏng, bổ sung bác sĩ và tài khoản nhân viên (chạy trên DB đã khởi tạo)
-- PowerShell: Get-Content database/seeds/fix_encoding_and_vets.sql -Encoding UTF8 -Raw | docker exec -i pccms-postgres psql -U pccms -d pccms

-- Sửa tên UTF-8 qua hex (tránh PowerShell/docker pipe làm hỏng dấu tiếng Việt)
UPDATE users SET full_name = convert_from(decode('5472E1BAA76E2056C4836E20416E', 'hex'), 'UTF8')
WHERE email = 'vet.an@pccms.vn';
UPDATE users SET full_name = convert_from(decode('4CC3AA205468E1BB8B2048C6B0C6A16E67', 'hex'), 'UTF8')
WHERE email = 'vet.huong@pccms.vn';

DELETE FROM pet_breeds WHERE name LIKE '%?%';

UPDATE care_logs SET
    feeding_status = 'Ăn tốt',
    hygiene_status = 'Vệ sinh bình thường',
    staff_note = 'Bé thân thiện, ăn hết khẩu phần buổi sáng.'
WHERE period_code = 'MORNING' AND log_date = CURRENT_DATE AND feeding_status LIKE '%?%';

UPDATE care_logs SET
    feeding_status = 'Ăn vừa đủ',
    hygiene_status = 'Đi dạo 15 phút',
    staff_note = 'Đã chơi với đồ chơi quen thuộc.'
WHERE period_code = 'AFTERNOON' AND log_date = CURRENT_DATE AND feeding_status LIKE '%?%';

UPDATE care_logs SET
    feeding_status = 'Ăn hơi ít',
    hygiene_status = 'Theo dõi thêm',
    health_note = 'Không sốt',
    staff_note = 'Nhân viên theo dõi thêm khẩu phần buổi sáng.'
WHERE period_code = 'MORNING' AND log_date = CURRENT_DATE - 1 AND feeding_status LIKE '%?%';

INSERT INTO users (id, email, phone, password_hash, full_name, role_id, status_code)
VALUES
    ('11111111-1111-1111-1111-000000000001', 'vet.an@pccms.vn', '0901000001',
     crypt('vet123', gen_salt('bf')), 'Trần Văn An',
     (SELECT id FROM roles WHERE code = 'VETERINARIAN'), 'ACTIVE'),
    ('11111111-1111-1111-1111-000000000002', 'vet.huong@pccms.vn', '0901000002',
     crypt('vet123', gen_salt('bf')), 'Lê Thị Hương',
     (SELECT id FROM roles WHERE code = 'VETERINARIAN'), 'ACTIVE')
ON CONFLICT (email) DO NOTHING;

INSERT INTO staff_profiles (user_id, staff_code, professional_title, is_service_provider)
SELECT u.id, v.staff_code, 'Bác sĩ thú y', TRUE
FROM (VALUES
    ('11111111-1111-1111-1111-000000000001'::uuid, 'VET-001'),
    ('11111111-1111-1111-1111-000000000002'::uuid, 'VET-002')
) AS v(user_id, staff_code)
JOIN users u ON u.id = v.user_id
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO work_schedules (staff_id, work_date, shift_id, exam_room_id, capacity, status_code)
SELECT v.staff_id, d.work_date::date, sh.id, er.id, 4, 'ASSIGNED'
FROM (VALUES
    ('11111111-1111-1111-1111-000000000001'::uuid),
    ('11111111-1111-1111-1111-000000000002'::uuid)
) AS v(staff_id)
CROSS JOIN generate_series(CURRENT_DATE, CURRENT_DATE + 60, INTERVAL '1 day') AS d(work_date)
JOIN shifts sh ON sh.code IN ('MORNING', 'AFTERNOON')
CROSS JOIN LATERAL (SELECT id FROM exam_rooms WHERE room_code = 'PKB-01' LIMIT 1) er
ON CONFLICT (staff_id, work_date, shift_id) DO NOTHING;

-- Tài khoản nhân viên lễ tân (tiếp nhận lịch hẹn / walk-in)
INSERT INTO users (id, email, phone, password_hash, full_name, role_id, status_code)
VALUES (
    '22222222-2222-2222-2222-000000000001',
    'staff.le@pccms.vn',
    '0902000001',
    crypt('staff123', gen_salt('bf')),
    convert_from(decode('4E677579E1BB856E205468E1BB8B204CE1BB87', 'hex'), 'UTF8'),
    (SELECT id FROM roles WHERE code = 'STAFF'),
    'ACTIVE'
)
ON CONFLICT (email) DO UPDATE SET
    full_name = EXCLUDED.full_name,
    phone = EXCLUDED.phone,
    role_id = EXCLUDED.role_id,
    status_code = EXCLUDED.status_code;
-- Mật khẩu: chạy database/seeds/reset_demo_passwords.sql (PepperBCryptEncoder, không dùng crypt() thuần)

INSERT INTO staff_profiles (user_id, staff_code, professional_title, is_service_provider)
VALUES (
    '22222222-2222-2222-2222-000000000001',
    'STAFF-001',
    convert_from(decode('4E68C3A26E7669C3AA6E207469E1BABF706E68E1BAAD6E', 'hex'), 'UTF8'),
    FALSE
)
ON CONFLICT (user_id) DO UPDATE SET
    professional_title = EXCLUDED.professional_title;

-- SĐT chủ nuôi demo (tra cứu walk-in tại quầy lễ tân)
UPDATE users SET phone = '0913123321'
WHERE email = 'hoangvanthang.work@gmail.com' AND (phone IS NULL OR phone = '');
