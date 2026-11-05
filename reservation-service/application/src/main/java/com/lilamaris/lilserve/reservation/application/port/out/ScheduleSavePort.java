package com.lilamaris.lilserve.reservation.application.port.out;

import com.lilamaris.lilserve.reservation.domain.Schedule;
import java.util.Collection;

/** Persists recurring slots within the caller's transaction. */
public interface ScheduleSavePort {
    /**
     * Saves a slot. Constraint failures can surface at flush or commit.
     *
     * @param schedule the slot to save, not null
     */
    void save(Schedule schedule);

    /**
     * Saves all slots in the caller's transaction. An empty collection is a no-op. Constraint
     * failures can surface at flush or commit and must roll back the whole batch.
     *
     * @param schedules the slots to save, without null elements
     */
    void saveAll(Collection<Schedule> schedules);
}
