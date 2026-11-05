-- ==============================================================================
-- V13: Seed Complete Operational Workforce for Elephant Fence Monitoring
-- 1. Regional Admins for all 9 Provinces
-- 2. Field Admins for all 5 Districts with active fences
-- 3. Dedicated Maintenance Technicians assigned to each of the 5 Fences
-- Default Password for all accounts: Admin@123456
-- ==============================================================================

-- 1. Seed Remaining Regional Admins (Western, Central, Northern, North Western, Uva, Sabaragamuwa)
INSERT INTO users (id, full_name, email, password_hash, role, enabled, password_change_required, staff_id, contact_number) VALUES
    ('a0000000-0000-0000-0000-000000000011', 'Western Regional Admin',       'admin.western@nerdc.lk',  '$2a$10$i4jTHUntTL5gYMeA7Fvgfurnf5nThxeYWRxFz4Js1w2WZcdnrWzfa', 'REGIONAL_ADMIN', TRUE, FALSE, 'RA-WST-01', '+94112220001'),
    ('a0000000-0000-0000-0000-000000000012', 'Central Regional Admin',       'admin.central@nerdc.lk',  '$2a$10$i4jTHUntTL5gYMeA7Fvgfurnf5nThxeYWRxFz4Js1w2WZcdnrWzfa', 'REGIONAL_ADMIN', TRUE, FALSE, 'RA-CEN-01', '+94112220002'),
    ('a0000000-0000-0000-0000-000000000014', 'Northern Regional Admin',      'admin.northern@nerdc.lk', '$2a$10$i4jTHUntTL5gYMeA7Fvgfurnf5nThxeYWRxFz4Js1w2WZcdnrWzfa', 'REGIONAL_ADMIN', TRUE, FALSE, 'RA-NOR-01', '+94112220004'),
    ('a0000000-0000-0000-0000-000000000016', 'North Western Regional Admin', 'admin.nw@nerdc.lk',       '$2a$10$i4jTHUntTL5gYMeA7Fvgfurnf5nThxeYWRxFz4Js1w2WZcdnrWzfa', 'REGIONAL_ADMIN', TRUE, FALSE, 'RA-NW-01',  '+94112220006'),
    ('a0000000-0000-0000-0000-000000000018', 'Uva Regional Admin',           'admin.uva@nerdc.lk',      '$2a$10$i4jTHUntTL5gYMeA7Fvgfurnf5nThxeYWRxFz4Js1w2WZcdnrWzfa', 'REGIONAL_ADMIN', TRUE, FALSE, 'RA-UVA-01', '+94112220008'),
    ('a0000000-0000-0000-0000-000000000019', 'Sabaragamuwa Regional Admin', 'admin.sab@nerdc.lk',      '$2a$10$i4jTHUntTL5gYMeA7Fvgfurnf5nThxeYWRxFz4Js1w2WZcdnrWzfa', 'REGIONAL_ADMIN', TRUE, FALSE, 'RA-SAB-01', '+94112220009')
ON CONFLICT (id) DO UPDATE SET
    full_name = EXCLUDED.full_name,
    email = EXCLUDED.email,
    role = EXCLUDED.role;

-- Assign Provinces for the new Regional Admins
INSERT INTO user_provinces (user_id, province_id) VALUES
    ('a0000000-0000-0000-0000-000000000011', 1), -- Western
    ('a0000000-0000-0000-0000-000000000012', 2), -- Central
    ('a0000000-0000-0000-0000-000000000014', 4), -- Northern
    ('a0000000-0000-0000-0000-000000000016', 6), -- North Western
    ('a0000000-0000-0000-0000-000000000018', 8), -- Uva
    ('a0000000-0000-0000-0000-000000000019', 9)  -- Sabaragamuwa
ON CONFLICT (user_id, province_id) DO NOTHING;

-- 2. Seed Field Admins for Active Fence Districts (Anuradhapura, Polonnaruwa, Hambantota, Ampara, Trincomalee)
INSERT INTO users (id, full_name, email, password_hash, role, enabled, password_change_required, staff_id, contact_number) VALUES
    ('b0000000-0000-0000-0000-000000000020', 'Anuradhapura Field Admin', 'fa.anuradhapura@dwc.gov.lk', '$2a$10$i4jTHUntTL5gYMeA7Fvgfurnf5nThxeYWRxFz4Js1w2WZcdnrWzfa', 'FIELD_ADMIN', TRUE, FALSE, 'FA-ANU-01', '+94771234501'),
    ('b0000000-0000-0000-0000-000000000021', 'Polonnaruwa Field Admin',  'fa.polonnaruwa@dwc.gov.lk',  '$2a$10$i4jTHUntTL5gYMeA7Fvgfurnf5nThxeYWRxFz4Js1w2WZcdnrWzfa', 'FIELD_ADMIN', TRUE, FALSE, 'FA-POL-01', '+94771234502'),
    ('b0000000-0000-0000-0000-000000000009', 'Hambantota Field Admin',   'fa.hambantota@dwc.gov.lk',   '$2a$10$i4jTHUntTL5gYMeA7Fvgfurnf5nThxeYWRxFz4Js1w2WZcdnrWzfa', 'FIELD_ADMIN', TRUE, FALSE, 'FA-HAM-01', '+94771234503'),
    ('b0000000-0000-0000-0000-000000000016', 'Ampara Field Admin',        'fa.ampara@dwc.gov.lk',       '$2a$10$i4jTHUntTL5gYMeA7Fvgfurnf5nThxeYWRxFz4Js1w2WZcdnrWzfa', 'FIELD_ADMIN', TRUE, FALSE, 'FA-AMP-01', '+94771234504'),
    ('b0000000-0000-0000-0000-000000000017', 'Trincomalee Field Admin',   'fa.trincomalee@dwc.gov.lk',   '$2a$10$i4jTHUntTL5gYMeA7Fvgfurnf5nThxeYWRxFz4Js1w2WZcdnrWzfa', 'FIELD_ADMIN', TRUE, FALSE, 'FA-TRI-01', '+94771234505')
ON CONFLICT (id) DO UPDATE SET
    full_name = EXCLUDED.full_name,
    email = EXCLUDED.email,
    role = EXCLUDED.role;

-- Assign Provinces & Districts for Field Admins
INSERT INTO user_provinces (user_id, province_id) VALUES
    ('b0000000-0000-0000-0000-000000000020', 7), -- North Central
    ('b0000000-0000-0000-0000-000000000021', 7), -- North Central
    ('b0000000-0000-0000-0000-000000000009', 3), -- Southern
    ('b0000000-0000-0000-0000-000000000016', 5), -- Eastern
    ('b0000000-0000-0000-0000-000000000017', 5)  -- Eastern
ON CONFLICT (user_id, province_id) DO NOTHING;

INSERT INTO user_districts (user_id, district_id) VALUES
    ('b0000000-0000-0000-0000-000000000020', 20), -- Anuradhapura
    ('b0000000-0000-0000-0000-000000000021', 21), -- Polonnaruwa
    ('b0000000-0000-0000-0000-000000000009', 9),  -- Hambantota
    ('b0000000-0000-0000-0000-000000000016', 16), -- Ampara
    ('b0000000-0000-0000-0000-000000000017', 17)  -- Trincomalee
ON CONFLICT (user_id, district_id) DO NOTHING;

-- 3. Seed 5 Dedicated Maintenance Technicians (1 for each Fence)
INSERT INTO users (id, full_name, email, password_hash, role, enabled, password_change_required, staff_id, contact_number) VALUES
    ('c0000000-0000-0000-0000-000000000001', 'Wilpattu Maintenance Officer',    'tech.wilpattu@dwc.gov.lk',    '$2a$10$i4jTHUntTL5gYMeA7Fvgfurnf5nThxeYWRxFz4Js1w2WZcdnrWzfa', 'MAINTENANCE', TRUE, FALSE, 'MN-WIL-01', '+94711234501'),
    ('c0000000-0000-0000-0000-000000000002', 'Minneriya Maintenance Officer',   'tech.minneriya@dwc.gov.lk',   '$2a$10$i4jTHUntTL5gYMeA7Fvgfurnf5nThxeYWRxFz4Js1w2WZcdnrWzfa', 'MAINTENANCE', TRUE, FALSE, 'MN-MIN-01', '+94711234502'),
    ('c0000000-0000-0000-0000-000000000003', 'Bundala Maintenance Officer',     'tech.bundala@dwc.gov.lk',     '$2a$10$i4jTHUntTL5gYMeA7Fvgfurnf5nThxeYWRxFz4Js1w2WZcdnrWzfa', 'MAINTENANCE', TRUE, FALSE, 'MN-BUN-01', '+94711234503'),
    ('c0000000-0000-0000-0000-000000000004', 'Gal Oya Maintenance Officer',     'tech.galoya@dwc.gov.lk',      '$2a$10$i4jTHUntTL5gYMeA7Fvgfurnf5nThxeYWRxFz4Js1w2WZcdnrWzfa', 'MAINTENANCE', TRUE, FALSE, 'MN-GAL-01', '+94711234504'),
    ('c0000000-0000-0000-0000-000000000005', 'Somawathiya Maintenance Officer', 'tech.somawathiya@dwc.gov.lk', '$2a$10$i4jTHUntTL5gYMeA7Fvgfurnf5nThxeYWRxFz4Js1w2WZcdnrWzfa', 'MAINTENANCE', TRUE, FALSE, 'MN-SOM-01', '+94711234505')
ON CONFLICT (id) DO UPDATE SET
    full_name = EXCLUDED.full_name,
    email = EXCLUDED.email,
    role = EXCLUDED.role;

-- Assign Provinces & Districts for Maintenance Technicians
INSERT INTO user_provinces (user_id, province_id) VALUES
    ('c0000000-0000-0000-0000-000000000001', 7), -- North Central
    ('c0000000-0000-0000-0000-000000000002', 7), -- North Central
    ('c0000000-0000-0000-0000-000000000003', 3), -- Southern
    ('c0000000-0000-0000-0000-000000000004', 5), -- Eastern
    ('c0000000-0000-0000-0000-000000000005', 5)  -- Eastern
ON CONFLICT (user_id, province_id) DO NOTHING;

INSERT INTO user_districts (user_id, district_id) VALUES
    ('c0000000-0000-0000-0000-000000000001', 20), -- Anuradhapura
    ('c0000000-0000-0000-0000-000000000002', 21), -- Polonnaruwa
    ('c0000000-0000-0000-0000-000000000003', 9),  -- Hambantota
    ('c0000000-0000-0000-0000-000000000004', 16), -- Ampara
    ('c0000000-0000-0000-0000-000000000005', 17)  -- Trincomalee
ON CONFLICT (user_id, district_id) DO NOTHING;

-- 4. Assign Primary Maintenance Officers to each Fence
UPDATE fences SET primary_maintenance_user_id = 'c0000000-0000-0000-0000-000000000001' WHERE id = 1; -- Wilpattu
UPDATE fences SET primary_maintenance_user_id = 'c0000000-0000-0000-0000-000000000002' WHERE id = 2; -- Minneriya
UPDATE fences SET primary_maintenance_user_id = 'c0000000-0000-0000-0000-000000000003' WHERE id = 3; -- Bundala
UPDATE fences SET primary_maintenance_user_id = 'c0000000-0000-0000-0000-000000000004' WHERE id = 4; -- Gal Oya
UPDATE fences SET primary_maintenance_user_id = 'c0000000-0000-0000-0000-000000000005' WHERE id = 5; -- Somawathiya

-- 5. Link Backup Maintenance Contacts (Field Admin & Super Admin)
DELETE FROM fence_backup_maintenance_users;

INSERT INTO fence_backup_maintenance_users (fence_id, user_id) VALUES
    (1, 'b0000000-0000-0000-0000-000000000020'), -- Wilpattu: Anuradhapura Field Admin
    (1, 'a0000000-0000-0000-0000-000000000001'), -- Wilpattu: Super Admin
    (2, 'b0000000-0000-0000-0000-000000000021'), -- Minneriya: Polonnaruwa Field Admin
    (2, 'a0000000-0000-0000-0000-000000000001'), -- Minneriya: Super Admin
    (3, 'b0000000-0000-0000-0000-000000000009'), -- Bundala: Hambantota Field Admin
    (3, 'a0000000-0000-0000-0000-000000000001'), -- Bundala: Super Admin
    (4, 'b0000000-0000-0000-0000-000000000016'), -- Gal Oya: Ampara Field Admin
    (4, 'a0000000-0000-0000-0000-000000000001'), -- Gal Oya: Super Admin
    (5, 'b0000000-0000-0000-0000-000000000017'), -- Somawathiya: Trincomalee Field Admin
    (5, 'a0000000-0000-0000-0000-000000000001'); -- Somawathiya: Super Admin
