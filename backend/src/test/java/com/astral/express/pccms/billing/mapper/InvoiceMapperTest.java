package com.astral.express.pccms.billing.mapper;

import com.astral.express.pccms.billing.dto.response.InvoiceLineResponse;
import com.astral.express.pccms.billing.entity.Invoice;
import com.astral.express.pccms.billing.entity.InvoiceLine;
import com.astral.express.pccms.boarding.entity.ServiceOrder;
import com.astral.express.pccms.medicine.entity.Medicine;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceMapperTest {

    private final InvoiceMapper mapper = Mappers.getMapper(InvoiceMapper.class);

    @Test
    void should_MapInvoiceLine_when_MedicineIsProvided() {
        // GIVEN
        UUID invoiceId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();
        UUID serviceOrderId = UUID.randomUUID();

        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);

        ServiceOrder serviceOrder = new ServiceOrder();
        serviceOrder.setId(serviceOrderId);

        Medicine medicine = new Medicine();
        medicine.setId(medicineId);

        InvoiceLine line = new InvoiceLine();
        line.setId(UUID.randomUUID());
        line.setInvoice(invoice);
        line.setServiceOrder(serviceOrder);
        line.setMedicine(medicine);
        line.setDescription("Vaccine shot");
        line.setQuantity(BigDecimal.valueOf(2));
        line.setUnitPriceVnd(BigDecimal.valueOf(100000));
        line.setSubtotalVnd(BigDecimal.valueOf(200000));

        // WHEN
        InvoiceLineResponse response = mapper.toLineResponse(line);

        // THEN
        assertThat(response.invoiceId()).isEqualTo(invoiceId);
        assertThat(response.serviceOrderId()).isEqualTo(serviceOrderId);
        assertThat(response.medicineId()).isEqualTo(medicineId);
        assertThat(response.description()).isEqualTo("Vaccine shot");
    }
}
