package com.axsoluciones.facturaTelegramBot.repository;

import com.axsoluciones.facturaTelegramBot.entity.BillState;
import com.axsoluciones.facturaTelegramBot.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ServiceRepository extends JpaRepository<Service, Long> {
    List<Service> findByBillState(BillState billState);

    List<Service> findByBillStateAndInvoiceDeadlineGreaterThanEqual(BillState billState, LocalDate date);
}
