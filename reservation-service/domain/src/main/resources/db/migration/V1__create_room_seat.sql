CREATE TABLE room (
    id              UUID                        PRIMARY KEY,
    code            VARCHAR(20)                 NOT NULL,
    name            VARCHAR(300)                NOT NULL,
    description     VARCHAR(1000)               NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE,
    deleted_at      TIMESTAMP WITH TIME ZONE,

    CONSTRAINT chk_room_updated_at
        CHECK (
            updated_at IS NULL
            OR updated_at >= created_at
        ),

    CONSTRAINT chk_room_deleted_at
        CHECK (
            deleted_at IS NULL
            OR deleted_at >= COALESCE(updated_at, created_at)
        )
);

CREATE TABLE seat (
    id              UUID                        PRIMARY KEY,
    room_id         UUID                        NOT NULL,
    code            VARCHAR(20)                 NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE,
    deleted_at      TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_seat_room_id FOREIGN KEY (room_id)
        REFERENCES room (id) ON DELETE RESTRICT ON UPDATE RESTRICT,

    CONSTRAINT chk_seat_updated_at
        CHECK (
            updated_at IS NULL
            OR updated_at >= created_at
        ),

    CONSTRAINT chk_seat_deleted_at
        CHECK (
            deleted_at IS NULL
            OR deleted_at >= COALESCE(updated_at, created_at)
        )
);

CREATE UNIQUE INDEX uk_room_code ON room (code);
CREATE UNIQUE INDEX uk_seat_code ON seat (code);
CREATE INDEX idx_seat_room_id ON seat (room_id);
