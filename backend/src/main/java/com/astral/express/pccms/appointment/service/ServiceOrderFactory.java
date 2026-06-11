package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.appointment.entity.ServiceCatalog;
import com.astral.express.pccms.appointment.entity.ServiceCategory;
import com.astral.express.pccms.appointment.entity.ServiceOrder;
import com.astral.express.pccms.appointment.entity.ServiceOrderStatus;
import com.astral.express.pccms.appointment.repository.ServiceOrderRepository;
import com.astral.express.pccms.pet.entity.Pets;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ServiceOrderFactory {

    private final ServiceOrderRepository serviceOrderRepository;

    public ServiceOrder createServiceOrder(Pets pet, ServiceCatalog service, UUID createdBy,
                                           OffsetDateTime startAt, OffsetDateTime endAt,
                                           ServiceCategory category) {
        ServiceOrder order = new ServiceOrder();
        order.setOrderCode(generateAppointmentCode());
        order.setOwner(pet.getOwner());
        order.setPet(pet);
        order.setService(service);
        order.setCategoryCode(category);
        order.setStatusCode(ServiceOrderStatus.REQUESTED);
        order.setRequestedAt(ClinicDateTime.now());
        order.setPlannedStartAt(startAt);
        order.setPlannedEndAt(endAt);
        order.setBaseAmountVnd(service.getBasePriceVnd());
        order.setCreatedBy(createdBy);
        return order;
    }

    private String generateAppointmentCode() {
        long seq = serviceOrderRepository.maxAppointmentOrderSequence() + 1;
        return String.format("AP%04d", seq);
    }
}
