package io.aygh.identity.entity;

/**
 * Whether an account may be used, independent of what it is allowed to reach.
 * A suspended manager still holds the manager's access — it is simply refused
 * at login until someone lifts the suspension.
 */
public enum UserStatus {

    /** Normal working account. */
    ACTIVE,

    /** Created but not yet handed over, or retired without deleting the history. */
    INACTIVE,

    /** Temporarily barred — disciplinary, security incident, unpaid leave. */
    SUSPENDED;

    public boolean canSignIn() {
        return this == ACTIVE;
    }
}
