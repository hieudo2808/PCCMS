package com.astral.express.pccms.appointment.repository;

import com.astral.express.pccms.appointment.entity.ServiceOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, java.util.UUID> {

    @Query(value = """
            SELECT COALESCE(MAX(CAST(SUBSTRING(order_code FROM 3) AS INTEGER)), 0)
            FROM service_orders
            WHERE order_code ~ '^AP[0-9]+$'
            """, nativeQuery = true)
    long maxAppointmentOrderSequence();

    boolean existsByService_Id(java.util.UUID serviceId);
}
