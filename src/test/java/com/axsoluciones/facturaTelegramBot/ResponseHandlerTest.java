package com.axsoluciones.facturaTelegramBot;

import com.axsoluciones.facturaTelegramBot.service.BillService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.abilitybots.api.db.DBContext;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResponseHandlerTest {

    @Mock
    private SilentSender silentSender;

    @Mock
    private DBContext dbContext;

    @Mock
    private BillService billService;

    private ResponseHandler responseHandler;

    private Map<Long, UserState> mockChatStates;
    private Map<String, String> mockUserData;
    private static final long CHAT_ID = 12345L;

    @BeforeEach
    void setUp() {
        mockChatStates = new HashMap<>();
        mockUserData = new HashMap<>();

        when(dbContext.<Long, UserState>getMap(Constants.CHAT_STATES)).thenReturn((Map) mockChatStates);
        when(dbContext.<String, String>getMap("USER_DATA")).thenReturn((Map) mockUserData);

        responseHandler = new ResponseHandler(silentSender, dbContext, billService);
    }

    @Test
    void testReplyToStart() {
        responseHandler.replyToStart(CHAT_ID);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(silentSender, times(1)).execute(captor.capture());

        SendMessage message = captor.getValue();
        assertEquals(String.valueOf(CHAT_ID), message.getChatId());
        assertEquals(Constants.START_TEXT, message.getText());
        assertNotNull(message.getReplyMarkup()); // El teclado Inline
        assertEquals(UserState.AWAITING_TYPE, mockChatStates.get(CHAT_ID));
    }

    @Test
    void testReplyToServiceType() {
        mockChatStates.put(CHAT_ID, UserState.AWAITING_TYPE);

        Message msg = mockMessage("Software");
        responseHandler.replyToButtons(CHAT_ID, msg);

        assertEquals("Software", mockUserData.get(CHAT_ID + "TYPE"));
        assertEquals(UserState.AWAITING_PHONE_NUMBER, mockChatStates.get(CHAT_ID));

        verify(silentSender, times(1)).execute(any(SendMessage.class));
    }

    @Test
    void testReplyToPhoneNumber_ExistingCustomer() {
        mockChatStates.put(CHAT_ID, UserState.AWAITING_PHONE_NUMBER);
        when(billService.customerExists("5551234567")).thenReturn(true);
        when(billService.getCustomerName("5551234567")).thenReturn("Juan");

        Message msg = mockMessage("5551234567");
        responseHandler.replyToButtons(CHAT_ID, msg);

        assertEquals("5551234567", mockUserData.get(CHAT_ID + "PHONE"));
        assertEquals(UserState.AWAITING_DETAILS, mockChatStates.get(CHAT_ID));
    }

    @Test
    void testReplyToPhoneNumber_NewCustomer() {
        mockChatStates.put(CHAT_ID, UserState.AWAITING_PHONE_NUMBER);
        when(billService.customerExists("9999999999")).thenReturn(false);

        Message msg = mockMessage("9999999999");
        responseHandler.replyToButtons(CHAT_ID, msg);

        assertEquals("9999999999", mockUserData.get(CHAT_ID + "PHONE"));
        assertEquals(UserState.TEMP_PHONE, mockChatStates.get(CHAT_ID));
    }

    @Test
    void testReplyToDetail_Software() {
        mockChatStates.put(CHAT_ID, UserState.TEMP_TYPE);
        mockUserData.put(CHAT_ID + "TYPE", "Software");

        Message msg = mockMessage("Antivirus 2025");
        responseHandler.replyToButtons(CHAT_ID, msg);

        assertEquals("Antivirus 2025", mockUserData.get(CHAT_ID + "DETAIL"));
        assertEquals(UserState.AWAITING_EXPIRATION, mockChatStates.get(CHAT_ID));
    }

    @Test
    void testReplyToDetail_Hardware() {
        mockChatStates.put(CHAT_ID, UserState.TEMP_TYPE);
        mockUserData.put(CHAT_ID + "TYPE", "Hardware");

        Message msg = mockMessage("Memoria RAM 16GB");
        responseHandler.replyToButtons(CHAT_ID, msg);

        assertEquals("Memoria RAM 16GB", mockUserData.get(CHAT_ID + "DETAIL"));
        assertEquals(UserState.AWAITING_INVOICE_DATE, mockChatStates.get(CHAT_ID));
    }

    @Test
    void testFinishOrderExecution() {
        mockChatStates.put(CHAT_ID, UserState.AWAITING_INVOICE_DATE);
        mockUserData.put(CHAT_ID + "TYPE", "Hardware");
        mockUserData.put(CHAT_ID + "PHONE", "5551234567");
        mockUserData.put(CHAT_ID + "PRICE", "1000");
        mockUserData.put(CHAT_ID + "DETAIL", "Disco duro");

        Message msg = mockMessage("2026-05-10");
        responseHandler.replyToButtons(CHAT_ID, msg);

        assertEquals("2026-05-10", mockUserData.get(CHAT_ID + "INVOICE_DATE"));
        verify(billService, times(1)).saveOrder(anyMap());
        assertFalse(mockChatStates.containsKey(CHAT_ID)); // Se debe limpiar en stopChat()
    }

    private Message mockMessage(String text) {
        Message mock = mock(Message.class);
        when(mock.getText()).thenReturn(text);
        return mock;
    }
}
