package com.axsoluciones.facturaTelegramBot.repository;

import com.axsoluciones.facturaTelegramBot.entity.SoftwareService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SoftwareRepository extends JpaRepository<SoftwareService, Long>{

    List<SoftwareService> findByExpiredDate(LocalDate date);
}
