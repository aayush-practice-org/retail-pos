package io.aygh.identity.service.sidebar;

import io.aygh.identity.dto.response.SidebarGroupResponse;

import java.util.List;

public interface SidebarService {

    List<SidebarGroupResponse> currentUserSidebar();

}
