package hackathon.team.controller;

import hackathon.team.dtos.LoginDTO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controlador de Autenticación
 * Conector Semántico - OneCard
 */
@Controller
public class AuthController {

    /**
     * Página de login
     */
    @GetMapping("/login")
    public String login(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "expired", required = false) String expired,
            Model model) {
        
        model.addAttribute("loginDTO", new LoginDTO());
        
        if (error != null) {
            model.addAttribute("error", "Usuario o contraseña incorrectos");
        }
        
        if (logout != null) {
            model.addAttribute("mensaje", "Has cerrado sesión exitosamente");
        }
        
        if (expired != null) {
            model.addAttribute("error", "Tu sesión ha expirado. Por favor, inicia sesión nuevamente");
        }
        
        return "login";
    }
}
