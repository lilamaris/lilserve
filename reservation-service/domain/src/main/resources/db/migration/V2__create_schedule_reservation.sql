CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE schedule (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL,
    day_of_week SMALLINT NOT NULL,
    start_at TIME NOT NULL,
    end_at TIME NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT chk_schedule_day_of_week
        CHECK (day_of_week BETWEEN 1 AND 7),

    CONSTRAINT chk_schedule_end_after_start
        CHECK (end_at > start_at),

    CONSTRAINT chk_schedule_deleted_at
        CHECK (deleted_at IS NULL OR deleted_at >= created_at),

    CONSTRAINT ex_schedule_no_overlap EXCLUDE USING gist (
        room_id WITH =,
        day_of_week WITH =,
        tsrange(DATE '2000-01-01' + start_at,
                DATE '2000-01-01' + end_at) WITH &&
    ) WHERE (deleted_at IS NULL)
);

CREATE TABLE reserved_schedule (
    id UUID PRIMARY KEY,
    reservation_id UUID NOT NULL,
    room_id UUID NOT NULL,
    seat_id UUID NOT NULL,
    schedule_id UUID NOT NULL,
    start_at TIME NOT NULL,
    end_at TIME NOT NULL,
    reservation_date DATE NOT NULL,
    time_zone TEXT NOT NULL,
    starts_at TIMESTAMP WITH TIME ZONE GENERATED ALWAYS AS (
        (reservation_date + start_at) AT TIME ZONE time_zone
    ) STORED NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE GENERATED ALWAYS AS (
        (reservation_date + end_at) AT TIME ZONE time_zone
    ) STORED NOT NULL,
    day_of_week SMALLINT GENERATED ALWAYS AS (
        EXTRACT(ISODOW FROM reservation_date)::SMALLINT
    ) STORED,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT chk_reserved_end_after_start
        CHECK (ends_at > starts_at),

    CONSTRAINT chk_schedule_deleted_at
        CHECK (deleted_at IS NULL OR deleted_at >= created_at),

    CONSTRAINT ex_reserved_seat_no_overlap
        EXCLUDE USING gist (
            seat_id WITH =,
            tstzrange(starts_at, ends_at, '[)') WITH &&
        ) WHERE (deleted_at IS NULL)
);

CREATE TABLE reservation (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL,
    seat_id UUID NOT NULL,
    reservation_date DATE NOT NULL,
    time_zone TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT chk_reservation_deleted_at
        CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE UNIQUE INDEX uk_seat_id_room
    ON seat (id, room_id);

CREATE UNIQUE INDEX uk_schedule_id_room
    ON schedule (id, room_id);

CREATE UNIQUE INDEX uk_schedule_id_day
    ON schedule (id, day_of_week);

CREATE UNIQUE INDEX uk_schedule_id_times
    ON schedule (id, start_at, end_at);

CREATE UNIQUE INDEX uk_schedule_room_day_start_active
    ON schedule (room_id, day_of_week, start_at)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uk_reserved_seat_schedule_date_active
    ON reserved_schedule (seat_id, schedule_id, reservation_date)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uk_reservation_allocation_scope
    ON reservation (id, room_id, seat_id, reservation_date, time_zone);

CREATE INDEX idx_reserved_reservation_id
    ON reserved_schedule (reservation_id);

CREATE INDEX idx_reserved_room_date
    ON reserved_schedule (room_id, reservation_date);

ALTER TABLE schedule
    ADD CONSTRAINT fk_schedule_room_id
    FOREIGN KEY (room_id)
    REFERENCES room (id) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE reserved_schedule
    ADD CONSTRAINT fk_reserved_reservation_scope
    FOREIGN KEY (reservation_id, room_id, seat_id, reservation_date, time_zone)
    REFERENCES reservation (id, room_id, seat_id, reservation_date, time_zone)
    ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE reserved_schedule
    ADD CONSTRAINT fk_reserved_room_id
    FOREIGN KEY (seat_id, room_id)
    REFERENCES seat (id, room_id) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE reserved_schedule
    ADD CONSTRAINT fk_reserved_schedule_room
    FOREIGN KEY (schedule_id, room_id)
    REFERENCES schedule (id, room_id) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE reserved_schedule
    ADD CONSTRAINT fk_reserved_schedule_day
    FOREIGN KEY (schedule_id, day_of_week)
    REFERENCES schedule (id, day_of_week) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE reserved_schedule
    ADD CONSTRAINT fk_reserved_schedule_times
    FOREIGN KEY (schedule_id, start_at, end_at)
    REFERENCES schedule (id, start_at, end_at) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE reservation
    ADD CONSTRAINT fk_reservation_room_id
    FOREIGN KEY (room_id)
    REFERENCES room (id) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE reservation
    ADD CONSTRAINT fk_reservation_seat_room
    FOREIGN KEY (seat_id, room_id)
    REFERENCES seat (id, room_id) ON DELETE RESTRICT ON UPDATE RESTRICT;