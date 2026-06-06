-- Reset mật khẩu tài khoản demo (tương thích PepperBCryptEncoder + security.pepper trong application.yml)
-- Hash sinh bởi: mvn test -Dtest=DemoPasswordHashGeneratorTest#printDemoPasswordHashes
-- Pepper mặc định: local-dev-pepper-change-me
--
-- PowerShell:
--   Get-Content database/seeds/reset_demo_passwords.sql -Encoding UTF8 -Raw | docker exec -i pccms-postgres psql -U pccms -d pccms

UPDATE users SET password_hash = '$2a$12$zIt5jcuuOaQ0z6TOSCa75.1gnmyIyRXIk6C0dafRg7Lyp9frSwtAG'
WHERE email = 'admin@pccms.vn';

UPDATE users SET password_hash = '$2a$12$Z6BZw6r4DoeSk1kgYGg2f.vKeJJf4Kkp45SgsQst4/sI/WFJhN.6G'
WHERE email = 'staff.le@pccms.vn';

UPDATE users SET password_hash = '$2a$12$j9FJLOCWKwWcGgGPlLJTmOya19PJ5cgVo13bLa1tD8iMs7G8AY36O'
WHERE email = 'vet.an@pccms.vn';

UPDATE users SET password_hash = '$2a$12$RmYQXJEDomQ6zs5x/aZe1.j.gk2.HxbT97BehrGXHU4ha1BIMZJVe'
WHERE email = 'vet.huong@pccms.vn';

-- Chủ nuôi demo cố định (đặt lịch / xem lịch hẹn)
INSERT INTO users (id, email, phone, password_hash, full_name, role_id, status_code)
VALUES (
    '33333333-3333-3333-3333-000000000001',
    'owner@pccms.vn',
    '0913000001',
    '$2a$12$/oD6uMQ7UGyRk63SFNWLzuX6O7diFrKQ2ddkC2kaKCFVl6g3bdh9C',
    'Chủ nuôi Demo',
    (SELECT id FROM roles WHERE code = 'OWNER'),
    'ACTIVE'
)
ON CONFLICT (email) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    phone = EXCLUDED.phone,
    full_name = EXCLUDED.full_name,
    role_id = EXCLUDED.role_id,
    status_code = EXCLUDED.status_code;

-- Reset mật khẩu tài khoản đăng ký thật (nếu có) để test local
UPDATE users SET password_hash = '$2a$12$/oD6uMQ7UGyRk63SFNWLzuX6O7diFrKQ2ddkC2kaKCFVl6g3bdh9C'
WHERE email = 'hoangvanthang.work@gmail.com';

-- Thú cưng cho owner@pccms.vn (nếu chưa có)
INSERT INTO pets (id, owner_id, name, species_id, breed_id, sex, estimated_age_months, weight_kg, is_active)
SELECT
    '44444444-4444-4444-4444-000000000001'::uuid,
    u.id,
    'Lucky',
    sp.id,
    br.id,
    'MALE',
    24,
    5.5,
    TRUE
FROM users u
JOIN pet_species sp ON sp.name = 'Chó'
JOIN pet_breeds br ON br.species_id = sp.id AND br.name = 'Poodle'
WHERE u.email = 'owner@pccms.vn'
  AND NOT EXISTS (SELECT 1 FROM pets p WHERE p.id = '44444444-4444-4444-4444-000000000001'::uuid);
