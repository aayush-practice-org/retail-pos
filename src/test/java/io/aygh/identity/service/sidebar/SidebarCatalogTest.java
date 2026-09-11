package io.aygh.identity.service.sidebar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SidebarCatalogTest {

    @Test
    void validateShouldPassWithoutExceptions() {
        assertDoesNotThrow(SidebarCatalog::validate);
    }

    @Test
    void masterCatalogShouldNotBeEmpty() {
        assertFalse(SidebarCatalog.master().isEmpty());
    }
}
