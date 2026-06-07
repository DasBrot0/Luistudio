package com.luistudio.reservas.service.booking.command;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import org.springframework.stereotype.Component;

@Component
public class BookingReminderCommandManager {

    private final Queue<BookingReminderCommand> colaComandos = new LinkedList<>();

    public void agregarComando(BookingReminderCommand comando) {
        colaComandos.add(comando);
    }

    public Optional<BookingReminderCommand> obtenerComando() {
        return Optional.ofNullable(colaComandos.poll());
    }

    public void ejecutarPendientes() {
        Optional<BookingReminderCommand> comando;
        while ((comando = obtenerComando()).isPresent()) {
            comando.get().ejecutar();
        }
    }
}
