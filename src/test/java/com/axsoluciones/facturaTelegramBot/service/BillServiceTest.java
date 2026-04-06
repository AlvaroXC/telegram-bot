package com.axsoluciones.facturaTelegramBot.service;

import com.axsoluciones.facturaTelegramBot.entity.*;
import com.axsoluciones.facturaTelegramBot.repository.CustomerRepository;
import com.axsoluciones.facturaTelegramBot.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @InjectMocks
    private BillService billService;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer();
        testCustomer.setId(1L);
        testCustomer.setName("Juan Perez");
        testCustomer.setPhoneNumber("5551234567");
    }

    @Test
    void testCustomerExists() {
        when(customerRepository.findByPhoneNumber("5551234567")).thenReturn(Optional.of(testCustomer));
        assertTrue(billService.customerExists("5551234567"));

        when(customerRepository.findByPhoneNumber("0000000000")).thenReturn(Optional.empty());
        assertFalse(billService.customerExists("0000000000"));
    }

    @Test
    void testGetCustomerName() {
        when(customerRepository.findByPhoneNumber("5551234567")).thenReturn(Optional.of(testCustomer));

        String name = billService.getCustomerName("5551234567");
        assertEquals("Juan Perez", name);

        String unknownName = billService.getCustomerName("1111111111");
        assertEquals("Desconocido", unknownName);
    }

    @Test
    void testSaveOrder_NewCustomer() {
        Map<String, String> data = Map.of(
                "TYPE", "Software",
                "PHONE", "9998887777",
                "NAME", "Nuevo Cliente",
                "PRICE", "1500.00",
                "DETAIL", "Licencia Antivirus",
                "DATE", "2027-12-31",
                "INVOICE_DATE", "2026-05-15"
        );

        when(customerRepository.findByPhoneNumber("9998887777")).thenReturn(Optional.empty());

        Customer savedCustomer = new Customer();
        savedCustomer.setName("Nuevo Cliente");
        savedCustomer.setPhoneNumber("9998887777");
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);

        billService.saveOrder(data);

        verify(customerRepository, times(1)).save(any(Customer.class));
        
        ArgumentCaptor<Service> serviceCaptor = ArgumentCaptor.forClass(Service.class);
        verify(serviceRepository, times(1)).save(serviceCaptor.capture());

        Service savedService = serviceCaptor.getValue();
        assertInstanceOf(SoftwareService.class, savedService);
        SoftwareService sw = (SoftwareService) savedService;
        assertEquals("Licencia Antivirus", sw.getSoftwareName());
        assertEquals(LocalDate.parse("2027-12-31"), sw.getExpiredDate());
        assertEquals(1500.00, sw.getPrice());
        assertEquals(BillState.NO_FACTURADO, sw.getBillState());
        assertEquals(LocalDate.parse("2026-05-15"), sw.getInvoiceDeadline());
    }

    @Test
    void testSaveOrder_ExistingCustomer_Hardware() {
        Map<String, String> data = Map.of(
                "TYPE", "Hardware",
                "PHONE", "5551234567",
                "NAME", "Juan Perez",
                "PRICE", "5000.00",
                "DETAIL", "Disco Duro 1TB",
                "INVOICE_DATE", "2026-04-10"
        );

        when(customerRepository.findByPhoneNumber("5551234567")).thenReturn(Optional.of(testCustomer));

        billService.saveOrder(data);

        // No debe guardar cliente porque ya existe
        verify(customerRepository, never()).save(any(Customer.class));

        ArgumentCaptor<Service> serviceCaptor = ArgumentCaptor.forClass(Service.class);
        verify(serviceRepository, times(1)).save(serviceCaptor.capture());

        Service savedService = serviceCaptor.getValue();
        assertInstanceOf(HardwareService.class, savedService);
        HardwareService hw = (HardwareService) savedService;
        assertEquals("Disco Duro 1TB", hw.getHardwareName());
        assertEquals(5000.00, hw.getPrice());
        assertEquals(BillState.NO_FACTURADO, hw.getBillState());
    }

    @Test
    void testFindUnbilledServices() {
        HardwareService hws = new HardwareService();
        hws.setBillState(BillState.NO_FACTURADO);

        when(serviceRepository.findByBillState(BillState.NO_FACTURADO))
                .thenReturn(Collections.singletonList(hws));

        List<Service> unbilled = billService.findUnbilledServices();
        assertEquals(1, unbilled.size());
        assertEquals(BillState.NO_FACTURADO, unbilled.get(0).getBillState());
    }

    @Test
    void testMarkAsBilled() {
        MaintenanceService ms = new MaintenanceService();
        ms.setId(10L);
        ms.setBillState(BillState.NO_FACTURADO);

        when(serviceRepository.findById(10L)).thenReturn(Optional.of(ms));

        billService.markAsBilled(10L);

        assertEquals(BillState.FACTURADO, ms.getBillState());
        verify(serviceRepository, times(1)).save(ms);
    }

    @Test
    void testMarkAsBilled_NotFound() {
        when(serviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> billService.markAsBilled(99L));
    }
}
