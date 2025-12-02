package hackathon.team.controller;

import hackathon.team.dtos.CategoriaDTO;
import hackathon.team.service.CategoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

/**
 * Controller para gestión de Categorías
 * Conector Semántico - OneCard
 */
@Controller
@RequestMapping("/categorias")
@RequiredArgsConstructor
@Slf4j
public class CategoriaController {

    private final CategoriaService categoriaService;

    /**
     * Listar todas las categorías
     */
    @GetMapping
    public String listar(Model model) {
        log.info("GET /categorias - Listando categorías");
        try {
            List<CategoriaDTO> categorias = categoriaService.obtenerTodas();
            
            // Calcular estadísticas
            long totalActivas = categorias.stream().filter(c -> c.getActiva()).count();
            long totalRaiz = categorias.stream().filter(c -> c.getCategoriaPadreId() == null).count();
            long totalSubcategorias = categorias.stream().filter(c -> c.getCategoriaPadreId() != null).count();
            
            model.addAttribute("categorias", categorias);
            model.addAttribute("totalActivas", totalActivas);
            model.addAttribute("totalRaiz", totalRaiz);
            model.addAttribute("totalSubcategorias", totalSubcategorias);
            model.addAttribute("titulo", "Gestión de Categorías");
            return "categorias/lista";
        } catch (Exception e) {
            log.error("Error al listar categorías", e);
            model.addAttribute("error", "Error al cargar las categorías: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Mostrar formulario para nueva categoría
     */
    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        log.info("GET /categorias/nuevo - Mostrando formulario nueva categoría");
        try {
            CategoriaDTO categoria = new CategoriaDTO();
            categoria.setActiva(true);
            categoria.setNivel(1);
            
            model.addAttribute("categoria", categoria);
            model.addAttribute("categorias", categoriaService.obtenerCategoriasRaizActivas());
            model.addAttribute("titulo", "Nueva Categoría");
            model.addAttribute("esNuevo", true);
            
            return "categorias/formulario";
        } catch (Exception e) {
            log.error("Error al mostrar formulario nueva categoría", e);
            model.addAttribute("error", "Error al cargar el formulario: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Mostrar formulario para editar categoría
     */
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model) {
        log.info("GET /categorias/editar/{} - Mostrando formulario editar categoría", id);
        try {
            CategoriaDTO categoria = categoriaService.obtenerPorId(id);
            
            model.addAttribute("categoria", categoria);
            model.addAttribute("categorias", categoriaService.obtenerCategoriasRaizActivas());
            model.addAttribute("titulo", "Editar Categoría");
            model.addAttribute("esNuevo", false);
            
            return "categorias/formulario";
        } catch (Exception e) {
            log.error("Error al mostrar formulario editar categoría ID: {}", id, e);
            model.addAttribute("error", "Error al cargar la categoría: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Guardar nueva categoría
     */
    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("categoria") CategoriaDTO categoria,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        log.info("POST /categorias/guardar - Guardando categoría: {}", categoria.getNombre());

        // Validar errores de validación
        if (result.hasErrors()) {
            log.warn("Errores de validación en formulario de categoría");
            model.addAttribute("categorias", categoriaService.obtenerCategoriasRaizActivas());
            model.addAttribute("titulo", "Nueva Categoría");
            model.addAttribute("esNuevo", true);
            return "categorias/formulario";
        }

        try {
            CategoriaDTO guardada = categoriaService.crear(categoria);
            log.info("Categoría creada exitosamente con ID: {}", guardada.getId());
            
            redirectAttributes.addFlashAttribute("mensaje", 
                "Categoría '" + guardada.getNombre() + "' creada exitosamente");
            redirectAttributes.addFlashAttribute("tipoMensaje", "success");
            
            return "redirect:/categorias";
        } catch (Exception e) {
            log.error("Error al guardar categoría", e);
            model.addAttribute("error", "Error al guardar la categoría: " + e.getMessage());
            model.addAttribute("categorias", categoriaService.obtenerCategoriasRaizActivas());
            model.addAttribute("titulo", "Nueva Categoría");
            model.addAttribute("esNuevo", true);
            return "categorias/formulario";
        }
    }

    /**
     * Actualizar categoría existente
     */
    @PostMapping("/actualizar/{id}")
    public String actualizar(@PathVariable Long id,
                            @Valid @ModelAttribute("categoria") CategoriaDTO categoria,
                            BindingResult result,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        log.info("POST /categorias/actualizar/{} - Actualizando categoría", id);

        // Validar errores de validación
        if (result.hasErrors()) {
            log.warn("Errores de validación en formulario de categoría");
            model.addAttribute("categorias", categoriaService.obtenerCategoriasRaizActivas());
            model.addAttribute("titulo", "Editar Categoría");
            model.addAttribute("esNuevo", false);
            return "categorias/formulario";
        }

        try {
            CategoriaDTO actualizada = categoriaService.actualizar(id, categoria);
            log.info("Categoría actualizada exitosamente");
            
            redirectAttributes.addFlashAttribute("mensaje", 
                "Categoría '" + actualizada.getNombre() + "' actualizada exitosamente");
            redirectAttributes.addFlashAttribute("tipoMensaje", "success");
            
            return "redirect:/categorias";
        } catch (Exception e) {
            log.error("Error al actualizar categoría ID: {}", id, e);
            model.addAttribute("error", "Error al actualizar la categoría: " + e.getMessage());
            model.addAttribute("categorias", categoriaService.obtenerCategoriasRaizActivas());
            model.addAttribute("titulo", "Editar Categoría");
            model.addAttribute("esNuevo", false);
            return "categorias/formulario";
        }
    }

    /**
     * Eliminar categoría
     */
    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("POST /categorias/eliminar/{} - Eliminando categoría", id);
        
        try {
            CategoriaDTO categoria = categoriaService.obtenerPorId(id);
            categoriaService.eliminar(id);
            log.info("Categoría eliminada exitosamente");
            
            redirectAttributes.addFlashAttribute("mensaje", 
                "Categoría '" + categoria.getNombre() + "' eliminada exitosamente");
            redirectAttributes.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            log.error("Error al eliminar categoría ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", 
                "Error al eliminar la categoría: " + e.getMessage());
            redirectAttributes.addFlashAttribute("tipoMensaje", "danger");
        }
        
        return "redirect:/categorias";
    }

    /**
     * Desactivar categoría (soft delete)
     */
    @PostMapping("/desactivar/{id}")
    public String desactivar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("POST /categorias/desactivar/{} - Desactivando categoría", id);
        
        try {
            CategoriaDTO categoria = categoriaService.obtenerPorId(id);
            categoriaService.desactivar(id);
            log.info("Categoría desactivada exitosamente");
            
            redirectAttributes.addFlashAttribute("mensaje", 
                "Categoría '" + categoria.getNombre() + "' desactivada exitosamente");
            redirectAttributes.addFlashAttribute("tipoMensaje", "warning");
        } catch (Exception e) {
            log.error("Error al desactivar categoría ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", 
                "Error al desactivar la categoría: " + e.getMessage());
            redirectAttributes.addFlashAttribute("tipoMensaje", "danger");
        }
        
        return "redirect:/categorias";
    }

    /**
     * Activar categoría
     */
    @PostMapping("/activar/{id}")
    public String activar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("POST /categorias/activar/{} - Activando categoría", id);
        
        try {
            CategoriaDTO categoria = categoriaService.obtenerPorId(id);
            categoriaService.activar(id);
            log.info("Categoría activada exitosamente");
            
            redirectAttributes.addFlashAttribute("mensaje", 
                "Categoría '" + categoria.getNombre() + "' activada exitosamente");
            redirectAttributes.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            log.error("Error al activar categoría ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", 
                "Error al activar la categoría: " + e.getMessage());
            redirectAttributes.addFlashAttribute("tipoMensaje", "danger");
        }
        
        return "redirect:/categorias";
    }

    /**
     * Ver detalle de categoría
     */
    @GetMapping("/detalle/{id}")
    public String verDetalle(@PathVariable Long id, Model model) {
        log.info("GET /categorias/detalle/{} - Viendo detalle de categoría", id);
        
        try {
            CategoriaDTO categoria = categoriaService.obtenerPorId(id);
            
            // Obtener subcategorías si las tiene
            if (categoria.getCantidadSubcategorias() > 0) {
                model.addAttribute("subcategorias", categoriaService.obtenerSubcategorias(id));
            }
            
            // Contar productos y subcategorías
            Long cantidadProductos = categoriaService.contarProductos(id);
            Long cantidadSubcategorias = categoriaService.contarSubcategorias(id);
            
            model.addAttribute("categoria", categoria);
            model.addAttribute("cantidadProductos", cantidadProductos);
            model.addAttribute("cantidadSubcategorias", cantidadSubcategorias);
            model.addAttribute("titulo", "Detalle de Categoría");
            
            return "categorias/detalle";
        } catch (Exception e) {
            log.error("Error al ver detalle de categoría ID: {}", id, e);
            model.addAttribute("error", "Error al cargar el detalle: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Buscar categorías
     */
    @GetMapping("/buscar")
    public String buscar(@RequestParam(required = false) String keyword, Model model) {
        log.info("GET /categorias/buscar - Buscando categorías con keyword: {}", keyword);
        
        try {
            if (keyword != null && !keyword.trim().isEmpty()) {
                model.addAttribute("categorias", categoriaService.buscar(keyword));
                model.addAttribute("keyword", keyword);
            } else {
                model.addAttribute("categorias", categoriaService.obtenerTodas());
            }
            
            model.addAttribute("titulo", "Búsqueda de Categorías");
            return "categorias/lista";
        } catch (Exception e) {
            log.error("Error al buscar categorías", e);
            model.addAttribute("error", "Error en la búsqueda: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Obtener subcategorías vía AJAX
     */
    @GetMapping("/subcategorias/{padreId}")
    @ResponseBody
    public List<CategoriaDTO> obtenerSubcategorias(@PathVariable Long padreId) {
        log.info("GET /categorias/subcategorias/{} - Obteniendo subcategorías", padreId);
        return categoriaService.obtenerSubcategorias(padreId);
    }

    /**
     * API: Obtener todas las categorías activas (JSON)
     */
    @GetMapping("/api/activas")
    @ResponseBody
    public List<CategoriaDTO> obtenerCategoriasActivas() {
        log.info("GET /categorias/api/activas - Obteniendo categorías activas");
        return categoriaService.obtenerActivas();
    }

    /**
     * API: Obtener categorías raíz activas (JSON)
     */
    @GetMapping("/api/raiz")
    @ResponseBody
    public List<CategoriaDTO> obtenerCategoriasRaiz() {
        log.info("GET /categorias/api/raiz - Obteniendo categorías raíz");
        return categoriaService.obtenerCategoriasRaizActivas();
    }
}