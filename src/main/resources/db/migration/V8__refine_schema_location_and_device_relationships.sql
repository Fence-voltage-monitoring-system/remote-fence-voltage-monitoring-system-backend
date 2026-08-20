-- V8: Refine schema location and device relationships
-- 1. Fix circular foreign key dependency between sections and devices
ALTER TABLE sections DROP COLUMN IF EXISTS device_id;

-- 2. Add optional province_id and district_id location overrides to sections
ALTER TABLE sections ADD COLUMN IF NOT EXISTS province_id BIGINT REFERENCES provinces(id) ON DELETE SET NULL;
ALTER TABLE sections ADD COLUMN IF NOT EXISTS district_id BIGINT REFERENCES districts(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_sections_province_district ON sections(province_id, district_id);

-- 3. Location Consistency Enforcement Function & Triggers
CREATE OR REPLACE FUNCTION check_location_consistency()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.district_id IS NOT NULL AND NEW.province_id IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1 FROM districts WHERE id = NEW.district_id AND province_id = NEW.province_id
        ) THEN
            RAISE EXCEPTION 'District ID % does not belong to Province ID %', NEW.district_id, NEW.province_id;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_fence_location_check ON fences;
CREATE TRIGGER trg_fence_location_check
BEFORE INSERT OR UPDATE ON fences
FOR EACH ROW EXECUTE FUNCTION check_location_consistency();

DROP TRIGGER IF EXISTS trg_section_location_check ON sections;
CREATE TRIGGER trg_section_location_check
BEFORE INSERT OR UPDATE ON sections
FOR EACH ROW EXECUTE FUNCTION check_location_consistency();

-- 4. Optimized compound telemetry index for real-time charting
CREATE INDEX IF NOT EXISTS idx_telemetry_device_time_voltage ON telemetry_readings(device_id, recorded_at DESC, voltage_kv);
