package com.axsoluciones.facturaTelegramBot;

import com.axsoluciones.facturaTelegramBot.entity.Customer;
import com.axsoluciones.facturaTelegramBot.repository.CustomerRepository;
import com.axsoluciones.facturaTelegramBot.service.BillService;
import org.telegram.abilitybots.api.db.DBContext;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ResponseHandler  {

    private final SilentSender silentSender;
    private final Map<Long, UserState> chatStates;

    private final Map<String, String> userData;
    private final BillService billService;

    public ResponseHandler(SilentSender silent, DBContext dbContext, BillService billService){
        this.silentSender = silent;
        chatStates = dbContext.getMap(Constants.CHAT_STATES);
        this.userData = dbContext.getMap("USER_DATA");
        this.billService = billService;
    }

    public void replyToStart(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(Constants.START_TEXT);
        message.setReplyMarkup(KeyboardFactory.getServiceTypeKeyboard());
        chatStates.put(chatId, UserState.AWAITING_TYPE);
        silentSender.execute(message);
    }

    public void replyToButtons(long chatId, Message message) {
        if (message.getText().equalsIgnoreCase("/stop")) {
            stopChat(chatId);
            return;
        }

        switch (chatStates.get(chatId)) {
            case AWAITING_TYPE -> replyToServiceType(chatId, message);
            case AWAITING_PHONE_NUMBER -> replyToPhoneNumber(chatId, message);
            case TEMP_PHONE -> replyToName(chatId, message);
            case AWAITING_DETAILS -> replyToPrice(chatId, message);
            case TEMP_TYPE -> replyToDetail(chatId, message);
            case AWAITING_EXPIRATION -> replyToExpiration(chatId, message);
            case AWAITING_INVOICE_DATE -> replyToInvoiceDate(chatId, message);

            default -> unexpectedMessage(chatId);
        }
    }

    private void replyToDetail(long chatId, Message message) {
        userData.put(chatId + "DETAIL", message.getText());
        String type = userData.get(chatId + "TYPE");

        if ("Software".equals(type)) {
            sendMessage(chatId, "¿Cuándo vence la licencia? (YYYY-MM-DD):");
            chatStates.put(chatId, UserState.AWAITING_EXPIRATION);
        } else {
            sendMessage(chatId, "¿Qué fecha quieres recordar la facturación? (YYYY-MM-DD):");
            chatStates.put(chatId, UserState.AWAITING_INVOICE_DATE);
        }
    }

    private void replyToExpiration(long chatId, Message message) {
        String dateText = message.getText();

        if (!dateText.matches("\\d{4}-\\d{2}-\\d{2}")) {
            sendMessage(chatId, " Formato incorrecto. Por favor usa AAAA-MM-DD (ej. 2027-05-20)");
            return;
        }

        userData.put(chatId + "DATE", dateText);

        sendMessage(chatId, "¿Qué fecha quieres recordar la facturación? (YYYY-MM-DD):");
        chatStates.put(chatId, UserState.AWAITING_INVOICE_DATE);
    }


    private void replyToInvoiceDate(long chatId, Message message) {
        String dateText = message.getText();

        if (!dateText.matches("\\d{4}-\\d{2}-\\d{2}")) {
            sendMessage(chatId, " Formato incorrecto. Usa AAAA-MM-DD (ej. 2026-02-28)");
            return;
        }

        userData.put(chatId + "INVOICE_DATE", dateText);

        finishOrder(chatId);
    }

    private void replyToServiceType(long chatId, Message message) {
        String type = message.getText();
        userData.put(chatId + "TYPE", type);

        sendMessage(chatId, "Has seleccionado " + type + ". \n Ingresa el teléfono del cliente:");
        chatStates.put(chatId, UserState.AWAITING_PHONE_NUMBER);
    }

    private void replyToPhoneNumber(long chatId, Message message) {
        String phone = message.getText();
        userData.put(chatId + "PHONE", phone);

        if (billService.customerExists(phone)) {
            String name = billService.getCustomerName(phone);
            sendMessage(chatId, "Cliente encontrado: " + name + ". \n Ingresa el precio del servicio:");
            chatStates.put(chatId, UserState.AWAITING_DETAILS);
        } else {
            sendMessage(chatId, "Cliente nuevo. \n Ingresa el nombre del cliente:");
            chatStates.put(chatId, UserState.TEMP_PHONE);
        }
    }

    private void replyToName(long chatId, Message message) {
        userData.put(chatId + "NAME", message.getText());
        sendMessage(chatId, "Guardado. \n Ingresa el precio del servicio realizado:");
        chatStates.put(chatId, UserState.AWAITING_DETAILS);
    }

    private void replyToPrice(long chatId, Message message) {
        try {
            Double.parseDouble(message.getText()); // Validar que sea número
            userData.put(chatId + "PRICE", message.getText());

            String type = userData.get(chatId + "TYPE");
            if ("Software".equals(type)) {
                sendMessage(chatId, " Escribe el nombre del Software:");
            } else if ("Hardware".equals(type)) {
                sendMessage(chatId, " Escribe el nombre del Hardware:");
            } else {
                sendMessage(chatId, " Escribe la descripción del mantenimiento:");
            }
            chatStates.put(chatId, UserState.TEMP_TYPE);

        } catch (NumberFormatException e) {
            sendMessage(chatId, " Error: Ingresa un precio válido (ej. 1500.50)");
        }
    }

    private void finishOrder(long chatId) {
        try {
            Map<String, String> data = Map.of(
                    "TYPE", userData.get(chatId + "TYPE"),
                    "PHONE", userData.get(chatId + "PHONE"),
                    "NAME", userData.getOrDefault(chatId + "NAME", ""),
                    "PRICE", userData.get(chatId + "PRICE"),
                    "DETAIL", userData.get(chatId + "DETAIL"),
                    "DATE", userData.getOrDefault(chatId + "DATE", ""),
                    "INVOICE_DATE", userData.get(chatId + "INVOICE_DATE")
            );

            billService.saveOrder(data);

            sendWhatsAppButton(chatId, data);

            stopChat(chatId);

        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(chatId, "Error guardando: " + e.getMessage());
        }
    }

    private void sendWhatsAppButton(long chatId,Map<String, String> data ) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText(" Guardado. ¿Enviar WhatsApp?");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText("Abrir WhatsApp");
        String text = "Hola! Gracias por contratar " + data.get("TYPE");
        btn.setUrl("https://wa.me/" + data.get("PHONE") + "?text=" + URLEncoder.encode(text, StandardCharsets.UTF_8));

        row.add(btn);
        rows.add(row);
        markup.setKeyboard(rows);
        msg.setReplyMarkup(markup);

        silentSender.execute(msg);
    }

    private void sendMessage(long chatId, String text) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText(text);
        msg.setReplyMarkup(new ReplyKeyboardRemove(true));
        silentSender.execute(msg);
    }

    private void unexpectedMessage(long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("No esperaba esto.");
        silentSender.execute(sendMessage);
    }

    private void stopChat(long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("El recordatorio quedó guardado!\nPresiona /start para guardar un nuevo recordatorio");
        chatStates.remove(chatId);
        sendMessage.setReplyMarkup(new ReplyKeyboardRemove(true));
        silentSender.execute(sendMessage);
    }

    public boolean userIsActive(Long chatId) {
        return chatStates.containsKey(chatId);
    }
}
