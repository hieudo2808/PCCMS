-- Seed loài / giống thú cưng (chạy một lần nếu dropdown trống)
-- psql -U pccms -d pccms -f database/seeds/pet_catalog.sql

INSERT INTO pet_species (name) VALUES
    ('Chó'), ('Mèo'), ('Thỏ'), ('Chim')
ON CONFLICT (name) DO NOTHING;

INSERT INTO pet_breeds (species_id, name)
SELECT s.id, b.name
FROM pet_species s
JOIN (VALUES
    ('Chó', 'Poodle'),
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
