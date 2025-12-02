package hackathon.team.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;

/**
 * DTO para Categoria
 * Conector Semántico - OneCard
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaDTO {

    private Long id;

    @NotBlank(message = "El nombre de la categoría es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres")
    private String nombre;

    @Size(max = 2000, message = "La descripción no puede exceder los 2000 caracteres")
    private String descripcion;

    private Long categoriaPadreId;
    
    private String categoriaPadreNombre;

    @NotNull(message = "El nivel es obligatorio")
    @Min(value = 1, message = "El nivel mínimo es 1")
    @Max(value = 5, message = "El nivel máximo es 5")
    private Integer nivel;

    @NotBlank(message = "Las palabras clave son obligatorias")
    private String palabrasClave;

    @NotNull(message = "El estado de la categoría es obligatorio")
    private Boolean activa;

    private Integer cantidadSubcategorias;
    
    private Integer cantidadProductos;

    private String fechaCreacion;

    /**
     * Constructor simplificado para crear nueva categoría
     */
    public CategoriaDTO(String nombre, String palabrasClave) {
        this.nombre = nombre;
        this.palabrasClave = palabrasClave;
        this.activa = true;
        this.nivel = 1;
    }

    /**
     * Constructor para listados
     */
    public CategoriaDTO(Long id, String nombre, Integer nivel, Boolean activa, Integer cantidadProductos) {
        this.id = id;
        this.nombre = nombre;
        this.nivel = nivel;
        this.activa = activa;
        this.cantidadProductos = cantidadProductos;
    }
}