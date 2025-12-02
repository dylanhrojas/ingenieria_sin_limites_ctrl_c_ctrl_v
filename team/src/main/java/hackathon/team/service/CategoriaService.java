package hackathon.team.service;

import hackathon.team.dtos.CategoriaDTO;
import hackathon.team.model.Categoria;
import hackathon.team.dao.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service para gestión de Categorías
 * Conector Semántico - OneCard
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Obtener todas las categorías
     */
    @Transactional(readOnly = true)
    public List<CategoriaDTO> obtenerTodas() {
        log.info("Obteniendo todas las categorías");
        return categoriaRepository.findAllByOrderByNombreAsc()
                .stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtener solo categorías activas
     */
    @Transactional(readOnly = true)
    public List<CategoriaDTO> obtenerActivas() {
        log.info("Obteniendo categorías activas");
        return categoriaRepository.findByActivaTrue()
                .stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtener categorías raíz (sin padre)
     */
    @Transactional(readOnly = true)
    public List<CategoriaDTO> obtenerCategoriasRaiz() {
        log.info("Obteniendo categorías raíz");
        return categoriaRepository.findCategoriasRaiz()
                .stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtener categorías raíz activas
     */
    @Transactional(readOnly = true)
    public List<CategoriaDTO> obtenerCategoriasRaizActivas() {
        log.info("Obteniendo categorías raíz activas");
        return categoriaRepository.findCategoriasRaizActivas()
                .stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtener subcategorías de una categoría padre
     */
    @Transactional(readOnly = true)
    public List<CategoriaDTO> obtenerSubcategorias(Long padreId) {
        log.info("Obteniendo subcategorías de la categoría ID: {}", padreId);
        return categoriaRepository.findSubcategoriasByPadreId(padreId)
                .stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtener categoría por ID
     */
    @Transactional(readOnly = true)
    public CategoriaDTO obtenerPorId(Long id) {
        log.info("Obteniendo categoría por ID: {}", id);
        return categoriaRepository.findById(id)
                .map(this::convertirADTO)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + id));
    }

    /**
     * Crear nueva categoría
     */
    @Transactional
    public CategoriaDTO crear(CategoriaDTO dto) {
        log.info("Creando nueva categoría: {}", dto.getNombre());

        // Validar que no exista otra categoría con el mismo nombre
        if (categoriaRepository.existsByNombreIgnoreCase(dto.getNombre())) {
            throw new RuntimeException("Ya existe una categoría con el nombre: " + dto.getNombre());
        }

        Categoria categoria = convertirAEntidad(dto);
        
        // Si tiene categoría padre, ajustar el nivel automáticamente
        if (dto.getCategoriaPadreId() != null) {
            Categoria padre = categoriaRepository.findById(dto.getCategoriaPadreId())
                    .orElseThrow(() -> new RuntimeException("Categoría padre no encontrada"));
            categoria.setCategoriaPadre(padre);
            categoria.setNivel(padre.getNivel() + 1);
            log.info("Asignando categoría padre: {} con nivel: {}", padre.getNombre(), categoria.getNivel());
        }

        Categoria guardada = categoriaRepository.save(categoria);
        log.info("Categoría creada exitosamente con ID: {}", guardada.getId());
        
        return convertirADTO(guardada);
    }

    /**
     * Actualizar categoría existente
     */
    @Transactional
    public CategoriaDTO actualizar(Long id, CategoriaDTO dto) {
        log.info("Actualizando categoría ID: {}", id);

        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + id));

        // Validar que no exista otra categoría con el mismo nombre
        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(dto.getNombre(), id)) {
            throw new RuntimeException("Ya existe otra categoría con el nombre: " + dto.getNombre());
        }

        // Actualizar campos
        categoria.setNombre(dto.getNombre());
        categoria.setDescripcion(dto.getDescripcion());
        categoria.setPalabrasClave(dto.getPalabrasClave());
        categoria.setActiva(dto.getActiva());
        categoria.setNivel(dto.getNivel());

        // Actualizar categoría padre si cambió
        if (dto.getCategoriaPadreId() != null) {
            if (dto.getCategoriaPadreId().equals(id)) {
                throw new RuntimeException("Una categoría no puede ser su propia categoría padre");
            }
            
            Categoria padre = categoriaRepository.findById(dto.getCategoriaPadreId())
                    .orElseThrow(() -> new RuntimeException("Categoría padre no encontrada"));
            categoria.setCategoriaPadre(padre);
            categoria.setNivel(padre.getNivel() + 1);
            log.info("Actualizando categoría padre a: {}", padre.getNombre());
        } else {
            categoria.setCategoriaPadre(null);
            categoria.setNivel(1);
        }

        Categoria actualizada = categoriaRepository.save(categoria);
        log.info("Categoría actualizada exitosamente");
        
        return convertirADTO(actualizada);
    }

    /**
     * Eliminar categoría
     */
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando categoría ID: {}", id);

        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + id));

        // Verificar si tiene productos asociados
        Long cantidadProductos = categoriaRepository.contarProductosPorCategoria(id);
        if (cantidadProductos > 0) {
            throw new RuntimeException("No se puede eliminar la categoría porque tiene " + 
                                     cantidadProductos + " producto(s) asociado(s)");
        }

        // Verificar si tiene subcategorías
        Long cantidadSubcategorias = categoriaRepository.contarSubcategorias(id);
        if (cantidadSubcategorias > 0) {
            throw new RuntimeException("No se puede eliminar la categoría porque tiene " + 
                                     cantidadSubcategorias + " subcategoría(s)");
        }

        categoriaRepository.delete(categoria);
        log.info("Categoría eliminada exitosamente");
    }

    /**
     * Desactivar categoría (soft delete)
     */
    @Transactional
    public void desactivar(Long id) {
        log.info("Desactivando categoría ID: {}", id);

        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + id));

        categoria.setActiva(false);
        categoriaRepository.save(categoria);
        log.info("Categoría desactivada exitosamente");
    }

    /**
     * Activar categoría
     */
    @Transactional
    public void activar(Long id) {
        log.info("Activando categoría ID: {}", id);

        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + id));

        categoria.setActiva(true);
        categoriaRepository.save(categoria);
        log.info("Categoría activada exitosamente");
    }

    /**
     * Buscar categorías por palabra clave
     */
    @Transactional(readOnly = true)
    public List<CategoriaDTO> buscar(String keyword) {
        log.info("Buscando categorías con keyword: {}", keyword);
        return categoriaRepository.searchByKeyword(keyword)
                .stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtener categorías más utilizadas
     */
    @Transactional(readOnly = true)
    public List<CategoriaDTO> obtenerMasUtilizadas() {
        log.info("Obteniendo categorías más utilizadas");
        return categoriaRepository.findMostUsedCategories()
                .stream()
                .map(this::convertirADTO)
                .limit(10)
                .collect(Collectors.toList());
    }

    /**
     * Contar productos de una categoría
     */
    @Transactional(readOnly = true)
    public Long contarProductos(Long categoriaId) {
        return categoriaRepository.contarProductosPorCategoria(categoriaId);
    }

    /**
     * Contar subcategorías de una categoría
     */
    @Transactional(readOnly = true)
    public Long contarSubcategorias(Long categoriaId) {
        return categoriaRepository.contarSubcategorias(categoriaId);
    }

    // ==================== MÉTODOS DE CONVERSIÓN ====================

    /**
     * Convertir entidad a DTO
     */
    private CategoriaDTO convertirADTO(Categoria entidad) {
        CategoriaDTO dto = new CategoriaDTO();
        dto.setId(entidad.getId());
        dto.setNombre(entidad.getNombre());
        dto.setDescripcion(entidad.getDescripcion());
        dto.setNivel(entidad.getNivel());
        dto.setPalabrasClave(entidad.getPalabrasClave());
        dto.setActiva(entidad.getActiva());

        if (entidad.getCategoriaPadre() != null) {
            dto.setCategoriaPadreId(entidad.getCategoriaPadre().getId());
            dto.setCategoriaPadreNombre(entidad.getCategoriaPadre().getNombre());
        }

        if (entidad.getFechaCreacion() != null) {
            dto.setFechaCreacion(entidad.getFechaCreacion().format(FORMATTER));
        }

        // Contar subcategorías y productos
        dto.setCantidadSubcategorias(entidad.getSubcategorias() != null ? 
                                    entidad.getSubcategorias().size() : 0);
        dto.setCantidadProductos(entidad.getProductos() != null ? 
                                entidad.getProductos().size() : 0);

        return dto;
    }

    /**
     * Convertir DTO a entidad
     */
    private Categoria convertirAEntidad(CategoriaDTO dto) {
        Categoria entidad = new Categoria();
        entidad.setNombre(dto.getNombre());
        entidad.setDescripcion(dto.getDescripcion());
        entidad.setNivel(dto.getNivel() != null ? dto.getNivel() : 1);
        entidad.setPalabrasClave(dto.getPalabrasClave());
        entidad.setActiva(dto.getActiva() != null ? dto.getActiva() : true);

        return entidad;
    }
}
