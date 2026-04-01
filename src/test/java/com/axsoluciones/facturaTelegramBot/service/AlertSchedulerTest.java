package com.axsoluciones.facturaTelegramBot.service;

import com.axsoluciones.facturaTelegramBot.FacturaBot;
import com.axsoluciones.facturaTelegramBot.entity.BillState;
import com.axsoluciones.facturaTelegramBot.entity.Customer;
import com.axsoluciones.facturaTelegramBot.entity.Service;
import com.axsoluciones.facturaTelegramBot.entity.SoftwareService;
import com.axsoluciones.facturaTelegramBot.repository.ServiceRepository;
import com.axsoluciones.facturaTelegramBot.repository.SoftwareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertSchedulerTest {

    @Mock
    private SoftwareRepository softwareRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private FacturaBot facturaBot;

    @Mock
    private SilentSender silentSender;

    private AlertScheduler alertScheduler;

    @BeforeEach
    void setUp() {
        alertScheduler = new AlertScheduler(softwareRepository, serviceRepository, facturaBot);
        ReflectionTestUtils.setField(alertScheduler, "ADMIN_CHAT_ID", 123456789L);
        when(facturaBot.silent()).thenReturn(silentSender);
    }

    @Test
    void testCheckLicenseExpirations_SendsAlert() {
        Customer customer = new Customer();
        customer.setName("Acme Corp");
        customer.setPhoneNumber("5551234567");

        SoftwareService sw = new SoftwareService();
        sw.setSoftwareName("ERP System");
        sw.setExpiredDate(LocalDate.now().plusDays(3));
        sw.setCustomer(customer);

        when(softwareRepository.findByExpiredDate(any(LocalDate.class)))
                .thenReturn(Collections.singletonList(sw));

        alertScheduler.checkLicenseExpirations();

        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(silentSender, times(1)).execute(messageCaptor.capture());

        SendMessage sentMessage = messageCaptor.getValue();
        assertEquals("123456789", sentMessage.getChatId());
        assertTrue(sentMessage.getText().contains("ALERTA DE VENCIMIENTO"));
        assertTrue(sentMessage.getText().contains("ERP System"));
        assertTrue(sentMessage.getText().contains("Acme Corp"));
    }

    @Test
    void testCheckInvoiceReminders_SendsAlert() {
        Customer customer = new Customer();
        customer.setName("Maria Lopez");

        Service ms = new com.axsoluciones.facturaTelegramBot.entity.MaintenanceService();
        ms.setCustomer(customer);
        ms.setBillState(BillState.NO_FACTURADO);
        ms.setInvoiceReminderDate(LocalDate.now());

        when(serviceRepository.findByBillStateAndInvoiceReminderDate(eq(BillState.NO_FACTURADO), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(ms));

        alertScheduler.checkInvoiceReminders();

        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(silentSender, times(1)).execute(messageCaptor.capture());

        SendMessage sentMessage = messageCaptor.getValue();
        assertEquals("123456789", sentMessage.getChatId());
        assertTrue(sentMessage.getText().contains("RECORDATORIO:"));
        assertTrue(sentMessage.getText().contains("Maria Lopez"));
    }
}
