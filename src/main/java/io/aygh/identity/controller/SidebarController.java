package io.aygh.identity.controller;

import io.aygh.identity.dto.response.sidebar.SidebarGroupResponse;
import io.aygh.identity.entity.SidebarApp;
import io.aygh.identity.service.sidebar.SidebarService;
import io.aygh.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
 *  The navigation for whoever is signed in, built from their role.
 *
 *  Both front-ends call this on load. The selling app asks for ?app=COUNTER and
 *  gets the till's nav — dashboard, sale screen, payments; the back office asks
 *  for ?app=BACK_OFFICE, or for everything at once when the same person runs both.
 */
@RestController
@RequestMapping("/api/sidebar")
@RequiredArgsConstructor
@Slf4j
public class SidebarController {

    private final SidebarService sidebarService;

    /**
     * @param app which front-end is asking; omit for every group the user can reach
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SidebarGroupResponse>>> getSidebar(
            @RequestParam(required = false) SidebarApp app) {
        log.info("REST request to get sidebar for app: {}", app);
        List<SidebarGroupResponse> sidebar = sidebarService.getSidebarForCurrentUser(app);
        return ResponseEntity.ok(ApiResponse.ok("Sidebar fetched successfully", sidebar));
    }
}
