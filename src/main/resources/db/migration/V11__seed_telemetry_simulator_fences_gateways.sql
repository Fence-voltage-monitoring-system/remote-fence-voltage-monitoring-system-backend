-- ==============================================================================
-- V11: Clean and Seed Telemetry Simulator Topology & Role-Based Access Control
-- Seeds Provinces, Districts, Users, Gateways, Fences, Sections, and Devices
-- ==============================================================================

-- 1. Ensure Sri Lanka Provinces exist
INSERT INTO provinces (id, name) VALUES
    (1, 'Western'),
    (2, 'Central'),
    (3, 'Southern'),
    (4, 'Northern'),
    (5, 'Eastern'),
    (6, 'North Western'),
    (7, 'North Central'),
    (8, 'Uva'),
    (9, 'Sabaragamuwa')
ON CONFLICT (id) DO NOTHING;

-- 2. Ensure Sri Lanka Districts exist
INSERT INTO districts (id, province_id, name) VALUES
    (1, 1, 'Colombo'),
    (2, 1, 'Gampaha'),
    (3, 1, 'Kalutara'),
    (4, 2, 'Kandy'),
    (5, 2, 'Matale'),
    (6, 2, 'Nuwara Eliya'),
    (7, 3, 'Galle'),
    (8, 3, 'Matara'),
    (9, 3, 'Hambantota'),
    (10, 4, 'Jaffna'),
    (11, 4, 'Kilinochchi'),
    (12, 4, 'Mannar'),
    (13, 4, 'Vavuniya'),
    (14, 4, 'Mullaitivu'),
    (15, 5, 'Batticaloa'),
    (16, 5, 'Ampara'),
    (17, 5, 'Trincomalee'),
    (18, 6, 'Kurunegala'),
    (19, 6, 'Puttalam'),
    (20, 7, 'Anuradhapura'),
    (21, 7, 'Polonnaruwa'),
    (22, 8, 'Badulla'),
    (23, 8, 'Monaragala'),
    (24, 9, 'Ratnapura'),
    (25, 9, 'Kegalle')
ON CONFLICT (id) DO NOTHING;

-- ==============================================================================
-- 3. Seed Default System Users (Password: Admin@123456)
-- ==============================================================================
-- Safely clean up any existing users with these emails to avoid UUID/email collision
DELETE FROM user_provinces WHERE user_id IN (SELECT id FROM users WHERE email IN ('admin@nerdc.lk', 'admin.nc@nerdc.lk', 'admin.south@nerdc.lk', 'admin.east@nerdc.lk', 'tech.maintenance@nerdc.lk'));
DELETE FROM user_districts WHERE user_id IN (SELECT id FROM users WHERE email IN ('admin@nerdc.lk', 'admin.nc@nerdc.lk', 'admin.south@nerdc.lk', 'admin.east@nerdc.lk', 'tech.maintenance@nerdc.lk'));
DELETE FROM users WHERE email IN ('admin@nerdc.lk', 'admin.nc@nerdc.lk', 'admin.south@nerdc.lk', 'admin.east@nerdc.lk', 'tech.maintenance@nerdc.lk');

INSERT INTO users (id, full_name, email, password_hash, role, enabled, password_change_required) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'National Super Admin', 'admin@nerdc.lk', '$2a$10$i4jTHUntTL5gYMeA7Fvgfurnf5nThxeYWRxFz4Js1w2WZcdnrWzfa', 'SUPER_ADMIN', TRUE, FALSE),
    ('a0000000-0000-0000-0000-000000000002', 'North Central Regional Admin', 'admin.nc@nerdc.lk', '$2a$10$i4jTHUntTL5gYMeA7Fvgfurnf5nThxeYWRxFz4Js1w2WZcdnrWzfa', 'REGIONAL_ADMIN', TRUE, FALSE),
    ('a0000000-0000-0000-0000-000000000003', 'Southern Regional Admin', 'admin.south@nerdc.lk', '$2a$10$i4jTHUntTL5gYMeA7Fvgfurnf5nThxeYWRxFz4Js1w2WZcdnrWzfa', 'REGIONAL_ADMIN', TRUE, FALSE),
    ('a0000000-0000-0000-0000-000000000004', 'Eastern Regional Admin', 'admin.east@nerdc.lk', '$2a$10$i4jTHUntTL5gYMeA7Fvgfurnf5nThxeYWRxFz4Js1w2WZcdnrWzfa', 'REGIONAL_ADMIN', TRUE, FALSE),
    ('a0000000-0000-0000-0000-000000000005', 'Chief Wildlife Maintenance Officer', 'tech.maintenance@nerdc.lk', '$2a$10$i4jTHUntTL5gYMeA7Fvgfurnf5nThxeYWRxFz4Js1w2WZcdnrWzfa', 'MAINTENANCE', TRUE, FALSE);

-- Assign Admin Scopes
INSERT INTO user_provinces (user_id, province_id) VALUES
    ('a0000000-0000-0000-0000-000000000002', 7), -- North Central
    ('a0000000-0000-0000-0000-000000000003', 3), -- Southern
    ('a0000000-0000-0000-0000-000000000004', 5)  -- Eastern
ON CONFLICT (user_id, province_id) DO NOTHING;

-- 4. Clear out old operational and telemetry data
DELETE FROM user_notifications;
DELETE FROM alert_events;
DELETE FROM alert_comments;
DELETE FROM alerts;
DELETE FROM telemetry_readings;
DELETE FROM gateway_fences;
DELETE FROM fence_backup_maintenance_users;
DELETE FROM devices;
DELETE FROM sections;
DELETE FROM fences;
DELETE FROM gateways;

-- Reset identity sequences
ALTER TABLE gateways ALTER COLUMN id RESTART WITH 1;
ALTER TABLE fences ALTER COLUMN id RESTART WITH 1;
ALTER TABLE sections ALTER COLUMN id RESTART WITH 1;
ALTER TABLE devices ALTER COLUMN id RESTART WITH 1;
ALTER TABLE telemetry_readings ALTER COLUMN id RESTART WITH 1;

-- Add gateway latitude and longitude columns if missing
ALTER TABLE gateways ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE gateways ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;

-- ==============================================================================
-- 5. Seed 5 Regional Gateways
-- ==============================================================================
INSERT INTO gateways (id, name, serial, imei, status, signal, power, firmware, enabled, latitude, longitude) VALUES
    (1, 'Anuradhapura North Hub', 'GW-ANURADHAPURA-01', '864201040000001', 'online', 92, 98, 'v2.4.1', TRUE, 8.4520, 79.9820),
    (2, 'Polonnaruwa South Hub',  'GW-POLONNARUWA-02',  '864201040000002', 'online', 88, 96, 'v2.4.1', TRUE, 8.0250, 80.8850),
    (3, 'Hambantota Southern Hub', 'GW-HAMBANTOTA-03',   '864201040000003', 'online', 90, 97, 'v2.4.1', TRUE, 6.1850, 81.1950),
    (4, 'Ampara Eastern Hub',      'GW-AMPARA-04',       '864201040000004', 'online', 85, 95, 'v2.4.1', TRUE, 7.2150, 81.5250),
    (5, 'Trincomalee North-East Hub', 'GW-TRINCOMALEE-05','864201040000005', 'online', 89, 94, 'v2.4.1', TRUE, 8.1950, 81.1850);

-- ==============================================================================
-- 6. Seed 5 National Fences (Assigned to Gateways & Maintenance Admins)
-- ==============================================================================
INSERT INTO fences (id, code, name, province_id, district_id, length_km, gateway_id, average_voltage_kv, health, primary_maintenance_user_id) VALUES
    (1, 'FC-WILPATPU-01', 'Wilpattu Border Protection Fence',   7, 20, 15.50, 1, 6.80, 'HEALTHY', 'a0000000-0000-0000-0000-000000000005'),
    (2, 'FC-MINNERIYA-01','Minneriya Elephant Corridor Fence',  7, 21, 12.00, 2, 6.70, 'HEALTHY', 'a0000000-0000-0000-0000-000000000005'),
    (3, 'FC-BUNDALA-01',  'Bundala Wildlife Reserve Fence',     3, 9,  10.20, 3, 6.80, 'HEALTHY', 'a0000000-0000-0000-0000-000000000005'),
    (4, 'FC-GALOYA-01',   'Gal Oya Valley Protection Fence',    5, 16, 14.80, 4, 6.60, 'HEALTHY', 'a0000000-0000-0000-0000-000000000005'),
    (5, 'FC-SOMAWATH-01', 'Somawathiya Border Corridor Fence',  5, 17, 11.50, 5, 6.70, 'HEALTHY', 'a0000000-0000-0000-0000-000000000005');

-- Link Gateway to Fences
INSERT INTO gateway_fences (gateway_id, fence_id) VALUES
    (1, 1),
    (2, 2),
    (3, 3),
    (4, 4),
    (5, 5);

-- Link Backup Maintenance Users
INSERT INTO fence_backup_maintenance_users (fence_id, user_id) VALUES
    (1, 'a0000000-0000-0000-0000-000000000001'),
    (2, 'a0000000-0000-0000-0000-000000000001'),
    (3, 'a0000000-0000-0000-0000-000000000001'),
    (4, 'a0000000-0000-0000-0000-000000000001'),
    (5, 'a0000000-0000-0000-0000-000000000001');

-- ==============================================================================
-- 7. Seed 25 Fence Sections (5 per fence) with GPS coordinates
-- ==============================================================================
-- Fence 1: Wilpattu Border Protection Fence
INSERT INTO sections (id, fence_id, code, start_gps, end_gps, length_km, voltage_kv, battery, status, province_id, district_id) VALUES
    (1, 1, 'SEC-01', '8.4520,79.9820', '8.4610,79.9910', 3.10, 7.20, 95, 'HEALTHY', 7, 20),
    (2, 1, 'SEC-02', '8.4610,79.9910', '8.4720,80.0030', 3.20, 6.80, 96, 'HEALTHY', 7, 20),
    (3, 1, 'SEC-03', '8.4720,80.0030', '8.4850,80.0150', 3.00, 6.60, 94, 'HEALTHY', 7, 20),
    (4, 1, 'SEC-04', '8.4850,80.0150', '8.4980,80.0280', 3.10, 6.40, 97, 'HEALTHY', 7, 20),
    (5, 1, 'SEC-05', '8.4980,80.0280', '8.5100,80.0400', 3.10, 6.70, 95, 'HEALTHY', 7, 20);

-- Fence 2: Minneriya Elephant Corridor Fence
INSERT INTO sections (id, fence_id, code, start_gps, end_gps, length_km, voltage_kv, battery, status, province_id, district_id) VALUES
    (6, 2, 'SEC-01', '8.0250,80.8850', '8.0340,80.8960', 2.40, 7.00, 98, 'HEALTHY', 7, 21),
    (7, 2, 'SEC-02', '8.0340,80.8960', '8.0450,80.9080', 2.50, 6.70, 95, 'HEALTHY', 7, 21),
    (8, 2, 'SEC-03', '8.0450,80.9080', '8.0560,80.9200', 2.30, 6.50, 96, 'HEALTHY', 7, 21),
    (9, 2, 'SEC-04', '8.0560,80.9200', '8.0670,80.9310', 2.40, 6.80, 94, 'HEALTHY', 7, 21),
    (10, 2, 'SEC-05', '8.0670,80.9310', '8.0780,80.9420', 2.40, 6.40, 95, 'HEALTHY', 7, 21);

-- Fence 3: Bundala Wildlife Reserve Fence
INSERT INTO sections (id, fence_id, code, start_gps, end_gps, length_km, voltage_kv, battery, status, province_id, district_id) VALUES
    (11, 3, 'SEC-01', '6.1850,81.1950', '6.1940,81.2060', 2.00, 7.10, 97, 'HEALTHY', 3, 9),
    (12, 3, 'SEC-02', '6.1940,81.2060', '6.2050,81.2180', 2.10, 6.80, 96, 'HEALTHY', 3, 9),
    (13, 3, 'SEC-03', '6.2050,81.2180', '6.2160,81.2300', 2.00, 6.60, 95, 'HEALTHY', 3, 9),
    (14, 3, 'SEC-04', '6.2160,81.2300', '6.2270,81.2410', 2.10, 6.50, 96, 'HEALTHY', 3, 9),
    (15, 3, 'SEC-05', '6.2270,81.2410', '6.2380,81.2520', 2.00, 6.70, 98, 'HEALTHY', 3, 9);

-- Fence 4: Gal Oya Valley Protection Fence
INSERT INTO sections (id, fence_id, code, start_gps, end_gps, length_km, voltage_kv, battery, status, province_id, district_id) VALUES
    (16, 4, 'SEC-01', '7.2150,81.5250', '7.2260,81.5380', 3.00, 6.90, 96, 'HEALTHY', 5, 16),
    (17, 4, 'SEC-02', '7.2260,81.5380', '7.2380,81.5510', 2.90, 6.70, 95, 'HEALTHY', 5, 16),
    (18, 4, 'SEC-03', '7.2380,81.5510', '7.2490,81.5630', 3.00, 6.50, 94, 'HEALTHY', 5, 16),
    (19, 4, 'SEC-04', '7.2490,81.5630', '7.2600,81.5750', 2.90, 6.60, 97, 'HEALTHY', 5, 16),
    (20, 4, 'SEC-05', '7.2600,81.5750', '7.2710,81.5870', 3.00, 6.30, 93, 'HEALTHY', 5, 16);

-- Fence 5: Somawathiya Border Corridor Fence
INSERT INTO sections (id, fence_id, code, start_gps, end_gps, length_km, voltage_kv, battery, status, province_id, district_id) VALUES
    (21, 5, 'SEC-01', '8.1950,81.1850', '8.2060,81.1980', 2.30, 7.30, 98, 'HEALTHY', 5, 17),
    (22, 5, 'SEC-02', '8.2060,81.1980', '8.2180,81.2110', 2.30, 6.80, 95, 'HEALTHY', 5, 17),
    (23, 5, 'SEC-03', '8.2180,81.2110', '8.2290,81.2230', 2.30, 6.40, 96, 'HEALTHY', 5, 17),
    (24, 5, 'SEC-04', '8.2290,81.2230', '8.2400,81.2350', 2.30, 6.60, 94, 'HEALTHY', 5, 17),
    (25, 5, 'SEC-05', '8.2400,81.2350', '8.2510,81.2470', 2.30, 6.50, 95, 'HEALTHY', 5, 17);

-- ==============================================================================
-- 8. Seed 25 IoT Voltage Monitoring Devices (Linked to Gateways & Sections)
-- ==============================================================================
-- Gateway 1 (Wilpattu) Devices
INSERT INTO devices (id, gateway_id, fence_id, section_id, name, serial, type, status, voltage, signal, battery, enabled) VALUES
    (1, 1, 1, 1, 'Wilpattu Node 01', 'SN-12345',   'Voltage Monitor', 'online', 7.20, 90, 95, TRUE),
    (2, 1, 1, 2, 'Wilpattu Node 02', 'SN-E-01001', 'Voltage Monitor', 'online', 6.80, 88, 96, TRUE),
    (3, 1, 1, 3, 'Wilpattu Node 03', 'SN-E-01002', 'Voltage Monitor', 'online', 6.60, 85, 94, TRUE),
    (4, 1, 1, 4, 'Wilpattu Node 04', 'SN-E-01003', 'Voltage Monitor', 'online', 6.40, 92, 97, TRUE),
    (5, 1, 1, 5, 'Wilpattu Node 05', 'SN-E-01004', 'Voltage Monitor', 'online', 6.70, 89, 95, TRUE);

-- Gateway 2 (Minneriya) Devices
INSERT INTO devices (id, gateway_id, fence_id, section_id, name, serial, type, status, voltage, signal, battery, enabled) VALUES
    (6,  2, 2, 6,  'Minneriya Node 01', 'SN-E-02001', 'Voltage Monitor', 'online', 7.00, 94, 98, TRUE),
    (7,  2, 2, 7,  'Minneriya Node 02', 'SN-E-02002', 'Voltage Monitor', 'online', 6.70, 89, 95, TRUE),
    (8,  2, 2, 8,  'Minneriya Node 03', 'SN-E-02003', 'Voltage Monitor', 'online', 6.50, 87, 96, TRUE),
    (9,  2, 2, 9,  'Minneriya Node 04', 'SN-E-02004', 'Voltage Monitor', 'online', 6.80, 91, 94, TRUE),
    (10, 2, 2, 10, 'Minneriya Node 05', 'SN-E-02005', 'Voltage Monitor', 'online', 6.40, 88, 95, TRUE);

-- Gateway 3 (Bundala) Devices
INSERT INTO devices (id, gateway_id, fence_id, section_id, name, serial, type, status, voltage, signal, battery, enabled) VALUES
    (11, 3, 3, 11, 'Bundala Node 01', 'SN-E-03001', 'Voltage Monitor', 'online', 7.10, 92, 97, TRUE),
    (12, 3, 3, 12, 'Bundala Node 02', 'SN-E-03002', 'Voltage Monitor', 'online', 6.80, 89, 96, TRUE),
    (13, 3, 3, 13, 'Bundala Node 03', 'SN-E-03003', 'Voltage Monitor', 'online', 6.60, 85, 95, TRUE),
    (14, 3, 3, 14, 'Bundala Node 04', 'SN-E-03004', 'Voltage Monitor', 'online', 6.50, 88, 96, TRUE),
    (15, 3, 3, 15, 'Bundala Node 05', 'SN-E-03005', 'Voltage Monitor', 'online', 6.70, 91, 98, TRUE);

-- Gateway 4 (Gal Oya) Devices
INSERT INTO devices (id, gateway_id, fence_id, section_id, name, serial, type, status, voltage, signal, battery, enabled) VALUES
    (16, 4, 4, 16, 'Gal Oya Node 01', 'SN-E-04001', 'Voltage Monitor', 'online', 6.90, 89, 96, TRUE),
    (17, 4, 4, 17, 'Gal Oya Node 02', 'SN-E-04002', 'Voltage Monitor', 'online', 6.70, 87, 95, TRUE),
    (18, 4, 4, 18, 'Gal Oya Node 03', 'SN-E-04003', 'Voltage Monitor', 'online', 6.50, 84, 94, TRUE),
    (19, 4, 4, 19, 'Gal Oya Node 04', 'SN-E-04004', 'Voltage Monitor', 'online', 6.60, 90, 97, TRUE),
    (20, 4, 4, 20, 'Gal Oya Node 05', 'SN-E-04005', 'Voltage Monitor', 'online', 6.30, 86, 93, TRUE);

-- Gateway 5 (Somawathiya) Devices
INSERT INTO devices (id, gateway_id, fence_id, section_id, name, serial, type, status, voltage, signal, battery, enabled) VALUES
    (21, 5, 5, 21, 'Somawathiya Node 01', 'SN-E-05001', 'Voltage Monitor', 'online', 7.30, 95, 98, TRUE),
    (22, 5, 5, 22, 'Somawathiya Node 02', 'SN-E-05002', 'Voltage Monitor', 'online', 6.80, 89, 95, TRUE),
    (23, 5, 5, 23, 'Somawathiya Node 03', 'SN-E-05003', 'Voltage Monitor', 'online', 6.40, 86, 96, TRUE),
    (24, 5, 5, 24, 'Somawathiya Node 04', 'SN-E-05004', 'Voltage Monitor', 'online', 6.60, 88, 94, TRUE),
    (25, 5, 5, 25, 'Somawathiya Node 05', 'SN-E-05005', 'Voltage Monitor', 'online', 6.50, 92, 95, TRUE);
