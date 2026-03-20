package com.accessrisk.api;

import com.accessrisk.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * HTTP-level integration tests for user management and dashboard endpoints.
 *
 * Uses MockMvc (MOCK web environment) so requests are dispatched synchronously
 * in the same thread as the test — @Transactional rollback works correctly.
 *
 * Seed data is present: 4 users (including alice@company.com), 6 OPEN violations.
 */
@Transactional
@DisplayName("User API + Dashboard — integration")
class UserApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // =========================================================================
    // Test 1 — Create user: valid request returns 201 with populated body
    // =========================================================================

    @Test
    @DisplayName("POST /api/users: valid request creates user and returns 201")
    void createUser_validRequest_returns201WithBody() throws Exception {
        String body = """
                {
                  "name": "Eve Turner",
                  "email": "eve.turner@company.com",
                  "department": "Compliance"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Eve Turner")))
                .andExpect(jsonPath("$.email", is("eve.turner@company.com")))
                .andExpect(jsonPath("$.department", is("Compliance")));
    }

    // =========================================================================
    // Test 2 — Create user: duplicate email returns 409 with machine-readable code
    // =========================================================================

    @Test
    @DisplayName("POST /api/users: email already taken returns 409 DUPLICATE_RESOURCE")
    void createUser_duplicateEmail_returns409WithErrorCode() throws Exception {
        // alice@company.com is seeded by DataSeeder
        String body = """
                {
                  "name": "Alice Clone",
                  "email": "alice@company.com",
                  "department": "Finance"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.errorCode", is("DUPLICATE_RESOURCE")))
                .andExpect(jsonPath("$.message", containsString("alice@company.com")));
    }

    // =========================================================================
    // Test 3 — Get user: unknown id returns 404 with machine-readable code
    // =========================================================================

    @Test
    @DisplayName("GET /api/users/{id}: unknown id returns 404 RESOURCE_NOT_FOUND")
    void getUser_unknownId_returns404WithErrorCode() throws Exception {
        mockMvc.perform(get("/api/users/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")))
                .andExpect(jsonPath("$.message", containsString("99999")));
    }

    // =========================================================================
    // Test 4 — Dashboard: seed data produces the expected violation summary
    // =========================================================================

    @Test
    @DisplayName("GET /api/dashboard/summary: returns correct violation counts after seed data")
    void getDashboardSummary_afterSeedData_returnsExpectedCounts() throws Exception {
        // Seed data: 4 users, 4 roles, 8 permissions, 4 active risk rules
        // StartupRunner detects 6 violations: 3 CRITICAL (Carol×2 + David×1), 3 HIGH (Carol×1 + David×2)
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers", is(4)))
                .andExpect(jsonPath("$.totalRoles", is(4)))
                .andExpect(jsonPath("$.totalPermissions", is(8)))
                .andExpect(jsonPath("$.activeRiskRules", is(4)))
                .andExpect(jsonPath("$.openViolations", is(6)))
                .andExpect(jsonPath("$.criticalViolations", is(3)))
                .andExpect(jsonPath("$.highViolations", is(3)));
    }
}
