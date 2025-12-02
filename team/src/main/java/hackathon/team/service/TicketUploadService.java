package hackathon.team.service;

import hackathon.team.dtos.TicketUploadDTO;
import hackathon.team.model.*;
import hackathon.team.dao.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

/**
 * Service para gestión de tickets y subida de archivos
 * Conector Semántico - OneCard
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketUploadService {

    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Verificar y crear directorio de uploads si no existe
     */
    @PostConstruct
    public void init() {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Directorio de uploads creado: {}", uploadPath.toAbsolutePath());
            } else {
                log.info("Directorio de uploads encontrado: {}", uploadPath.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Error al crear directorio de uploads", e);
            throw new RuntimeException("No se pudo inicializar el directorio de uploads", e);
        }
    }

    /**
     * Obtener todos los tickets ordenados por fecha (más recientes primero)
     */
    public List<Ticket> obtenerTodosLosTickets() {
        return ticketRepository.findAll().stream()
                .sorted((t1, t2) -> t2.getFechaHora().compareTo(t1.getFechaHora()))
                .toList();
    }

    /**
     * Obtener ticket por ID
     */
    public Optional<Ticket> obtenerTicketPorId(Long id) {
        return ticketRepository.findById(id);
    }

    /**
     * Guardar ticket con imagen y productos
     */
    @Transactional
    public Ticket guardarTicket(TicketUploadDTO dto) throws IOException {
        log.info("Guardando ticket con imagen y {} productos", dto.getProductos().size());

        // Validar usuario
        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + dto.getUsuarioId()));

        // Guardar imagen
        String rutaImagen = guardarImagen(dto.getImagenTicket());
        log.info("Imagen guardada en: {}", rutaImagen);

        // Crear ticket
        Ticket ticket = new Ticket();
        ticket.setNumeroTicket(generarNumeroTicket());
        ticket.setUsuario(usuario);
        ticket.setTotal(dto.getTotal());
        ticket.setSubtotal(dto.getSubtotal() != null ? dto.getSubtotal() : dto.getTotal());
        ticket.setImpuestos(dto.getImpuestos() != null ? dto.getImpuestos() : BigDecimal.ZERO);
        ticket.setDescuentos(dto.getDescuentos() != null ? dto.getDescuentos() : BigDecimal.ZERO);
        ticket.setMetodoPago(dto.getMetodoPago());
        ticket.setObservaciones(dto.getObservaciones());
        ticket.setEstado("pendiente_clasificacion");

        ticket.setImagenTicket(rutaImagen); 

        // Agregar productos
        for (TicketUploadDTO.ProductoTicketDTO productoDTO : dto.getProductos()) {
            // Buscar o crear producto
            Producto producto = buscarOCrearProducto(productoDTO);

            // Crear item del ticket
            TicketItem item = new TicketItem();
            item.setProducto(producto);
            item.setCantidad(productoDTO.getCantidad());
            item.setPrecioUnitario(productoDTO.getPrecioUnitario());
            item.setDescuento(productoDTO.getDescuento() != null ? productoDTO.getDescuento() : BigDecimal.ZERO);
            item.calcularSubtotal();

            ticket.agregarItem(item);
        }

        // Guardar en base de datos
        Ticket guardado = ticketRepository.save(ticket);
        log.info("Ticket guardado exitosamente con ID: {} y número: {}", guardado.getId(), guardado.getNumeroTicket());

        return guardado;
    }

    /**
     * Guardar imagen en el sistema de archivos
     */
    private String guardarImagen(MultipartFile archivo) throws IOException {
        // Crear directorio si no existe
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("Directorio de uploads creado: {}", uploadPath.toAbsolutePath());
        }

        // Generar nombre único para el archivo
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String extension = obtenerExtension(archivo.getOriginalFilename());
        String nombreArchivo = "ticket_" + timestamp + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

        // Guardar archivo
        Path rutaDestino = uploadPath.resolve(nombreArchivo);
        Files.copy(archivo.getInputStream(), rutaDestino, StandardCopyOption.REPLACE_EXISTING);

        log.info("Archivo guardado: {} (tamaño: {} bytes)", nombreArchivo, archivo.getSize());

        return nombreArchivo;
    }

    /**
     * Buscar o crear producto basado en el DTO
     */
    private Producto buscarOCrearProducto(TicketUploadDTO.ProductoTicketDTO productoDTO) {
        // Intentar buscar producto existente
        String nombreCompleto = productoDTO.getNombreProducto() + 
                               (productoDTO.getMarca() != null ? " - " + productoDTO.getMarca() : "");

        List<Producto> productosExistentes = productoRepository.findByNombreIgnoreCase(nombreCompleto);
        
        Producto producto = null;
        if (productosExistentes != null && !productosExistentes.isEmpty()) {
            producto = productosExistentes.get(0);
            log.info("Producto encontrado: {}", nombreCompleto);
        }

        if (producto == null) {
            log.info("Creando nuevo producto: {}", nombreCompleto);

            // Crear nuevo producto
            producto = new Producto();
            producto.setNombre(productoDTO.getNombreProducto());
            producto.setMarca(productoDTO.getMarca());
            producto.setPrecioReferencia(productoDTO.getPrecioUnitario());
            producto.setActivo(true);

            // Asignar categoría si se especificó
            if (productoDTO.getCategoriaId() != null) {
                Categoria categoria = categoriaRepository.findById(productoDTO.getCategoriaId())
                        .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
                producto.setCategoria(categoria);
            } else {
                // Asignar categoría por defecto o "Sin clasificar"
                Categoria categoriaDefault = categoriaRepository.findByNombreIgnoreCase("Sin Clasificar")
                        .orElseGet(() -> {
                            Categoria nueva = new Categoria("Sin Clasificar", "sin clasificar, general, otros");
                            return categoriaRepository.save(nueva);
                        });
                producto.setCategoria(categoriaDefault);
            }

            producto = productoRepository.save(producto);
        }

        return producto;
    }

    /**
     * Generar número único de ticket
     */
    private String generarNumeroTicket() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "TKT-" + timestamp + "-" + random;
    }

    /**
     * Obtener extensión del archivo
     */
    private String obtenerExtension(String nombreArchivo) {
        if (nombreArchivo == null || !nombreArchivo.contains(".")) {
            return ".jpg";
        }
        return nombreArchivo.substring(nombreArchivo.lastIndexOf("."));
    }

    /**
     * Obtener ruta completa de la imagen
     */
    public Path obtenerRutaImagen(String nombreArchivo) {
        return Paths.get(uploadDir).resolve(nombreArchivo);
    }

    /**
     * Validar que el archivo es una imagen
     */
    public boolean esImagenValida(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            return false;
        }

        String contentType = archivo.getContentType();
        if (contentType == null) {
            return false;
        }

        return contentType.equals("image/jpeg") ||
               contentType.equals("image/jpg") ||
               contentType.equals("image/png") ||
               contentType.equals("image/gif") ||
               contentType.equals("image/webp");
    }

    /**
     * Validar tamaño del archivo (máximo 10MB)
     */
    public boolean tamanioValido(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            return false;
        }
        // 10MB = 10 * 1024 * 1024 bytes
        return archivo.getSize() <= 10 * 1024 * 1024;
    }
}