package recipehub.category.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import recipehub.category.dto.CreateCategoryRequest;
import recipehub.category.entity.Category;
import recipehub.category.repository.CategoryRepository;
import recipehub.common.exception.DuplicateResourceException;
import recipehub.common.exception.ResourceNotFoundException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock CategoryRepository repository;
    @InjectMocks CategoryService service;
    CreateCategoryRequest request;

    @BeforeEach void setup() {
        request = new CreateCategoryRequest();
        request.setName("Món Việt");
        request.setDescription("Ẩm thực Việt");
    }

    @Test void listReturnsAll() {
        when(repository.findAll()).thenReturn(List.of(new Category("A", "B")));
        assertEquals(1, service.getAllCategories().size());
    }

    @Test void createSuccessfullySaves() {
        when(repository.save(any(Category.class))).thenAnswer(call -> call.getArgument(0));
        Category result = service.createCategory(request);
        assertEquals("Món Việt", result.getName());
        assertEquals("Ẩm thực Việt", result.getDescription());
    }

    @Test void createDuplicateIsConflict() {
        when(repository.existsByName("Món Việt")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> service.createCategory(request));
        verify(repository, never()).save(any());
    }

    @Test void getFound() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Category("A", "B")));
        assertEquals("A", service.getCategoryById(1L).getName());
    }

    @Test void getMissing() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getCategoryById(1L));
    }

    @Test void updateNameWithoutDuplicate() {
        Category existing = new Category("Cũ", "x");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        assertEquals("Món Việt", service.updateCategory(1L, request).getName());
        verify(repository).existsByName("Món Việt");
    }

    @Test void updateUnchangedNameSkipsDuplicateCheck() {
        Category existing = new Category("Món Việt", "x");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        assertEquals("Ẩm thực Việt", service.updateCategory(1L, request).getDescription());
        verify(repository, never()).existsByName(any());
    }

    @Test void updateDuplicateThrows() {
        Category existing = new Category("Cũ", "x");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByName("Món Việt")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> service.updateCategory(1L, request));
    }

    @Test void updateMissingThrows() {
        assertThrows(ResourceNotFoundException.class, () -> service.updateCategory(1L, request));
    }

    @Test void deleteFound() {
        Category existing = new Category("Cũ", "x");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        service.deleteCategory(1L);
        verify(repository).delete(existing);
    }

    @Test void deleteMissingThrows() {
        assertThrows(ResourceNotFoundException.class, () -> service.deleteCategory(1L));
    }
}
