-- Seed danh mục thuốc (chạy khi DB đã có schema nhưng chưa có nhóm thuốc)
INSERT INTO medicine_categories (id, name, description) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-000000000001', 'Kháng sinh', 'Nhóm thuốc kháng sinh điều trị nhiễm khuẩn'),
('aaaaaaaa-aaaa-aaaa-aaaa-000000000002', 'Giảm đau - Hạ sốt', 'Thuốc giảm đau, hạ sốt cho thú cưng'),
('aaaaaaaa-aaaa-aaaa-aaaa-000000000003', 'Vitamin & Bổ sung', 'Vitamin và dưỡng chất bổ sung'),
('aaaaaaaa-aaaa-aaaa-aaaa-000000000004', 'Kháng viêm', 'Thuốc kháng viêm, chống dị ứng'),
('aaaaaaaa-aaaa-aaaa-aaaa-000000000005', 'Tiêu hóa', 'Thuốc hỗ trợ tiêu hóa')
ON CONFLICT DO NOTHING;

INSERT INTO medicines (medicine_code, name, category_id, unit, default_instruction, current_stock, unit_price_vnd) VALUES
('MED001', 'Amoxicillin 500mg', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000001', 'Viên', 'Ngày 2 lần sau ăn', 120, 5000),
('MED002', 'Meloxicam 1.5mg', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000002', 'Viên', 'Ngày 1 lần sau ăn', 80, 8000),
('MED003', 'Vitamin B Complex', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000003', 'Viên', 'Ngày 1 lần', 200, 3000),
('MED004', 'Prednisolone 5mg', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000004', 'Viên', 'Theo chỉ định bác sĩ', 60, 4500),
('MED005', 'Probiotics Pet', 'aaaaaaaa-aaaa-aaaa-aaaa-000000000005', 'Gói', 'Pha vào thức ăn ngày 1 lần', 45, 12000)
ON CONFLICT DO NOTHING;
