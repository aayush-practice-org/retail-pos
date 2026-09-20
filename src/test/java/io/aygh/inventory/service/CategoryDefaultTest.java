package io.aygh.inventory.service;

import io.aygh.exception.BusinessException;
import io.aygh.inventory.dto.response.CategorySummaryResponse;
import io.aygh.inventory.entity.Category;
import io.aygh.inventory.helper.InventoryResolver;
import io.aygh.inventory.helper.InventoryValidation;
import io.aygh.inventory.mapper.CategoryMapper;
import io.aygh.inventory.repository.CategoryRepository;
import io.aygh.inventory.repository.CategoryUnitRepository;
import io.aygh.inventory.service.command.impl.CategoryCommandServiceImpl;
import io.aygh.inventory.service.query.CategoryQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Moving the aisle that unfiled products land in.
 * <p>
 * Only one category may carry the flag, and a partial unique index enforces it,
 * so the order the two writes happen in is the whole behaviour.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CategoryDefaultTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private CategoryUnitRepository categoryUnitRepository;
    @Mock private InventoryResolver resolver;
    @Mock private InventoryValidation validation;
    @Mock private CategoryMapper categoryMapper;
    @Mock private CategoryQueryService categoryQueryService;

    private CategoryCommandServiceImpl service;

    private Category general;
    private Category beverages;

    @BeforeEach
    void setUp() {
        service = new CategoryCommandServiceImpl(
                categoryRepository, categoryUnitRepository, resolver,
                validation, categoryMapper, categoryQueryService);

        general = category(1L, "General", true);
        beverages = category(2L, "Beverages", false);

        when(resolver.category(1L)).thenReturn(general);
        when(resolver.category(2L)).thenReturn(beverages);
        when(categoryRepository.save(any(Category.class))).thenAnswer(call -> call.getArgument(0));
        when(categoryMapper.toSummary(any(Category.class))).thenReturn(new CategorySummaryResponse());
    }

    @Test
    void theOldDefaultIsReleasedBeforeTheNewOneClaimsIt() {
        service.makeDefault(2L);

        // Reversed, this violates the one-default index.
        InOrder order = inOrder(categoryRepository);
        order.verify(categoryRepository).clearDefault();
        order.verify(categoryRepository).save(beverages);

        assertTrue(beverages.isDefaultCategory());
    }

    @Test
    void theCategoryIsLoadedAgainAfterTheClear() {
        service.makeDefault(2L);

        // clearDefault detaches everything, so the instance saved must be one
        // fetched after it — not the one the method opened with.
        verify(resolver, times(2)).category(2L);
    }

    @Test
    void namingTheCategoryThatAlreadyHasItWritesNothing() {
        service.makeDefault(1L);

        verify(categoryRepository, never()).clearDefault();
        verify(categoryRepository, never()).save(any());
        assertTrue(general.isDefaultCategory());
    }

    @Test
    void deleteChecksTheCategoryIsNotTheDefault() {
        service.delete(2L);

        verify(validation).requireNotDefaultCategory(beverages);
        verify(categoryRepository).delete(beverages);
    }

    @Test
    void theDefaultCategoryCannotBeDeleted() {
        // The real check, rather than the mocked one the service talks to.
        // It reads one field and touches no repository, so the collaborators
        // are irrelevant to it.
        InventoryValidation real = new InventoryValidation(null, null, null, null, null, null);

        BusinessException thrown = assertThrows(BusinessException.class,
                () -> real.requireNotDefaultCategory(general));
        assertTrue(thrown.getMessage().contains("unfiled products go"));

        assertDoesNotThrow(() -> real.requireNotDefaultCategory(beverages));
    }

    private static Category category(Long id, String name, boolean isDefault) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setDefaultCategory(isDefault);
        return category;
    }
}
