package io.aygh.tenant;

import io.aygh.exception.BusinessException;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Turns a company name into the name of a PostgreSQL schema, and refuses
 * anything that would not be safe as one.
 * <p>
 * A slug is not cosmetic here: it is interpolated into {@code CREATE SCHEMA} and
 * into every {@code search_path} the tenant's connections use, so it is checked
 * against a whitelist rather than escaped. Identifiers cannot be bound as
 * parameters, and a slug that only ever matches {@code [a-z][a-z0-9_]*} cannot
 * carry a quote, a semicolon or a comment marker into either statement.
 */
public final class TenantSlug {

    /** PostgreSQL truncates identifiers at 63 bytes; a longer slug would collide silently. */
    public static final int MAX_LENGTH = 63;

    private static final Pattern VALID = Pattern.compile("^[a-z][a-z0-9_]{1," + (MAX_LENGTH - 1) + "}$");

    private static final Pattern SEPARATORS = Pattern.compile("[^a-z0-9]+");

    /**
     * Names Postgres owns or that this application already means something by.
     * The {@code pg_} prefix is reserved outright and is rejected by the pattern
     * check below rather than listed.
     */
    private static final Set<String> RESERVED = Set.of(
            "public", "information_schema", "pg_catalog", "pg_toast", "postgres",
            "admin", "superadmin", "tenant", "template", "flyway");

    private TenantSlug() {
    }

    /**
     * The slug a company name would be given. Deterministic, so the same name
     * always proposes the same schema and the uniqueness check downstream is the
     * one that decides.
     */
    public static String from(String companyName) {
        if (companyName == null || companyName.isBlank()) {
            throw new BusinessException("Company name is required to derive a tenant schema name");
        }

        String slug = SEPARATORS.matcher(companyName.trim().toLowerCase())
                .replaceAll("_")
                .replaceAll("^_+|_+$", "");

        // Schema names cannot start with a digit; prefixing beats rejecting a
        // company that legitimately begins with one ("7 Eleven").
        if (!slug.isEmpty() && Character.isDigit(slug.charAt(0))) {
            slug = "m_" + slug;
        }

        if (slug.length() > MAX_LENGTH) {
            slug = slug.substring(0, MAX_LENGTH).replaceAll("_+$", "");
        }

        return requireValid(slug);
    }

    /** Throws unless the slug is safe to use as a schema identifier. */
    public static String requireValid(String slug) {
        if (slug == null || !VALID.matcher(slug).matches()) {
            throw new BusinessException(
                    "'" + slug + "' is not a usable tenant name: use 2 to " + MAX_LENGTH
                            + " lowercase letters, digits or underscores, starting with a letter");
        }
        if (slug.startsWith("pg_") || RESERVED.contains(slug)) {
            throw new BusinessException("'" + slug + "' is a reserved schema name — choose another company name");
        }
        return slug;
    }
}
