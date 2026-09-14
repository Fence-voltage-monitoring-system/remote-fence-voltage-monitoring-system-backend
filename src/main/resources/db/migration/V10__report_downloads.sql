ALTER TABLE generated_reports ADD COLUMN file_content BYTEA;
ALTER TABLE generated_reports ADD COLUMN fence_ids TEXT NOT NULL DEFAULT '';
