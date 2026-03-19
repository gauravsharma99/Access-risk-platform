package com.accessrisk.config;

import com.accessrisk.entity.*;
import com.accessrisk.enums.Severity;
import com.accessrisk.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Loads a realistic enterprise dataset on first startup.
 *
 * Domain: Accounts Payable / Procurement in a financial services firm.
 *
 * Idempotency guard: if any permissions already exist the seeder exits immediately,
 * making it safe across restarts without needing a separate Spring profile.
 *
 * Transaction model: no @Transactional on run() — each repository.save() call
 * executes inside its own Spring Data transaction and commits immediately.
 * This ensures data is visible to the @Order(2) StartupRunner even if they
 * run within the same JVM startup sequence.
 *
 * Expected violations after StartupRunner analysis:
 *   Carol Martinez  CRITICAL  CREATE_VENDOR + APPROVE_PAYMENT
 *   Carol Martinez  CRITICAL  PROCESS_PAYMENT + APPROVE_PAYMENT
 *   Carol Martinez  HIGH      CREATE_PURCHASE_ORDER + APPROVE_PURCHASE_ORDER
 *   David Chen      CRITICAL  CREATE_VENDOR + APPROVE_PAYMENT
 *   David Chen      HIGH      CREATE_PURCHASE_ORDER + APPROVE_PURCHASE_ORDER
 *   David Chen      HIGH      APPROVE_VENDOR + APPROVE_PAYMENT
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final PermissionRepository     permissionRepository;
    private final RoleRepository           roleRepository;
    private final UserRepository           userRepository;
    private final UserRoleRepository       userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RiskRuleRepository       riskRuleRepository;

    @Override
    public void run(String... args) {
        if (permissionRepository.count() > 0) {
            log.info("[DataSeeder] Seed data already present — skipping.");
            return;
        }

        log.info("[DataSeeder] Loading enterprise seed data...");

        Map<String, Permission> perms = seedPermissions();
        Map<String, Role>       roles = seedRoles();
        seedRolePermissions(roles, perms);

        Map<String, User> users = seedUsers();
        seedUserRoles(users, roles);
        seedRiskRules(perms);

        log.info("[DataSeeder] Seed complete — {} permissions, {} roles, {} users, {} risk rules.",
                permissionRepository.count(),
                roleRepository.count(),
                userRepository.count(),
                riskRuleRepository.count());
    }

    // =========================================================================
    // 8 Permissions
    // =========================================================================

    private Map<String, Permission> seedPermissions() {
        Permission createVendor         = perm("CREATE_VENDOR",
                "Register a new vendor in the accounts payable system");
        Permission approveVendor        = perm("APPROVE_VENDOR",
                "Approve or reject new vendor registrations");
        Permission createPurchaseOrder  = perm("CREATE_PURCHASE_ORDER",
                "Create and submit purchase orders for goods or services");
        Permission approvePurchaseOrder = perm("APPROVE_PURCHASE_ORDER",
                "Approve purchase orders before they are fulfilled");
        Permission processPayment       = perm("PROCESS_PAYMENT",
                "Initiate payment transactions to approved vendors");
        Permission approvePayment       = perm("APPROVE_PAYMENT",
                "Authorise payment transactions above the auto-approval threshold");
        Permission accessReports        = perm("ACCESS_FINANCIAL_REPORTS",
                "View financial statements, ledgers, and spend analytics");
        Permission manageAccess         = perm("MANAGE_USER_ACCESS",
                "Assign and revoke user roles within the platform");

        return Map.of(
                "CREATE_VENDOR",            createVendor,
                "APPROVE_VENDOR",           approveVendor,
                "CREATE_PURCHASE_ORDER",    createPurchaseOrder,
                "APPROVE_PURCHASE_ORDER",   approvePurchaseOrder,
                "PROCESS_PAYMENT",          processPayment,
                "APPROVE_PAYMENT",          approvePayment,
                "ACCESS_FINANCIAL_REPORTS", accessReports,
                "MANAGE_USER_ACCESS",       manageAccess
        );
    }

    // =========================================================================
    // 4 Roles
    // =========================================================================

    private Map<String, Role> seedRoles() {
        Role apClerk = role("AP_CLERK",
                "Accounts Payable Clerk — day-to-day AP operations: vendor registration, " +
                "purchase order creation, and payment initiation");
        Role finMgr  = role("FINANCE_MANAGER",
                "Finance Manager — approval authority over vendors, purchase orders, " +
                "and payments; full access to financial reports");
        Role procOff = role("PROCUREMENT_OFFICER",
                "Procurement Officer — manages vendor sourcing and purchase order creation; " +
                "read access to financial reports for budget tracking");
        Role itAdmin = role("IT_ADMIN",
                "IT Administrator — manages platform user access; no financial transaction permissions");

        return Map.of(
                "AP_CLERK",            apClerk,
                "FINANCE_MANAGER",     finMgr,
                "PROCUREMENT_OFFICER", procOff,
                "IT_ADMIN",            itAdmin
        );
    }

    // =========================================================================
    // Role → Permission assignments
    //
    //  AP_CLERK            → CREATE_VENDOR, CREATE_PURCHASE_ORDER, PROCESS_PAYMENT
    //  FINANCE_MANAGER     → APPROVE_VENDOR, APPROVE_PURCHASE_ORDER, APPROVE_PAYMENT,
    //                        ACCESS_FINANCIAL_REPORTS
    //  PROCUREMENT_OFFICER → CREATE_VENDOR, CREATE_PURCHASE_ORDER, ACCESS_FINANCIAL_REPORTS
    //  IT_ADMIN            → MANAGE_USER_ACCESS
    // =========================================================================

    private void seedRolePermissions(Map<String, Role> roles, Map<String, Permission> perms) {
        assign(roles.get("AP_CLERK"), perms.get("CREATE_VENDOR"));
        assign(roles.get("AP_CLERK"), perms.get("CREATE_PURCHASE_ORDER"));
        assign(roles.get("AP_CLERK"), perms.get("PROCESS_PAYMENT"));

        assign(roles.get("FINANCE_MANAGER"), perms.get("APPROVE_VENDOR"));
        assign(roles.get("FINANCE_MANAGER"), perms.get("APPROVE_PURCHASE_ORDER"));
        assign(roles.get("FINANCE_MANAGER"), perms.get("APPROVE_PAYMENT"));
        assign(roles.get("FINANCE_MANAGER"), perms.get("ACCESS_FINANCIAL_REPORTS"));

        assign(roles.get("PROCUREMENT_OFFICER"), perms.get("CREATE_VENDOR"));
        assign(roles.get("PROCUREMENT_OFFICER"), perms.get("CREATE_PURCHASE_ORDER"));
        assign(roles.get("PROCUREMENT_OFFICER"), perms.get("ACCESS_FINANCIAL_REPORTS"));

        assign(roles.get("IT_ADMIN"), perms.get("MANAGE_USER_ACCESS"));
    }

    // =========================================================================
    // 4 Users
    //
    //  Alice Johnson   → AP_CLERK only              (clean)
    //  Bob Williams    → FINANCE_MANAGER only        (clean)
    //  Carol Martinez  → AP_CLERK + FINANCE_MANAGER  (RISKY — full initiate+approve cycle)
    //  David Chen      → PROCUREMENT_OFFICER + FINANCE_MANAGER  (RISKY — vendor+approval)
    // =========================================================================

    private Map<String, User> seedUsers() {
        return Map.of(
                "ALICE", user("Alice Johnson",  "alice.johnson@company.com",  "Finance"),
                "BOB",   user("Bob Williams",   "bob.williams@company.com",   "Finance"),
                "CAROL", user("Carol Martinez", "carol.martinez@company.com", "Finance"),
                "DAVID", user("David Chen",     "david.chen@company.com",     "Procurement")
        );
    }

    // =========================================================================
    // User → Role assignments
    // =========================================================================

    private void seedUserRoles(Map<String, User> users, Map<String, Role> roles) {
        assign(users.get("ALICE"), roles.get("AP_CLERK"));

        assign(users.get("BOB"),   roles.get("FINANCE_MANAGER"));

        // Carol: accumulated both roles — triggers 3 violations
        assign(users.get("CAROL"), roles.get("AP_CLERK"));
        assign(users.get("CAROL"), roles.get("FINANCE_MANAGER"));

        // David: vendor sourcing + approval authority — triggers 3 violations
        assign(users.get("DAVID"), roles.get("PROCUREMENT_OFFICER"));
        assign(users.get("DAVID"), roles.get("FINANCE_MANAGER"));
    }

    // =========================================================================
    // 4 Risk Rules — SoD conflict definitions
    // =========================================================================

    private void seedRiskRules(Map<String, Permission> perms) {
        rule("Vendor Creation and Payment Approval",
             "financial approval fraud risk — a single user can register a fictitious vendor " +
             "and authorise payment to that vendor without any independent verification",
             perms.get("CREATE_VENDOR"), perms.get("APPROVE_PAYMENT"),
             Severity.CRITICAL);

        rule("Purchase Order Self-Approval",
             "procurement control bypass risk — a single user can raise a purchase order " +
             "and approve it without a second authorisation, circumventing spend controls",
             perms.get("CREATE_PURCHASE_ORDER"), perms.get("APPROVE_PURCHASE_ORDER"),
             Severity.HIGH);

        rule("Payment Initiation and Approval Conflict",
             "payment integrity risk — a single user can both initiate and approve the same " +
             "payment transaction, violating the four-eyes principle required by SOX and PCI-DSS",
             perms.get("PROCESS_PAYMENT"), perms.get("APPROVE_PAYMENT"),
             Severity.CRITICAL);

        rule("Vendor Approval and Payment Authorisation",
             "vendor lifecycle risk — a single user controls both vendor approval and payment " +
             "authorisation, providing end-to-end control of the vendor payment cycle " +
             "without independent oversight",
             perms.get("APPROVE_VENDOR"), perms.get("APPROVE_PAYMENT"),
             Severity.HIGH);
    }

    // =========================================================================
    // Entity builders
    // =========================================================================

    private Permission perm(String name, String description) {
        return permissionRepository.save(
                Permission.builder().name(name).description(description).build());
    }

    private Role role(String name, String description) {
        return roleRepository.save(
                Role.builder().name(name).description(description).build());
    }

    private User user(String name, String email, String department) {
        return userRepository.save(
                User.builder().name(name).email(email).department(department).build());
    }

    private void assign(Role role, Permission permission) {
        rolePermissionRepository.save(
                RolePermission.builder()
                        .roleId(role.getId())
                        .permissionId(permission.getId())
                        .build());
    }

    private void assign(User user, Role role) {
        userRoleRepository.save(
                UserRole.builder()
                        .userId(user.getId())
                        .roleId(role.getId())
                        .build());
    }

    private void rule(String name, String description,
                      Permission permA, Permission permB, Severity severity) {
        riskRuleRepository.save(
                RiskRule.builder()
                        .name(name).description(description)
                        .permissionAId(permA.getId())
                        .permissionBId(permB.getId())
                        .severity(severity)
                        .active(true)
                        .build());
    }
}
