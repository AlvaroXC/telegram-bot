package com.axsoluciones.facturaTelegramBot.service;

import com.axsoluciones.facturaTelegramBot.FacturaBot; // <--- IMPORTANTE: Importa tu Bot
import com.axsoluciones.facturaTelegramBot.entity.BillState;
import com.axsoluciones.facturaTelegramBot.entity.SoftwareService;
import com.axsoluciones.facturaTelegramBot.repository.ServiceRepository;
import com.axsoluciones.facturaTelegramBot.repository.SoftwareRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.time.LocalDate;
import java.util.List;

@Service
public class AlertScheduler {

    private final SoftwareRepository softwareRepository;
    private final ServiceRepository serviceRepository;
    private final FacturaBot facturaBot;

    @Value("${telegram.bot.admin-chat-id}")
    private long ADMIN_CHAT_ID;

    @Autowired
    public AlertScheduler(SoftwareRepository softwareRepository,
                          ServiceRepository serviceRepository,
                          @Lazy FacturaBot facturaBot) {
        this.softwareRepository = softwareRepository;
        this.serviceRepository = serviceRepository;
        this.facturaBot = facturaBot;
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void checkLicenseExpirations() {
        LocalDate targetDate = LocalDate.now().plusDays(3);
        List<SoftwareService> expiringServices = softwareRepository.findByExpiredDate(targetDate);

        for (SoftwareService service : expiringServices) {
            String msg = String.format("""
                ALERTA DE VENCIMIENTO
                Cliente: %s
                Software: %s
                Vence: %s
                Tel: %s
                """,
                    service.getCustomer().getName(),
                    service.getSoftwareName(),
                    service.getExpiredDate(),
                    service.getCustomer().getPhoneNumber()
            );

            enviarMensaje(msg);
        }
    }

    @Scheduled(cron = "0 35 11 * * *")
    public void checkInvoiceReminders() {
        LocalDate today = LocalDate.now();
        List<com.axsoluciones.facturaTelegramBot.entity.Service> reminders = serviceRepository.findByBillStateAndInvoiceDeadlineGreaterThanEqual(
                BillState.NO_FACTURADO, today
        );

        for (com.axsoluciones.facturaTelegramBot.entity.Service s : reminders) {
            enviarMensaje("RECORDATORIO: Tienes pendiente facturar a " + s.getCustomer().getName() + " por el servicio de " + s.getNameToBill() + ". Fecha límite: " + s.getInvoiceDeadline());
        }
    }

    private void enviarMensaje(String texto) {
        SendMessage message = new SendMessage();
        message.setChatId(ADMIN_CHAT_ID);
        message.setText(texto);

        facturaBot.silent().execute(message);
    }
}