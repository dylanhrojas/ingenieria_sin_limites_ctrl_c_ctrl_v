package hackathon.team.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador principal para Landing Page y Home
 * Conector Semántico - OneCard
 */
@Controller
public class HomeController {

    /**
     * Landing Page (página pública de inicio)
     */
    @GetMapping("/landing")
    public String landing(Model model) {
        model.addAttribute("titulo", "Conector Semántico - OneCard");
        model.addAttribute("descripcion", "Sistema inteligente de búsqueda y gestión de productos");
        return "landing";
    }

    /**
     * Dashboard (página principal después del login)
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("titulo", "Dashboard");
        return "dashboard";
    }

    /**
     * Página de error 404
     */
    @GetMapping("/404")
    public String error404(Model model) {
        model.addAttribute("mensaje", "Página no encontrada");
        return "error/404";
    }

    /**
     * Página de error 403 (Acceso denegado)
     */
    @GetMapping("/403")
    public String error403(Model model) {
        model.addAttribute("mensaje", "No tienes permiso para acceder a esta página");
        return "error/403";
    }

    /**
     * Página de error 500
     */
    @GetMapping("/500")
    public String error500(Model model) {
        model.addAttribute("mensaje", "Error interno del servidor");
        return "error/500";
    }
}