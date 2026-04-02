package com.axsoluciones.facturaTelegramBot;

import com.axsoluciones.facturaTelegramBot.entity.Service;
import com.axsoluciones.facturaTelegramBot.service.BillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.bot.BaseAbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.Flag;
import org.telegram.abilitybots.api.objects.Reply;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.function.BiConsumer;

import static org.telegram.abilitybots.api.objects.Locality.USER;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;
import static org.telegram.abilitybots.api.util.AbilityUtils.getChatId;

@Component
public class FacturaBot extends AbilityBot {

    private final ResponseHandler responseHandler;
    @Autowired
    private BillService billService;

    @Autowired
    public FacturaBot(@Value("${telegram.bot.token}") String botToken, @Value("${telegram.bot.name}") String botUsername, BillService billService) {
        super(botToken, botUsername);
        this.responseHandler = new ResponseHandler(silent, db, billService);
    }

    public Ability startBot(){
        return Ability
                .builder()
                .name("start")
                .info(Constants.START_DESCRIPTION)
                .locality(USER)
                .privacy(PUBLIC)
                .action(ctx -> responseHandler.replyToStart(ctx.chatId()))
                .build();
    }

    public Ability listarPendientes() {
        return Ability.builder()
                .name("pendientes")
                .info("Lista servicios sin facturar")
                .locality(USER)
                .privacy(PUBLIC)
                .action(ctx -> {

                    List<Service> pendientes = billService.findUnbilledServices();

                    if (pendientes.isEmpty()) {
                        silent.send(" No tienes facturas pendientes.", ctx.chatId());
                    } else {
                        StringBuilder sb = new StringBuilder(" Pendientes de Facturar: \n\n");
                        sb.append("Para marcar como FACTURADO ejecuta el comando: /facturar [ID] \nEjemplo: /factura 1 \n\n");
                        for (Service s : pendientes) {
                            sb.append(String.format("ID: %d\nCliente: %s\nServicio: %s\nMonto: $%.2f\n\n",
                                    s.getId(),
                                    s.getCustomer().getName(),
                                    s.getNameToBill(),
                                    s.getPrice()));
                        }
                        silent.send(sb.toString(), ctx.chatId());
                    }
                })
                .build();
    }

    public Ability marcarFacturado() {
        return Ability.builder()
                .name("facturar")
                .info("Marca un servicio como facturado")
                .locality(USER)
                .privacy(PUBLIC)
                .action(ctx -> {

                    String firstArg = ctx.firstArg();

                    try {
                        Long serviceId = Long.parseLong(firstArg);
                        billService.markAsBilled(serviceId);
                        silent.send(" ¡Servicio #" + serviceId + " marcado como FACTURADO!", ctx.chatId());
                    } catch (Exception e) {
                        silent.send(" Error: ID no válido o no encontrado.", ctx.chatId());
                    }
                })
                .build();
    }


    public Reply replyToButtons() {
        BiConsumer<BaseAbilityBot, Update> action = (abilityBot, upd) -> responseHandler.replyToButtons(getChatId(upd), upd.getMessage());
        return Reply.of(action, Flag.TEXT, upd -> responseHandler.userIsActive(getChatId(upd)));
    }

    @Override
    public long creatorId() {
        return 0;
    }
}
