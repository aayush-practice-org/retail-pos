package io.aygh.security.context;

import java.util.UUID;

/**
 * Per-request holder for the authenticated user, populated by
 * {@code PasetoAuthenticationFilter} and cleared when the request completes.
 */
public class UserHolder {

    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();
    private static final ThreadLocal<UUID> USER_ID = new ThreadLocal<>();

    private UserHolder() {
    }

    public static void setUsername(String username) {
        USERNAME.set(username);
    }

    public static void setRole(String role) {
        ROLE.set(role);
    }

    public static void setUserId(UUID userId) {
        USER_ID.set(userId);
    }

    public static String getCurrentUsername() {
        return USERNAME.get();
    }

    public static String getCurrentUserRole() {
        return ROLE.get();
    }

    public static UUID getCurrentUserId() {
        return USER_ID.get();
    }

    public static String printToString() {
        return "Username: " + getCurrentUsername() + " , " +
                "Role: " + getCurrentUserRole() + " , " +
                "User ID: " + getCurrentUserId();
    }

    public static void clear() {
        USERNAME.remove();
        ROLE.remove();
        USER_ID.remove();
    }
}
