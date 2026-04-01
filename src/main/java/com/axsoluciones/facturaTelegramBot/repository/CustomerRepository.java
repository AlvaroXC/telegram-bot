package com.axsoluciones.facturaTelegramBot.repository;

import com.axsoluciones.facturaTelegramBot.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByPhoneNumber(String phoneNumber);

}
