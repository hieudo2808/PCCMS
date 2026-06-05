-- Sửa loài trùng (lỗi encoding) và bổ sung giống cho loài UTF-8 đúng
-- Chạy: Get-Content database/seeds/pet_catalog_fix.sql -Raw | docker exec -i pccms-postgres psql -U pccms -d pccms

INSERT INTO pet_breeds (species_id, name)
SELECT s.id, b.name
FROM pet_species s
JOIN (VALUES
  ('Chó', 'Husky'),
  ('Chó', 'Corgi'),
  ('Mèo', 'Anh lông ngắn'),
  ('Mèo', 'Ba Tư'),
  ('Mèo', 'Munchkin'),
  ('Thỏ', 'Thỏ mini'),
  ('Chim', 'Vẹt'),
  ('Chim', 'Yến phụng')
) AS b(species_name, name) ON s.name = b.species_name
ON CONFLICT (species_id, name) DO NOTHING;

DELETE FROM pet_breeds WHERE species_id IN (
  SELECT id FROM pet_species WHERE name LIKE '%?%'
);
DELETE FROM pet_species WHERE name LIKE '%?%';
