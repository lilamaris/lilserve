package com.lilamaris.lilserve.identity.domain;

/** The account status of a user. Deletion is tracked separately by the deletion timestamp. */
public enum UserStatus {
    /** The user is allowed to authenticate when not deleted. */
    ACTIVE,
    /** The user is temporarily prevented from authenticating. */
    SUSPENDED
}
