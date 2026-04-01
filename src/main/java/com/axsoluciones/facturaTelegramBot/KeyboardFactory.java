package com.axsoluciones.facturaTelegramBot;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.List;

public class KeyboardFactory {

    public static ReplyKeyboard getServiceTypeKeyboard() {
        KeyboardRow row = new KeyboardRow();
        row.add("Mantenimiento");
        row.add("Software");
        row.add("Hardware");
        return new ReplyKeyboardMarkup(List.of(row));
    }
}
