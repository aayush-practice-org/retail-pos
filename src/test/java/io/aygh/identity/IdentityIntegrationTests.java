package io.aygh.identity;

import io.aygh.exception.BusinessException;
import io.aygh.identity.dto.request.ChangePasswordRequest;
import io.aygh.identity.dto.request.StaffCreateRequest;
import io.aygh.identity.dto.request.StaffUpdateRequest;
import io.aygh.identity.dto.response.RoleOptionResponse;
import io.aygh.identity.dto.response.StaffResponse;
import io.aygh.identity.dto.response.sidebar.SidebarGroupResponse;
import io.aygh.identity.dto.response.sidebar.SidebarItemResponse;
import io.aygh.identity.entity.SidebarApp;
import io.aygh.identity.entity.User;
import io.aygh.identity.entity.UserRole;
import io.aygh.identity.repository.UserRepository;
import io.aygh.identity.service.command.StaffCommandService;
import io.aygh.identity.service.query.SelfQueryService;
import io.aygh.identity.service.query.StaffQueryService;
import io.aygh.identity.service.sidebar.SidebarService;
import io.aygh.security.context.UserHolder;
import io.aygh.shared.response.PagedResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class IdentityIntegrationTests {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private StaffCommandService staffCommandService;
    @Autowired
    private StaffQueryService staffQueryService;
    @Autowired
    private SelfQueryService selfQueryService;
    @Autowired
    private SidebarService sidebarService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private User owner;

    @BeforeEach
    void seedOwner() {
        userRepository.deleteAll();
        owner = signIn(UserRole.ADMIN, "owner");
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
        UserHolder.clear();
    }

    // ── Staff operations ──────────────────────────────────────────────────

    @Test
    void anAdminHiresStaffAndFindsThemAgain() {
        StaffResponse cashier = staffCommandService.create(request("till", UserRole.CASHIER));

        assertThat(cashier.role()).isEqualTo("CASHIER");
        assertThat(cashier.isActive()).isTrue();

        assertThat(staffQueryService.findById(cashier.id()).username()).isEqualTo(cashier.username());

        PagedResponse<StaffResponse> cashiers =
                staffQueryService.findAll(null, UserRole.CASHIER, null, PageRequest.of(0, 20));

        assertThat(cashiers.getContent()).extracting(StaffResponse::id).containsExactly(cashier.id());
    }

    @Test
    void theStaffListSearchesNameEmailAndUsername() {
        StaffResponse keeper = staffCommandService.create(
                request("shelves", UserRole.STOREKEEPER, "Bishnu Thapa"));
        staffCommandService.create(request("till", UserRole.CASHIER));

        PagedResponse<StaffResponse> found =
                staffQueryService.findAll("bishnu", null, null, PageRequest.of(0, 20));

        assertThat(found.getContent()).extracting(StaffResponse::id).containsExactly(keeper.id());
    }

    @Test
    void aUsernameOrEmailIsNeverIssuedTwice() {
        StaffCreateRequest first = request("till", UserRole.CASHIER);
        staffCommandService.create(first);

        assertThatThrownBy(() -> staffCommandService.create(first))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Username already taken");
    }

    @Test
    void anUpdateTouchesOnlyTheFieldsItCarries() {
        StaffResponse cashier = staffCommandService.create(request("till", UserRole.CASHIER));

        StaffResponse updated = staffCommandService.update(cashier.id(), new StaffUpdateRequest(
                null, "Sabina Rai", null, "9800000000", null, null, null));

        assertThat(updated.fullName()).isEqualTo("Sabina Rai");
        assertThat(updated.phone()).isEqualTo("9800000000");
        assertThat(updated.username()).isEqualTo(cashier.username());
        assertThat(updated.email()).isEqualTo(cashier.email());
        assertThat(updated.role()).isEqualTo("CASHIER");
    }

    @Test
    void anAdminResetsAForgottenPassword() {
        StaffResponse cashier = staffCommandService.create(request("till", UserRole.CASHIER));

        staffCommandService.update(cashier.id(), new StaffUpdateRequest(
                null, null, null, null, "brand-new-secret", null, null));

        User stored = userRepository.findById(cashier.id()).orElseThrow();
        assertThat(passwordEncoder.matches("brand-new-secret", stored.getPassword())).isTrue();
    }

    @Test
    void deactivatingKeepsTheAccountButShutsItOut() {
        StaffResponse cashier = staffCommandService.create(request("till", UserRole.CASHIER));

        assertThat(staffCommandService.setActive(cashier.id(), false).isActive()).isFalse();
        assertThat(userRepository.findById(cashier.id())).isPresent();
        assertThat(staffCommandService.setActive(cashier.id(), true).isActive()).isTrue();
    }

    /**
     * Soft delete: the row stays for the sales it signed, but the account is gone
     * from every listing. On Postgres the partial unique indexes then free the
     * username and email for reuse.
     */
    @Test
    void aDeletedStaffMemberDisappearsFromEveryListing() {
        StaffResponse cashier = staffCommandService.create(request("till", UserRole.CASHIER));

        staffCommandService.delete(cashier.id());

        assertThat(userRepository.findById(cashier.id())).isEmpty();
        assertThat(staffQueryService.findAll(null, null, null, PageRequest.of(0, 20)).getContent())
                .extracting(StaffResponse::id)
                .doesNotContain(cashier.id());
    }

    @Test
    void nobodyLocksThemselvesOut() {
        assertThatThrownBy(() -> staffCommandService.delete(owner.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot delete your own account");
    }

    @Test
    void theLastOwnerCannotBeDemoted() {
        User second = signIn(UserRole.ADMIN, "second-owner");
        signInAs(owner);

        // Two owners: demoting one is fine
        assertThat(staffCommandService.update(second.getId(), new StaffUpdateRequest(
                null, null, null, null, null, UserRole.MANAGER, null)).role()).isEqualTo("MANAGER");

        // One left: the mart would have nobody who can let anyone back in
        assertThatThrownBy(() -> staffCommandService.update(owner.getId(), new StaffUpdateRequest(
                null, null, null, null, null, UserRole.MANAGER, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("only ADMIN");
    }

    @Test
    void aManagerStaffsTheFloorButMintsNoSeniors() {
        User manager = signIn(UserRole.MANAGER, "floor-manager");
        signInAs(manager);

        assertThat(staffCommandService.create(request("till", UserRole.CASHIER)).role()).isEqualTo("CASHIER");

        assertThatThrownBy(() -> staffCommandService.create(request("rival", UserRole.ADMIN)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Only an ADMIN can assign");

        assertThatThrownBy(() -> staffCommandService.delete(owner.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Only an ADMIN can manage");
    }

    @Test
    void theRolePickerShowsOnlyWhatTheAskerMayHandOut() {
        assertThat(staffQueryService.assignableRoles())
                .extracting(RoleOptionResponse::value)
                .containsExactlyInAnyOrder("ADMIN", "MANAGER", "CASHIER", "STOREKEEPER", "ACCOUNTANT");

        signInAs(signIn(UserRole.MANAGER, "floor-manager"));

        List<RoleOptionResponse> forManager = staffQueryService.assignableRoles();
        assertThat(forManager).extracting(RoleOptionResponse::value)
                .containsExactlyInAnyOrder("CASHIER", "STOREKEEPER", "ACCOUNTANT");
        assertThat(forManager).filteredOn(role -> role.value().equals("CASHIER")).singleElement()
                .satisfies(cashier -> {
                    assertThat(cashier.label()).isEqualTo("Cashier");
                    assertThat(cashier.apps()).containsExactly("Counter");
                    assertThat(cashier.modules()).contains("Point of Sale", "Payments");
                });
    }

    // ── Sidebar ───────────────────────────────────────────────────────────

    @Test
    void aCashierGetsTheCounterAndNothingElse() {
        signInAs(signIn(UserRole.CASHIER, "till-hand"));

        List<SidebarGroupResponse> sidebar = sidebarService.getSidebarForCurrentUser(null);

        assertThat(sidebar).extracting(SidebarGroupResponse::title).containsExactly("Counter");
        assertThat(sidebar.getFirst().items())
                .extracting(SidebarItemResponse::menuKey)
                .containsExactly("DASHBOARD", "POS", "PAYMENTS", "SALES");
        assertThat(sidebar.getFirst().items())
                .extracting(SidebarItemResponse::path)
                .allSatisfy(path -> assertThat(path).startsWith("/sell/"));
    }

    @Test
    void theCashiersPaymentsScreenIsThere() {
        signInAs(signIn(UserRole.CASHIER, "till-hand"));

        SidebarItemResponse payments = sidebarService.getSidebarForCurrentUser(SidebarApp.COUNTER)
                .getFirst().items().stream()
                .filter(item -> item.menuKey().equals("PAYMENTS"))
                .findFirst()
                .orElseThrow();

        assertThat(payments.path()).isEqualTo("/sell/payments");
        assertThat(payments.subItems()).extracting("name")
                .contains("Collect Payment", "Transactions");
    }

    @Test
    void anOwnerWalksBothTheFloorAndTheTill() {
        List<SidebarGroupResponse> sidebar = sidebarService.getSidebarForCurrentUser(null);

        assertThat(sidebar).extracting(SidebarGroupResponse::title)
                .containsExactly("Counter", "Operations", "Catalog", "Insights", "Administration");

        assertThat(sidebarService.getSidebarForCurrentUser(SidebarApp.BACK_OFFICE))
                .extracting(SidebarGroupResponse::title)
                .doesNotContain("Counter");
    }

    @Test
    void aStorekeeperSeesTheShelvesAndNotTheMoney() {
        signInAs(signIn(UserRole.STOREKEEPER, "shelves"));

        List<String> visible = sidebarService.getSidebarForCurrentUser(null).stream()
                .flatMap(group -> group.items().stream())
                .map(SidebarItemResponse::menuKey)
                .toList();

        assertThat(visible).contains("PRODUCTS", "CATEGORIES", "UNITS");
        assertThat(visible).doesNotContain("PAYMENTS", "SALES", "STAFF", "POS");
    }

    @Test
    void aStorekeeperCannotOpenTheCounter() {
        signInAs(signIn(UserRole.STOREKEEPER, "shelves"));

        assertThatThrownBy(() -> sidebarService.getSidebarForCurrentUser(SidebarApp.COUNTER))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no access to the Counter app");
    }

    // ── Self ──────────────────────────────────────────────────────────────

    @Test
    void changingYourOwnPasswordRequiresTheCurrentOne() {
        assertThatThrownBy(() -> selfQueryService.changePassword(
                new ChangePasswordRequest("not-my-password", "a-new-one")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Current password doesn't match");

        selfQueryService.changePassword(new ChangePasswordRequest("secret", "a-new-one"));

        User stored = userRepository.findById(owner.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("a-new-one", stored.getPassword())).isTrue();
    }

    @Test
    void selfIsWhoeverHoldsTheToken() {
        assertThat(selfQueryService.getSelfInfo().username()).isEqualTo(owner.getUsername());

        User cashier = signIn(UserRole.CASHIER, "till-hand");
        signInAs(cashier);

        assertThat(selfQueryService.getSelfInfo().username()).isEqualTo(cashier.getUsername());
    }

    // ── Fixtures ──────────────────────────────────────────────────────────

    private StaffCreateRequest request(String name, UserRole role) {
        return request(name, role, null);
    }

    private StaffCreateRequest request(String name, UserRole role, String fullName) {
        int seq = SEQ.incrementAndGet();
        return new StaffCreateRequest(
                name + "-" + seq,
                fullName,
                name + seq + "@mart.local",
                null,
                "secret",
                role);
    }

    /** Saves a user straight to the repository and signs in as them. */
    private User signIn(UserRole role, String name) {
        int seq = SEQ.incrementAndGet();

        User user = userRepository.save(User.builder()
                .username(name + "-" + seq)
                .email(name + seq + "@mart.local")
                .password(passwordEncoder.encode("secret"))
                .role(role)
                .isActive(true)
                .build());

        signInAs(user);
        return user;
    }

    private void signInAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        UserHolder.setUserId(user.getId());
        UserHolder.setUsername(user.getUsername());
        UserHolder.setRole(user.getRole().name());
    }
}
