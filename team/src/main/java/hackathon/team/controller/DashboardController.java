package hackathon.team.controller;

import hackathon.team.dao.CategoriaRepository;
import hackathon.team.dao.ProductoRepository;
import hackathon.team.dao.TicketRepository;
import hackathon.team.model.Ticket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Controller para el Dashboard principal con estadísticas
 * Conector Semántico - OneCard
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final TicketRepository ticketRepository;
    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    /**
     * Mostrar dashboard principal con estadísticas reales
     */
    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        log.info("Cargando dashboard con estadísticas");
        
        try {
            // Obtener todos los tickets
            List<Ticket> tickets = ticketRepository.findAll();
            
            // Calcular estadísticas de tickets
            long totalTickets = tickets.size();
            
            BigDecimal totalGastado = tickets.stream()
                    .map(Ticket::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal promedioTicket = totalTickets > 0 
                    ? totalGastado.divide(BigDecimal.valueOf(totalTickets), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            
            // Contar productos y categorías
            long totalProductos = productoRepository.count();
            long totalCategorias = categoriaRepository.count();
            
            // Agregar al modelo
            model.addAttribute("totalTickets", totalTickets);
            model.addAttribute("totalGastado", totalGastado);
            model.addAttribute("promedioTicket", promedioTicket);
            model.addAttribute("totalProductos", totalProductos);
            model.addAttribute("totalCategorias", totalCategorias);
            
            log.info("Dashboard cargado: {} tickets, ${} gastado, {} productos, {} categorías",
                    totalTickets, totalGastado, totalProductos, totalCategorias);
            
        } catch (Exception e) {
            log.error("Error al cargar estadísticas del dashboard", e);
            
            // Valores por defecto en caso de error
            model.addAttribute("totalTickets", 0);
            model.addAttribute("totalGastado", BigDecimal.ZERO);
            model.addAttribute("promedioTicket", BigDecimal.ZERO);
            model.addAttribute("totalProductos", 0);
            model.addAttribute("totalCategorias", 0);
        }
        
        return "dashboard";
    }
}