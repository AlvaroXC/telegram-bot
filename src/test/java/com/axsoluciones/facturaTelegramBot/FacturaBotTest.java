package com.axsoluciones.facturaTelegramBot;

import com.axsoluciones.facturaTelegramBot.service.BillService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.Reply;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class FacturaBotTest {

    @Mock
    private BillService billService;

    private FacturaBot facturaBot;

    @BeforeEach
    void setUp() {
        // Inicializamos FacturaBot con tokens ficticios. El nombre debe ser único por test para evitar bloqueos del archivo DB.
        String uniqueBotName = "dummyBot_" + java.util.UUID.randomUUID().toString();
        facturaBot = new FacturaBot("dummyToken", uniqueBotName, billService);
    }

    @Test
    void testStartBotAbility() {
        Ability startAbility = facturaBot.startBot();
        assertNotNull(startAbility);
        assertEquals("start", startAbility.name());
    }

    @Test
    void testListarPendientesAbility() {
        Ability pendientesAbility = facturaBot.listarPendientes();
        assertNotNull(pendientesAbility);
        assertEquals("pendientes", pendientesAbility.name());
    }

    @Test
    void testMarcarFacturadoAbility() {
        Ability facturarAbility = facturaBot.marcarFacturado();
        assertNotNull(facturarAbility);
        assertEquals("facturar", facturarAbility.name());
    }

    @Test
    void testReplyToButtons() {
        Reply reply = facturaBot.replyToButtons();
        assertNotNull(reply);
    }
}
