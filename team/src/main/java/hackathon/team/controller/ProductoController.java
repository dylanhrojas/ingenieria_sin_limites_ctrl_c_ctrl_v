package hackathon.team.controller;

import hackathon.team.dao.CategoriaRepository;
import hackathon.team.model.Producto;
import hackathon.team.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Controlador de Productos
 * Sistema de Viáticos - OneCard
 */
@Controller
@RequestMapping("/productos")
public class ProductoController {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private CategoriaRepository categoriaRepository;

    /**
     * Listar todos los productos
     */
    @GetMapping
    public String listar(Model model) {
        List<Producto> productos = productoService.findAll();
        model.addAttribute("productos", productos);
        return "productos/lista";
    }

    /**
     * Mostrar formulario para nuevo producto
     */
    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        model.addAttribute("producto", new Producto());
        model.addAttribute("categorias", categoriaRepository.findByActivaTrue());
        model.addAttribute("titulo", "Nuevo Producto");
        return "productos/formulario";
    }

    /**
     * Guardar producto
     */
    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute Producto producto,
                         BindingResult result,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        
        if (result.hasErrors()) {
            model.addAttribute("categorias", categoriaRepository.findByActivaTrue());
            model.addAttribute("titulo", producto.getId() == null ? "Nuevo Producto" : "Editar Producto");
            return "productos/formulario";
        }

        try {
            productoService.guardar(producto);
            redirectAttributes.addFlashAttribute("success", 
                producto.getId() == null ? "Producto creado exitosamente" : "Producto actualizado exitosamente");
            return "redirect:/productos";
        } catch (Exception e) {
            model.addAttribute("error", "Error al guardar el producto: " + e.getMessage());
            model.addAttribute("categorias", categoriaRepository.findByActivaTrue());
            model.addAttribute("titulo", producto.getId() == null ? "Nuevo Producto" : "Editar Producto");
            return "productos/formulario";
        }
    }

    /**
     * Mostrar formulario para editar producto
     */
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Producto producto = productoService.findById(id);
        
        if (producto == null) {
            redirectAttributes.addFlashAttribute("error", "Producto no encontrado");
            return "redirect:/productos";
        }

        model.addAttribute("producto", producto);
        model.addAttribute("categorias", categoriaRepository.findByActivaTrue());
        model.addAttribute("titulo", "Editar Producto");
        return "productos/formulario";
    }

    /**
     * Eliminar producto (desactivar)
     */
    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productoService.eliminar(id);
            redirectAttributes.addFlashAttribute("success", "Producto eliminado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar el producto: " + e.getMessage());
        }
        
        return "redirect:/productos";
    }

    /**
     * Buscar productos (búsqueda semántica)
     */
    @GetMapping("/buscar")
    public String buscar(@RequestParam(required = false) String q, Model model) {
        List<Producto> productos;
        
        if (q != null && !q.trim().isEmpty()) {
            productos = productoService.buscarSemantico(q);
            model.addAttribute("busqueda", q);
        } else {
            productos = productoService.findAll();
        }
        
        model.addAttribute("productos", productos);
        return "productos/lista";
    }
}