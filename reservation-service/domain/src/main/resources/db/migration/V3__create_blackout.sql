CREATE TABLE blackout_schedule (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL,
    schedule_id UUID NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT chk_blackout_schedule_ends_after_starts
        CHECK (ends_at > starts_at),

    CONSTRAINT chk_blackout_schedule_updated_at
        CHECK (
            updated_at IS NULL
            OR updated_at >= created_at
        ),

    CONSTRAINT chk_blackout_schedule_deleted_at
        CHECK (
            deleted_at IS NULL
            OR deleted_at >= COALESCE(updated_at, created_at)
        )
);

ALTER TABLE blackout_schedule
    ADD CONSTRAINT fk_blackout_schedule_room
    FOREIGN KEY (schedule_id, room_id)
    REFERENCES schedule (id, room_id)
    ON DELETE RESTRICT ON UPDATE RESTRICT;
