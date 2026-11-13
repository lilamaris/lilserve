CREATE INDEX idx_blackout_room_usage_active
    ON blackout_schedule USING gist (room_id, tstzrange(starts_at, ends_at, '[)'))
    WHERE deleted_at IS NULL;
