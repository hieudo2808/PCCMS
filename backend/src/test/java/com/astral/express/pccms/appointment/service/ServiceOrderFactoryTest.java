package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.appointment.entity.ServiceCatalog;
import com.astral.express.pccms.appointment.entity.ServiceCategory;
import com.astral.express.pccms.appointment.entity.ServiceOrder;
import com.astral.express.pccms.appointment.entity.ServiceOrderStatus;
import com.astral.express.pccms.appointment.repository.ServiceOrderRepository;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.user.entity.Users;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ServiceOrderFactoryTest {

    @Mock
    private ServiceOrderRepository serviceOrderRepository;

    @InjectMocks
    private ServiceOrderFactory factory;

    @Test
    void createServiceOrder_shouldReturnConfiguredOrder() {
        Pets pet = new Pets();
        Users owner = new Users();
        pet.setOwner(owner);

        ServiceCatalog service = new ServiceCatalog();
        service.setBasePriceVnd(100000L);

        UUID createdBy = UUID.randomUUID();
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusMinutes(30);

        given(serviceOrderRepository.maxAppointmentOrderSequence()).willReturn(42L);

        ServiceOrder result = factory.createServiceOrder(pet, service, createdBy, start, end, ServiceCategory.MEDICAL);

        assertThat(result.getOrderCode()).isEqualTo("AP0043");
        assertThat(result.getOwner()).isEqualTo(owner);
        assertThat(result.getPet()).isEqualTo(pet);
        assertThat(result.getService()).isEqualTo(service);
        assertThat(result.getCategoryCode()).isEqualTo(ServiceCategory.MEDICAL);
        assertThat(result.getStatusCode()).isEqualTo(ServiceOrderStatus.REQUESTED);
        assertThat(result.getRequestedAt()).isNotNull();
        assertThat(result.getPlannedStartAt()).isEqualTo(start);
        assertThat(result.getPlannedEndAt()).isEqualTo(end);
        assertThat(result.getBaseAmountVnd()).isEqualTo(100000L);
        assertThat(result.getCreatedBy()).isEqualTo(createdBy);
    }
}
