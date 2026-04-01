package com.axsoluciones.facturaTelegramBot.service;

import com.axsoluciones.facturaTelegramBot.entity.*;
import com.axsoluciones.facturaTelegramBot.repository.CustomerRepository;
import com.axsoluciones.facturaTelegramBot.repository.ServiceRepository;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@org.springframework.stereotype.Service
public class BillService {

    private final CustomerRepository customerRepository;
    private final ServiceRepository serviceRepository;

    public BillService(CustomerRepository customerRepository, ServiceRepository serviceRepository){
        this.serviceRepository = serviceRepository;
        this.customerRepository = customerRepository;
    }

    public boolean customerExists(String phoneNumber){
        return customerRepository.findByPhoneNumber(phoneNumber).isPresent();
    }

    public String getCustomerName(String phoneNumber){
        return customerRepository.findByPhoneNumber(phoneNumber)
                .map(Customer::getName)
                .orElse("Desconocido");
    }

    @Transactional
    public void saveOrder(Map<String, String> data){
        String type = data.get("TYPE");
        String phone = data.get("PHONE");
        String name = data.get("NAME");
        Double price = Double.parseDouble(data.get("PRICE"));
        String detail = data.get("DETAIL");

        Customer customer = customerRepository.findByPhoneNumber(phone)
                .orElseGet(() -> {
                    Customer newCustomer = new Customer();
                    newCustomer.setPhoneNumber(phone);
                    newCustomer.setName(name);
                    return customerRepository.save(newCustomer);
                });

        com.axsoluciones.facturaTelegramBot.entity.Service service;

        switch (type) {
            case "Software" -> {
                SoftwareService sw = new SoftwareService();
                sw.setSoftwareName(detail);
                sw.setExpiredDate(LocalDate.parse(data.get("DATE")));
                service = sw;
            }
            case "Hardware" -> {
                HardwareService hw = new HardwareService();
                hw.setHardwareName(detail);
                service = hw;
            }
            case "Mantenimiento" -> {
                MaintenanceService mt = new MaintenanceService();
                mt.setDescription(detail);
                service = mt;
            }
            default -> throw new IllegalStateException("Unexpected value: " + type);
        }

        service.setCustomer(customer);
        service.setPrice(price);
        service.setHiringDate(LocalDate.now());
        service.setBillState(BillState.NO_FACTURADO);
        String invoiceDateStr = data.get("INVOICE_DATE");
        service.setInvoiceReminderDate(LocalDate.parse(invoiceDateStr));

        serviceRepository.save(service);
    }

    public List<Service> findUnbilledServices() {
        return serviceRepository.findByBillState(BillState.NO_FACTURADO);
    }

    @Transactional
    public void markAsBilled(Long id) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));

        service.setBillState(BillState.FACTURADO);
        serviceRepository.save(service);
    }

}
