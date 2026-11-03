# Reservation schema design intent

Date: 2026-11-03  
Tracking issue: https://github.com/lilamaris/lilserve/issues/6  
Source: [V2 migration](../../reservation-service/domain/src/main/resources/db/migration/V2__create_schedule_reservation.sql)

## Purpose and scope

The schema separates recurring room availability from reservations of dated seat
usage. Operators can replace available slots without rewriting the slots referenced
by existing reservations. Reservation conflicts are evaluated using actual occupied
time, including reservations created from different schedule versions.

This document describes the current V2 schema, including its enforcement boundaries.
It does not introduce schema changes, JPA mappings, blackout rules, or a reservation
status machine. The current V2 is the basis for this document.

## Table responsibilities

| Table | Responsibility |
| --- | --- |
| `schedule` | A recurring room slot defined by an ISO weekday and local start/end times. |
| `reservation` | A header for one seat, one service date, and one timezone, grouping at least one reserved slot. |
| `reserved_schedule` | A dated seat allocation referencing a reservation and its original slot, with local-time and absolute-time snapshots. |

A reservation contains at least one allocation for exactly one seat on one service
date. All allocations share the header's room, seat, date, and timezone. Their schedules
must belong to the seat's room and match the service date's weekday. Each slot ends
on the same service date; multi-day reservations are not supported.

Slots do not need to be contiguous. A reservation for seat S1 may contain 11:00-13:00
and 14:00-15:00; another reservation for S1 may occupy 13:00-14:00 on the same date.
Conflict checks operate on each allocation, never on a single enclosing interval
covering the gaps between selected slots.

Cancellation applies to the whole reservation. Partial cancellation of individual
allocations is not supported.

## Recurring availability and schedule history

`schedule` stores `room_id`, `day_of_week`, `start_at`, and `end_at`.
Weekdays use ISO numbering: Monday is 1 and Sunday is 7. The end time must be strictly
later than the start time, so a slot cannot cross midnight.

Active slots have a null `deleted_at`. Their exclusion constraint rejects overlapping
times for the same room and weekday. A fixed anchor date converts local times into
`tsrange` values; that date is not a service date. The default half-open range permits
adjacent slots such as 10:00-11:00 and 11:00-12:00.

The partial unique index on room, weekday, and start time explicitly rejects duplicate
active starts. This case is also covered by the overlap exclusion constraint.

The intended replacement workflow is:

1. Retain the original schedule row and mark it inactive using `deleted_at`.
2. Insert replacement slots with new IDs in the same transaction.
3. Preserve existing allocations and their references to the original schedule.

This is append-only for slot definitions, with a mutable retirement marker; it is not
a strictly insert-only event history. V2 does not prohibit every update or deletion.
Foreign keys restrict changes to referenced keys and prevent deletion of referenced
rows, but unreferenced schedule definitions can still be changed by SQL. Preserving
definitions remains a write-policy requirement unless additional enforcement is added.

## Dated allocations and snapshots

Each `reserved_schedule` row stores the following related values:

| Columns | Meaning |
| --- | --- |
| `schedule_id` | The original recurring slot used to create the allocation. |
| `reservation_date`, `start_at`, `end_at` | The service date and copied local slot times. |
| `time_zone` | The application timezone supplied when creating the allocation. |
| `starts_at`, `ends_at` | Stored generated absolute instants of seat usage. |

Both copied local times and confirmed instants are snapshots. The local-time snapshot
is checked against its original schedule by a composite foreign key. The absolute
snapshot is used for overlap enforcement and preserves confirmed instants independently
of later application timezone configuration changes.

Writers supply `time_zone` from application configuration, for example `Asia/Seoul`.
The header stores the timezone once as reservation scope; the allocation copy must
match it through a composite foreign key, just like the seat and service date.
The database derives `starts_at` and `ends_at` using
`(reservation_date + local_time) AT TIME ZONE time_zone`. Both are `STORED` generated
columns: writers cannot supply independent absolute instants, so a conversion-equality
CHECK is unnecessary. The existing end-after-start CHECK remains in place.

`TIMESTAMP WITH TIME ZONE` represents an instant rather than retaining a timezone
identifier; `time_zone` separately preserves the conversion input. Later application
configuration changes do not change existing rows. Updating a row's date, local times,
or timezone recalculates its generated instants, so snapshot immutability still needs
a write policy. An invalid timezone is rejected during conversion; matching a valid
timezone to the application's intended configuration remains the writer's responsibility.

`day_of_week` is generated from `reservation_date` using `EXTRACT(ISODOW ...)`.
Writers supply the date rather than an independent weekday. A composite foreign key
then requires that generated weekday to match the referenced schedule.

## Referential integrity

| Constraint | Guarantee |
| --- | --- |
| `fk_reserved_reservation_scope` | The reservation exists and has the allocation's room, seat, service date, and timezone. |
| `fk_reservation_seat_room` | The header's seat belongs to its room, even before allocations are inserted. |
| `fk_reserved_room_id` | The seat exists and belongs to that same room. |
| `fk_reserved_schedule_room` | The original slot exists and belongs to that same room. |
| `fk_reserved_schedule_day` | The service date's weekday matches the original slot. |
| `fk_reserved_schedule_times` | Copied local start/end times match the original slot. |

The unique indexes containing each parent ID and its associated attributes support
these composite foreign keys. Although each ID is already a primary key, the composite
reference requires a corresponding unique target.

Foreign keys use `ON DELETE RESTRICT` and `ON UPDATE RESTRICT` to retain referenced
records and prevent referenced key changes. They do not check soft-deletion state.
Each table also rejects a non-null `deleted_at` earlier than its `created_at`.

The schema does not use custom functions or triggers to enforce aggregate lifecycle
rules. Requiring at least one selected allocation and cancelling the whole reservation
are business rules for the reservation workflow and its transaction boundary. The
header and allocations must be saved together; an empty header is not rejected by V2.

## Conflict prevention across schedule versions

The active-allocation unique index on `(seat_id, schedule_id, reservation_date)`
prevents selecting the same slot twice for the same seat and date. It cannot alone
prevent conflicts between different schedule IDs.

`ex_reserved_seat_no_overlap` additionally excludes overlapping
`tstzrange(starts_at, ends_at, '[)')` values for the same seat among active allocations.
`btree_gist` provides the scalar equality support used with the range comparison.
The strict end-after-start check prevents empty or reversed occupied ranges.

For example, an existing allocation for seat X at 10:00-11:00 remains active after
its slot A is retired. A replacement slot B offering 10:00-12:00 can be created, but
an allocation of B for X on the same date is rejected if its confirmed instants
overlap the existing allocation. An allocation starting exactly at 11:00 is permitted.

The database constraint is the final conflict boundary, including concurrent inserts.
An availability query can improve feedback but cannot replace the constraint. Writers
must handle constraint violations and roll back the reservation transaction.

The unique slot/date rule and the absolute-time overlap rule are both enforced.
Generated columns ensure that occupied instants are derived from the stored date,
local times, and timezone rather than independently supplied values.

## Lifecycle and transaction requirements

Retiring a schedule only stops offering that slot for new allocations. It does not
cancel existing allocations or release their occupied time.

Cancelling a reservation must mark all its allocations inactive in the same transaction
as the header. The application must not expose partial cancellation and must update
both the header and all its allocations; the database does not propagate cancellation
or reject inconsistent cancellation states. On successful cancellation, every occupied
interval is released together. Reactivating allocations subjects them to conflict
constraints again; whether restoration is offered remains an application policy.

Reservation creation must coordinate with schedule retirement. A possible protocol is
to lock the selected schedule rows, verify they remain active, and insert all
allocations in one transaction. Retirement must participate in the same locking
protocol. Seat and room active-state checks require their own agreed write protocol.
The active-state and retirement locking workflows are implementation requirements.
Any concurrent modification of one reservation's allocation set must also use an
agreed locking protocol when needed. V2 enforces allocation scope and occupied-time
conflicts, while non-empty selection and whole cancellation belong to business logic.

## Enforcement boundaries and remaining decisions

The following are not guaranteed by the current schema:

- Whether the supplied `time_zone` matches the intended application configuration.
  Conversion consistency with the stored timezone is enforced by generated columns.
- Rejection of new allocations referencing inactive schedules, seats, rooms, or headers.
  Foreign keys validate existence and attributes, not active state.
- Immutability of all schedule definitions and confirmed allocations. Physical deletion
  or reassignment of allocations must be excluded from normal cancellation workflows.
- At least one selected allocation per reservation. The reservation workflow must
  validate a non-empty selection and persist it with its header in one transaction.
- Whole cancellation and propagation from a header to its allocations. Writers update
  both in one transaction; V2 alone allows inconsistent cancellation states.
- Timezone conversion policy for ambiguous or nonexistent local times if the configured
  timezone observes daylight saving transitions. The supported timezone range and any
  custom daylight saving policy have not been decided. PostgreSQL currently applies
  its default conversion rules; no additional rejection policy is introduced here.

These boundaries must remain explicit: overlap enforcement is reliable for the stored
absolute intervals, while correct construction and lifecycle of those intervals need
the corresponding write rules.

## Validation status

The [JPA mapping tests](../../reservation-service/persistence/src/test/java/com/lilamaris/lilserve/reservation/persistence/jpa/ReservedScheduleJpaMappingTest.java)
exercise generated instants, ISO weekday mapping, overlap rejection across schedule
replacement, and adjacent allocations using PostgreSQL Testcontainers. Domain unit
tests cover the entities, identifiers, and weekday converter. The former standalone
SQL check and custom aggregate constraint triggers are no longer part of this design.
Non-empty selection and whole-cancellation orchestration still need application-level
implementation and verification when reservation use cases are introduced.
