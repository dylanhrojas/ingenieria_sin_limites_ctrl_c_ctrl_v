package hackathon.team.controller;

import hackathon.team.dtos.TicketUploadDTO;
import hackathon.team.model.Ticket;
import hackathon.team.model.TicketItem;
import hackathon.team.dao.UsuarioRepository;
import hackathon.team.service.CategoriaService;
import hackathon.team.service.TicketUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Controller para subir tickets con imagen
 * Conector Semántico - OneCard
 */
@Controller
@RequestMapping("/tickets")
@RequiredArgsConstructor
@Slf4j
public class TicketUploadController {

    private final TicketUploadService ticketUploadService;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaService categoriaService;

    /**
     * Mostrar lista de todos los tickets
     */
    @GetMapping("/lista")
    public String listarTickets(Model model) {
        List<Ticket> tickets = ticketUploadService.obtenerTodosLosTickets();
        
        // Calcular estadísticas
        BigDecimal totalGastado = tickets.stream()
                .map(Ticket::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal promedioTicket = tickets.isEmpty() ? BigDecimal.ZERO :
                totalGastado.divide(BigDecimal.valueOf(tickets.size()), 2, RoundingMode.HALF_UP);
        
        Optional<Ticket> ticketMayor = tickets.stream()
                .max(Comparator.comparing(Ticket::getTotal));
        
        Optional<Ticket> ticketMenor = tickets.stream()
                .filter(t -> t.getTotal().compareTo(BigDecimal.ZERO) > 0)
                .min(Comparator.comparing(Ticket::getTotal));
        
        // Contar productos únicos
        long totalProductos = tickets.stream()
                .flatMap(t -> t.getItems().stream())
                .mapToLong(TicketItem::getCantidad)
                .sum();
        
        model.addAttribute("tickets", tickets);
        model.addAttribute("totalTickets", tickets.size());
        model.addAttribute("totalGastado", totalGastado);
        model.addAttribute("promedioTicket", promedioTicket);
        model.addAttribute("ticketMayor", ticketMayor.orElse(null));
        model.addAttribute("ticketMenor", ticketMenor.orElse(null));
        model.addAttribute("totalProductos", totalProductos);
        
        return "tickets/lista";
    }

    /**
     * Ver detalle de un ticket específico (fragmento para modal)
     */
    @GetMapping("/detalle/{id}")
    public String verDetalle(@PathVariable Long id, Model model) {
        Ticket ticket = ticketUploadService.obtenerTicketPorId(id)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));
        
        model.addAttribute("ticket", ticket);
        return "tickets/detalle :: detalle";
    }

    /**
     * Endpoint para formulario de subida
     */
    @GetMapping("/subir")
    public String mostrarFormulario(Model model) {
        log.info("GET /tickets/subir - Mostrando formulario para subir ticket");
        
        try {
            TicketUploadDTO ticketDTO = new TicketUploadDTO();
            // Agregar un producto vacío por defecto
            ticketDTO.agregarProducto(new TicketUploadDTO.ProductoTicketDTO());
            
            model.addAttribute("ticketDTO", ticketDTO);
            model.addAttribute("usuarios", usuarioRepository.findAll());
            model.addAttribute("categorias", categoriaService.obtenerActivas());
            model.addAttribute("titulo", "Subir Ticket de Compra");
            
            return "tickets/subir";
        } catch (Exception e) {
            log.error("Error al mostrar formulario de ticket", e);
            model.addAttribute("error", "Error al cargar el formulario: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Procesar subida de ticket
     */
    @PostMapping("/guardar")
    public String guardarTicket(@Valid @ModelAttribute("ticketDTO") TicketUploadDTO ticketDTO,
                               BindingResult result,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        log.info("POST /tickets/guardar - Procesando subida de ticket");

        // Validar imagen
        MultipartFile imagen = ticketDTO.getImagenTicket();
        if (imagen == null || imagen.isEmpty()) {
            result.rejectValue("imagenTicket", "error.imagenTicket", "Debe subir una imagen del ticket");
        } else {
            // Validar tipo de archivo
            if (!ticketUploadService.esImagenValida(imagen)) {
                result.rejectValue("imagenTicket", "error.imagenTicket", 
                    "El archivo debe ser una imagen (JPG, PNG, GIF, WEBP)");
            }
            // Validar tamaño
            if (!ticketUploadService.tamanioValido(imagen)) {
                result.rejectValue("imagenTicket", "error.imagenTicket", 
                    "El tamaño de la imagen no debe exceder 10MB");
            }
        }

        // Validar que hay productos
        if (ticketDTO.getProductos() == null || ticketDTO.getProductos().isEmpty()) {
            result.reject("error.productos", "Debe agregar al menos un producto");
        }

        // Si hay errores de validación
        if (result.hasErrors()) {
            log.warn("Errores de validación en formulario de ticket");
            model.addAttribute("usuarios", usuarioRepository.findAll());
            model.addAttribute("categorias", categoriaService.obtenerActivas());
            model.addAttribute("titulo", "Subir Ticket de Compra");
            return "tickets/subir";
        }

        try {
            // Guardar ticket
            Ticket ticket = ticketUploadService.guardarTicket(ticketDTO);
            log.info("Ticket guardado exitosamente: {}", ticket.getNumeroTicket());
            
            redirectAttributes.addFlashAttribute("mensaje", 
                "Ticket '" + ticket.getNumeroTicket() + "' subido exitosamente");
            redirectAttributes.addFlashAttribute("tipoMensaje", "success");
            
            return "redirect:/tickets/lista";
        } catch (Exception e) {
            log.error("Error al guardar ticket", e);
            model.addAttribute("error", "Error al guardar el ticket: " + e.getMessage());
            model.addAttribute("usuarios", usuarioRepository.findAll());
            model.addAttribute("categorias", categoriaService.obtenerActivas());
            model.addAttribute("titulo", "Subir Ticket de Compra");
            return "tickets/subir";
        }
    }

    /**
     * Ver imagen del ticket
     */
    @GetMapping("/imagen/{nombreArchivo:.+}")
    @ResponseBody
    public ResponseEntity<Resource> verImagen(@PathVariable String nombreArchivo) {
        try {
            Path rutaArchivo = ticketUploadService.obtenerRutaImagen(nombreArchivo);
            Resource resource = new UrlResource(rutaArchivo.toUri());

            if (resource.exists() && resource.isReadable()) {
                // Determinar el tipo de contenido
                String contentType = "image/jpeg";
                if (nombreArchivo.endsWith(".png")) {
                    contentType = "image/png";
                } else if (nombreArchivo.endsWith(".gif")) {
                    contentType = "image/gif";
                } else if (nombreArchivo.endsWith(".webp")) {
                    contentType = "image/webp";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nombreArchivo + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error al cargar imagen: {}", nombreArchivo, e);
            return ResponseEntity.notFound().build();
        }
    }
}