package hackathon.team.configure;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración de Spring MVC
 * Conector Semántico - OneCard
 */
@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configuración de recursos estáticos (CSS, JS, imágenes)
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // CSS
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCachePeriod(3600);

        // JavaScript
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCachePeriod(3600);

        // Imágenes
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCachePeriod(3600);

        // Favicon
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);

        // Recursos adicionales (fonts, etc.)
        registry.addResourceHandler("/fonts/**")
                .addResourceLocations("classpath:/static/fonts/")
                .setCachePeriod(3600);

        // WebJars (Bootstrap, jQuery, etc. desde Maven)
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/")
                .setCachePeriod(3600);
    }

    /**
     * Configuración de controladores de vistas simples
     * Para páginas que no necesitan lógica en el controller
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Redirigir la raíz a la landing page
        registry.addViewController("/").setViewName("forward:/landing");
        
        // Vista de login (manejada por Spring Security)
        registry.addViewController("/login").setViewName("login");
        
        // Vista de error 404
        registry.addViewController("/404").setViewName("error/404");
        
        // Vista de error 403 (acceso denegado)
        registry.addViewController("/403").setViewName("error/403");
        
        // Vista de error 500
        registry.addViewController("/500").setViewName("error/500");
    }
}