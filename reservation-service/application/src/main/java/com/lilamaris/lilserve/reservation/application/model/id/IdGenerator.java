package com.lilamaris.lilserve.reservation.application.model.id;

/**
 * Generates identifiers of one type. The application layer owns this policy, so services depend on
 * this function interface rather than on a database sequence.
 *
 * @param <ID> the identifier type
 */
@FunctionalInterface
public interface IdGenerator<ID> {

    /**
     * Returns a new identifier.
     *
     * @return an identifier that is not in use
     */
    ID generate();
}
