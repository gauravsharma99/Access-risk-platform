package com.accessrisk.util;

/**
 * Builds structured key=value detail strings for audit log entries.
 *
 * Design decision: using a lightweight pipe-delimited format rather than full JSON
 * keeps the utility dependency-free and the audit_logs.details column human-readable
 * without a JSON parser. A future iteration could replace this with Jackson
 * serialization of a dedicated AuditDetailDto.
 *
 * Example output:
 *   name=Alice Smith | email=alice@company.com | department=Finance
 *   roleId=3 | roleName=Accounts Payable | userId=7 | userName=Bob Jones
 */
public final class AuditDetails {

    private AuditDetails() {}

    /**
     * Accepts alternating key-value pairs and joins them into a structured detail string.
     *
     * Usage:
     *   AuditDetails.of("name", user.getName(), "email", user.getEmail())
     *   // → "name=Alice Smith | email=alice@company.com"
     *
     * @throws IllegalArgumentException if an odd number of arguments is passed
     */
    public static String of(Object... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "AuditDetails.of() requires an even number of arguments (key-value pairs)"
            );
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            if (i > 0) sb.append(" | ");
            sb.append(keyValuePairs[i])
              .append("=")
              .append(keyValuePairs[i + 1] != null ? keyValuePairs[i + 1] : "null");
        }
        return sb.toString();
    }
}
