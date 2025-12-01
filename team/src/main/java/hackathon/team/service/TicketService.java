package hackathon.team.service;

import hackathon.team.dao.TicketRepository;
import hackathon.team.model.Ticket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

/**
 * Servicio para gestión de Tickets
 * Conector Semántico - OneCard
 */
@Service
@Transactional
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;

    /**
     * Generar número de ticket único
     * Formato: TICKET-2024-000001
     */
    public String generarNumeroTicket() {
        int anioActual = Year.now().getValue();
        String prefijo = "TICKET-" + anioActual + "-";
        
        // Obtener los últimos tickets del año
        List<String> ultimosTickets = ticketRepository.findTicketsPorPrefijo(prefijo + "%");
        
        int siguienteNumero = 1;
        
        if (!ultimosTickets.isEmpty()) {
            String ultimoTicket = ultimosTickets.get(0);
            // Extraer el número del ticket (últimos 6 dígitos)
            try {
                String numeroStr = ultimoTicket.substring(ultimoTicket.lastIndexOf("-") + 1);
                int ultimoNumero = Integer.parseInt(numeroStr);
                siguienteNumero = ultimoNumero + 1;
            } catch (Exception e) {
                // Si hay error en el parsing, empezar desde 1
                siguienteNumero = 1;
            }
        }
        
        // Formatear con 6 dígitos con ceros a la izquierda
        return prefijo + String.format("%06d", siguienteNumero);
    }

    /**
     * Guardar ticket
     */
    public Ticket guardar(Ticket ticket) {
        if (ticket.getNumeroTicket() == null || ticket.getNumeroTicket().isEmpty()) {
            ticket.setNumeroTicket(generarNumeroTicket());
        }
        ticket.calcularTotal();
        return ticketRepository.save(ticket);
    }

    /**
     * Buscar todos los tickets
     */
    public List<Ticket> findAll() {
        return ticketRepository.findAll();
    }

    /**
     * Buscar ticket por ID
     */
    public Ticket findById(Long id) {
        return ticketRepository.findById(id).orElse(null);
    }

    /**
     * Buscar ticket por número
     */
    public Ticket findByNumeroTicket(String numeroTicket) {
        return ticketRepository.findByNumeroTicket(numeroTicket).orElse(null);
    }

    /**
     * Buscar tickets por usuario
     */
    public List<Ticket> findByUsuario(Long usuarioId) {
        return ticketRepository.findByUsuario(usuarioId);
    }

    /**
     * Buscar tickets del día actual
     */
    public List<Ticket> findTicketsDelDia() {
        return ticketRepository.findTicketsPorFecha(LocalDate.now());
    }

    /**
     * Calcular ventas del día actual
     */
    public BigDecimal calcularVentasDelDia() {
        return ticketRepository.calcularVentasPorFecha(LocalDate.now());
    }

    /**
     * Contar tickets del día actual
     */
    public Long contarTicketsDelDia() {
        return ticketRepository.contarTicketsPorFecha(LocalDate.now());
    }

    /**
     * Buscar tickets por fecha específica
     */
    public List<Ticket> findTicketsPorFecha(LocalDate fecha) {
        return ticketRepository.findTicketsPorFecha(fecha);
    }

    /**
     * Calcular ventas por fecha específica
     */
    public BigDecimal calcularVentasPorFecha(LocalDate fecha) {
        return ticketRepository.calcularVentasPorFecha(fecha);
    }

    /**
     * Buscar tickets por rango de fechas
     */
    public List<Ticket> findByRangoFechas(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return ticketRepository.findByRangoFechas(fechaInicio, fechaFin);
    }

    /**
     * Calcular ventas por rango de fechas
     */
    public BigDecimal calcularVentasPorRango(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return ticketRepository.calcularVentasPorRango(fechaInicio, fechaFin);
    }

    /**
     * Buscar últimos tickets
     */
    public List<Ticket> findUltimosTickets() {
        return ticketRepository.findUltimosTickets();
    }

    /**
     * Buscar tickets por método de pago
     */
    public List<Ticket> findByMetodoPago(String metodoPago) {
        return ticketRepository.findByMetodoPago(metodoPago);
    }

    /**
     * Eliminar ticket
     */
    public void eliminar(Long id) {
        ticketRepository.deleteById(id);
    }
}