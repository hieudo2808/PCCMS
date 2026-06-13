package com.astral.express.pccms.medicine.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.medicine.dto.request.MedicineCategoryCreateRequest;
import com.astral.express.pccms.medicine.dto.request.MedicineCategoryUpdateRequest;
import com.astral.express.pccms.medicine.entity.MedicineCategory;
import com.astral.express.pccms.medicine.repository.MedicineCategoryRepository;
import com.astral.express.pccms.medicine.repository.MedicineRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MedicineCategoryServiceTest {

    @Mock
    private MedicineCategoryRepository categoryRepository;

    @Mock
    private MedicineRepository medicineRepository;

    @InjectMocks
    private MedicineCategoryService categoryService;

    @Test
    void should_create_success() {
        MedicineCategoryCreateRequest request = new MedicineCategoryCreateRequest("Name", "Desc", true);
        given(categoryRepository.existsByName("Name")).willReturn(false);
        given(categoryRepository.save(any(MedicineCategory.class))).willAnswer(inv -> {
            MedicineCategory cat = inv.getArgument(0);
            cat.setId(UUID.randomUUID());
            return cat;
        });

        var res = categoryService.create(request);
        assertThat(res.name()).isEqualTo("Name");
    }

    @Test
    void should_create_throwExists() {
        MedicineCategoryCreateRequest request = new MedicineCategoryCreateRequest("Name", "Desc", true);
        given(categoryRepository.existsByName("Name")).willReturn(true);

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_MED_009_CATEGORY_NAME_EXISTS);
    }

    @Test
    void should_update_success() {
        UUID id = UUID.randomUUID();
        MedicineCategoryUpdateRequest request = new MedicineCategoryUpdateRequest("New Name", "Desc", true);
        MedicineCategory cat = new MedicineCategory();
        cat.setId(id);
        cat.setName("Old Name");

        given(categoryRepository.findById(id)).willReturn(Optional.of(cat));
        given(categoryRepository.existsByNameAndIdNot("New Name", id)).willReturn(false);
        given(categoryRepository.save(any(MedicineCategory.class))).willAnswer(inv -> inv.getArgument(0));

        var res = categoryService.update(id, request);
        assertThat(res.name()).isEqualTo("New Name");
    }

    @Test
    void should_update_throwNotFound() {
        UUID id = UUID.randomUUID();
        MedicineCategoryUpdateRequest request = new MedicineCategoryUpdateRequest("New Name", "Desc", true);
        given(categoryRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.update(id, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_MED_006_MEDICINE_CATEGORY_NOT_FOUND);
    }

    @Test
    void should_update_throwExists() {
        UUID id = UUID.randomUUID();
        MedicineCategoryUpdateRequest request = new MedicineCategoryUpdateRequest("New Name", "Desc", true);
        MedicineCategory cat = new MedicineCategory();
        cat.setId(id);
        given(categoryRepository.findById(id)).willReturn(Optional.of(cat));
        given(categoryRepository.existsByNameAndIdNot("New Name", id)).willReturn(true);

        assertThatThrownBy(() -> categoryService.update(id, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_MED_009_CATEGORY_NAME_EXISTS);
    }

    @Test
    void should_getById_success() {
        UUID id = UUID.randomUUID();
        MedicineCategory cat = new MedicineCategory();
        cat.setId(id);
        cat.setName("Name");
        given(categoryRepository.findById(id)).willReturn(Optional.of(cat));

        var res = categoryService.getById(id);
        assertThat(res.name()).isEqualTo("Name");
    }

    @Test
    void should_getById_throwNotFound() {
        UUID id = UUID.randomUUID();
        given(categoryRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getById(id))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_MED_006_MEDICINE_CATEGORY_NOT_FOUND);
    }

    @Test
    void should_listAll() {
        MedicineCategory cat = new MedicineCategory();
        cat.setId(UUID.randomUUID());
        cat.setName("Name");
        given(categoryRepository.findAllByOrderByNameAsc()).willReturn(List.of(cat));

        var res = categoryService.listAll();
        assertThat(res).hasSize(1);
    }

    @Test
    void should_listActive() {
        MedicineCategory cat = new MedicineCategory();
        cat.setId(UUID.randomUUID());
        cat.setName("Name");
        given(categoryRepository.findByIsActiveTrueOrderByNameAsc()).willReturn(List.of(cat));

        var res = categoryService.listActive();
        assertThat(res).hasSize(1);
    }

    @Test
    void should_delete_success() {
        UUID id = UUID.randomUUID();
        MedicineCategory cat = new MedicineCategory();
        cat.setId(id);
        cat.setIsActive(true);
        given(categoryRepository.findById(id)).willReturn(Optional.of(cat));
        given(medicineRepository.existsByCategoryId(id)).willReturn(false);

        categoryService.delete(id);

        verify(categoryRepository).save(cat);
        assertThat(cat.getIsActive()).isFalse();
    }

    @Test
    void should_delete_throwNotFound() {
        UUID id = UUID.randomUUID();
        given(categoryRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.delete(id))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_MED_006_MEDICINE_CATEGORY_NOT_FOUND);
    }

    @Test
    void should_delete_throwInUse() {
        UUID id = UUID.randomUUID();
        MedicineCategory cat = new MedicineCategory();
        cat.setId(id);
        given(categoryRepository.findById(id)).willReturn(Optional.of(cat));
        given(medicineRepository.existsByCategoryId(id)).willReturn(true);

        assertThatThrownBy(() -> categoryService.delete(id))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_MED_010_CATEGORY_IN_USE);
    }
}
