package com.astral.express.pccms.billing.mapper;

import com.astral.express.pccms.billing.dto.response.InvoiceLineResponse;
import com.astral.express.pccms.billing.dto.response.InvoiceResponse;
import com.astral.express.pccms.billing.entity.Invoice;
import com.astral.express.pccms.billing.entity.InvoiceLine;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InvoiceMapper {

    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "petId", source = "pet.id")
    InvoiceResponse toResponse(Invoice invoice);

    @Mapping(target = "invoiceId", source = "invoice.id")
    @Mapping(target = "serviceOrderId", source = "serviceOrder.id")
    @Mapping(target = "medicineId", source = "medicine.id")
    InvoiceLineResponse toLineResponse(InvoiceLine line);
}
