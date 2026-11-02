package recipehub.category.service;

import org.springframework.stereotype.Service;
import recipehub.category.entity.Category;
import recipehub.category.repository.CategoryRepository;
import recipehub.category.dto.CreateCategoryRequest;
import recipehub.common.exception.DuplicateResourceException;
import recipehub.common.exception.ResourceNotFoundException;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category createCategory(CreateCategoryRequest request) {

        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException(
                    "Tên danh mục đã tồn tại");
        }

        Category category = new Category(
                request.getName(),
                request.getDescription());

        return categoryRepository.save(category);
    }

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy danh mục với ID: " + id));
    }

    public Category updateCategory(
            Long id,
            CreateCategoryRequest request) {

        Category category = getCategoryById(id);

        if (!category.getName().equals(request.getName())
                && categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException(
                    "Tên danh mục đã tồn tại");
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());

        return categoryRepository.save(category);
    }

    public void deleteCategory(Long id) {

        Category category = getCategoryById(id);

        categoryRepository.delete(category);
        categoryRepository.flush();
    }
}