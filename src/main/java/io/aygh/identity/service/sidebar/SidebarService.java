package io.aygh.identity.service.sidebar;

import io.aygh.identity.dto.response.sidebar.SidebarGroupResponse;
import io.aygh.identity.entity.SidebarApp;

import java.util.List;

public interface SidebarService {

    /**
     * @param app the front-end asking — {@code null} for every group the signed-in
     *            user can reach, whichever app it belongs to
     */
    List<SidebarGroupResponse> getSidebarForCurrentUser(SidebarApp app);
}
