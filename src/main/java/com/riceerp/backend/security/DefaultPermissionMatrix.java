package com.riceerp.backend.security;

import com.riceerp.backend.enums.OrgRole;

import java.util.*;

public class DefaultPermissionMatrix {

    public static final List<String> ALL_PERMISSIONS = List.of(
            "stock:adjust",
            "product:view", "product:create", "product:edit", "product:status",
            "purchase:view", "purchase:create", "purchase:approve",
            "customer:view", "customer:create", "customer:edit", "customer:status",
            "supplier:view", "supplier:create", "supplier:edit", "supplier:status",
            "sale:view", "sale:create",
            "sales-order:view", "sales-order:create", "sales-order:cancel",
            "delivery:view", "delivery:create", "delivery:confirm", "delivery:fail",
            "payment:view", "payment:create",
            "invoice:view", "invoice:status",
            "reconciliation:view", "reconciliation:create",
            "inventory:view",
            "beat-plan:view", "beat-plan:manage",
            "dashboard:view",
            "member:view", "member:manage"
    );

    private static final Map<OrgRole, Set<String>> DEFAULTS = new EnumMap<>(OrgRole.class);

    static {
        // 1. ADMIN
        DEFAULTS.put(OrgRole.ADMIN, new HashSet<>(ALL_PERMISSIONS));

        // 2. MANAGER
        Set<String> manager = new HashSet<>(ALL_PERMISSIONS);
        manager.remove("member:manage");
        DEFAULTS.put(OrgRole.MANAGER, manager);

        // 3. ACCOUNTANT
        Set<String> accountant = new HashSet<>(Set.of(
                "product:view",
                "purchase:view", "purchase:create", "purchase:approve",
                "customer:view", "customer:create", "customer:edit", "customer:status",
                "supplier:view", "supplier:create", "supplier:edit", "supplier:status",
                "sale:view", "sale:create",
                "sales-order:view",
                "delivery:view",
                "payment:view", "payment:create",
                "invoice:view", "invoice:status",
                "reconciliation:view", "reconciliation:create",
                "inventory:view",
                "beat-plan:view",
                "dashboard:view",
                "member:view"
        ));
        DEFAULTS.put(OrgRole.ACCOUNTANT, accountant);

        // 4. SALES
        Set<String> sales = new HashSet<>(Set.of(
                "product:view", "product:edit",
                "customer:view", "customer:create", "customer:edit", "customer:status",
                "sale:view", "sale:create",
                "sales-order:view", "sales-order:create",
                "delivery:view",
                "payment:view", "payment:create",
                "inventory:view",
                "beat-plan:view",
                "dashboard:view",
                "member:view"
        ));
        DEFAULTS.put(OrgRole.SALES, sales);

        // 5. WAREHOUSE
        Set<String> warehouse = new HashSet<>(Set.of(
                "stock:adjust",
                "product:view",
                "purchase:view", "purchase:create",
                "customer:view",
                "supplier:view",
                "sale:view",
                "sales-order:view",
                "delivery:view", "delivery:create", "delivery:confirm", "delivery:fail",
                "invoice:view",
                "inventory:view",
                "dashboard:view",
                "member:view"
        ));
        DEFAULTS.put(OrgRole.WAREHOUSE, warehouse);
    }

    public static Set<String> getDefaults(OrgRole role) {
        if (role == null) return Collections.emptySet();
        return DEFAULTS.getOrDefault(role, Collections.emptySet());
    }

    public static boolean isDefaultAllowed(OrgRole role, String permission) {
        return getDefaults(role).contains(permission);
    }
}
