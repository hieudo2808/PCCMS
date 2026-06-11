package com.astral.express.pccms.billing.mapper;

import com.astral.express.pccms.appointment.entity.ServiceOrder;
import com.astral.express.pccms.billing.dto.response.InvoiceLineResponse;
import com.astral.express.pccms.billing.dto.response.InvoiceResponse;
import com.astral.express.pccms.billing.entity.Invoice;
import com.astral.express.pccms.billing.entity.InvoiceLine;
import com.astral.express.pccms.billing.entity.InvoiceStatus;
import com.astral.express.pccms.medicine.entity.Medicine;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.user.entity.Users;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceMapperTest {

    private final InvoiceMapper mapper = new InvoiceMapperImpl();

    @Test
    void TC_BILL_MAP_001_should_MapInvoiceToResponse_when_AllFieldsArePresent() {
        // GIVEN
        UUID invoiceId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        Users owner = new Users();
        owner.setId(ownerId);

        Pets pet = new Pets();
        pet.setId(petId);

        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setInvoiceCode("INV-TEST");
        invoice.setOwner(owner);
        invoice.setPet(pet);
        invoice.setStatusCode(InvoiceStatus.UNPAID);
        invoice.setTotalAmountVnd(5000L);
        invoice.setPaidAmountVnd(1000L);
        invoice.setNote("Test note");

        // WHEN
        InvoiceResponse response = mapper.toResponse(invoice);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(invoiceId);
        assertThat(response.invoiceCode()).isEqualTo("INV-TEST");
        assertThat(response.ownerId()).isEqualTo(ownerId);
        assertThat(response.petId()).isEqualTo(petId);
        assertThat(response.statusCode()).isEqualTo(InvoiceStatus.UNPAID);
        assertThat(response.totalAmountVnd()).isEqualTo(5000L);
        assertThat(response.paidAmountVnd()).isEqualTo(1000L);
        assertThat(response.note()).isEqualTo("Test note");
    }

    @Test
    void TC_BILL_MAP_002_should_MapInvoiceLineToResponse_when_AllFieldsArePresent() {
        // GIVEN
        UUID lineId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        UUID serviceOrderId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();

        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);

        ServiceOrder serviceOrder = new ServiceOrder();
        serviceOrder.setId(serviceOrderId);

        Medicine medicine = new Medicine();
        medicine.setId(medicineId);

        InvoiceLine line = new InvoiceLine();
        line.setId(lineId);
        line.setInvoice(invoice);
        line.setServiceOrder(serviceOrder);
        line.setMedicine(medicine);
        line.setDescription("Desc");
        line.setQuantity(new BigDecimal("2.5"));
        line.setUnitPriceVnd(100L);
        line.setLineOrder(1);

        // WHEN
        InvoiceLineResponse response = mapper.toLineResponse(line);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(lineId);
        assertThat(response.invoiceId()).isEqualTo(invoiceId);
        assertThat(response.serviceOrderId()).isEqualTo(serviceOrderId);
        assertThat(response.medicineId()).isEqualTo(medicineId);
        assertThat(response.description()).isEqualTo("Desc");
        assertThat(response.quantity()).isEqualTo(2);
        assertThat(response.unitPriceVnd()).isEqualTo(100L);
    }

    @Test
    void TC_BILL_MAP_003_should_ReturnNull_when_MappingNullEntity() {
        // WHEN
        InvoiceResponse response = mapper.toResponse(null);
        InvoiceLineResponse lineResponse = mapper.toLineResponse(null);

        // THEN
        assertThat(response).isNull();
        assertThat(lineResponse).isNull();
    }
}
