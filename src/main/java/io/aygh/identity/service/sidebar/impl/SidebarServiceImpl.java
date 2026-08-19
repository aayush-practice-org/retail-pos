package io.aygh.identity.service.sidebar.impl;

import io.aygh.config.DynamicRbacService;
import io.aygh.exception.BusinessException;
import io.aygh.exception.UserNotFoundException;
import io.aygh.identity.dto.response.sidebar.SidebarGroupResponse;
import io.aygh.identity.dto.response.sidebar.SidebarItemResponse;
import io.aygh.identity.dto.response.sidebar.SidebarSubItemResponse;
import io.aygh.identity.entity.SidebarApp;
import io.aygh.identity.entity.SidebarMenu;
import io.aygh.identity.entity.UserRole;
import io.aygh.identity.service.sidebar.SidebarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/*
 *  Builds the navigation for whoever is signed in.
 *
 *  The master menu below is the whole of both front-ends; a request gets back the
 *  slice of it their role reaches. Nothing here touches the database — visibility
 *  comes from SidebarApp (which front-end a role belongs to) and SidebarMenu
 *  (which modules that role may open), and SidebarMenu is the very same table the
 *  @PreAuthorize("@rbac.canAccess('...')") guards read. What a role can see and
 *  what it can call therefore cannot drift apart.
 */
@Service
@Slf4j
@RequiredArgsConstructor
class SidebarServiceImpl implements SidebarService {

    private final DynamicRbacService rbacService;

    @Override
    public List<SidebarGroupResponse> getSidebarForCurrentUser(SidebarApp app) {
        UserRole role = rbacService.getCurrentUserRole();

        if (role == null) {
            throw new UserNotFoundException("No authenticated user on this request. Try logging in again.");
        }

        if (app != null && !app.isOpenTo(role)) {
            log.warn("Role {} asked for the {} sidebar it has no access to", role, app);
            throw new BusinessException("A %s has no access to the %s app".formatted(role, app.getTitle()));
        }

        return MENU.stream()
                .filter(group -> app == null ? group.app().isOpenTo(role) : group.app() == app)
                .map(group -> new SidebarGroupResponse(group.title(), visibleItems(group, role)))
                .filter(group -> !group.items().isEmpty())
                .toList();
    }

    private List<SidebarItemResponse> visibleItems(MenuGroup group, UserRole role) {
        return group.items().stream()
                .filter(item -> item.menu().hasAccess(role))
                .map(MenuItem::toResponse)
                .toList();
    }

    // ── The master menu ───────────────────────────────────────────────────

    private static final List<MenuGroup> MENU = List.of(

            /*
             *  The counter — the selling app the till runs on. A cashier signs in
             *  here and gets their own dashboard, the sale screen and payments;
             *  the back-office groups below never reach them.
             */
            new MenuGroup(SidebarApp.COUNTER, "Counter", List.of(
                    new MenuItem(SidebarMenu.DASHBOARD, "Dashboard", "/sell/dashboard", "LayoutDashboard", null),
                    new MenuItem(SidebarMenu.POS, "New Sale", "/sell/pos", "ScanBarcode", List.of(
                            new SidebarSubItemResponse("Open Till", "/sell/pos"),
                            new SidebarSubItemResponse("Held Sales", "/sell/pos/held")
                    )),
                    new MenuItem(SidebarMenu.PAYMENTS, "Payments", "/sell/payments", "CreditCard", List.of(
                            new SidebarSubItemResponse("Collect Payment", "/sell/payments/collect"),
                            new SidebarSubItemResponse("Transactions", "/sell/payments/transactions"),
                            new SidebarSubItemResponse("Day Close", "/sell/payments/day-close")
                    )),
                    new MenuItem(SidebarMenu.SALES, "Sales History", "/sell/sales", "Receipt", null)
            )),

            // The back office — everything the mart is run from
            new MenuGroup(SidebarApp.BACK_OFFICE, "Operations", List.of(
                    new MenuItem(SidebarMenu.DASHBOARD, "Dashboard", "/admin/dashboard", "LayoutDashboard", null),
                    new MenuItem(SidebarMenu.SALES, "Sales", "/admin/sales", "Receipt", List.of(
                            new SidebarSubItemResponse("All Sales", "/admin/sales"),
                            new SidebarSubItemResponse("Returns", "/admin/sales/returns")
                    )),
                    new MenuItem(SidebarMenu.PAYMENTS, "Payments", "/admin/payments", "CreditCard", List.of(
                            new SidebarSubItemResponse("Transactions", "/admin/payments"),
                            new SidebarSubItemResponse("Day Close", "/admin/payments/day-close")
                    ))
            )),

            new MenuGroup(SidebarApp.BACK_OFFICE, "Catalog", List.of(
                    new MenuItem(SidebarMenu.PRODUCTS, "Products", "/admin/products", "Package", List.of(
                            new SidebarSubItemResponse("All Products", "/admin/products"),
                            new SidebarSubItemResponse("New Product", "/admin/products/new")
                    )),
                    new MenuItem(SidebarMenu.CATEGORIES, "Categories", "/admin/categories", "FolderTree", null),
                    new MenuItem(SidebarMenu.UNITS, "Units", "/admin/units", "Scale", null)
            )),

            new MenuGroup(SidebarApp.BACK_OFFICE, "Insights", List.of(
                    new MenuItem(SidebarMenu.REPORTS, "Reports", "/admin/reports", "ChartColumn", List.of(
                            new SidebarSubItemResponse("Sales Report", "/admin/reports/sales"),
                            new SidebarSubItemResponse("Payment Report", "/admin/reports/payments"),
                            new SidebarSubItemResponse("Stock Report", "/admin/reports/stock")
                    ))
            )),

            new MenuGroup(SidebarApp.BACK_OFFICE, "Administration", List.of(
                    new MenuItem(SidebarMenu.STAFF, "Staff", "/admin/staff", "Users", null)
            ))
    );

    /**
     * A group belongs to exactly one front-end, which is what keeps a cashier out
     * of the back office even for modules both apps share.
     */
    private record MenuGroup(SidebarApp app, String title, List<MenuItem> items) {
    }

    /**
     * Keyed by the {@link SidebarMenu} itself rather than its name, so a menu key
     * that does not exist cannot be typed into the master menu at all.
     */
    private record MenuItem(SidebarMenu menu,
                            String name,
                            String path,
                            String icon,
                            List<SidebarSubItemResponse> subItems) {

        SidebarItemResponse toResponse() {
            return new SidebarItemResponse(name, path, icon, menu.name(), subItems);
        }
    }
}
