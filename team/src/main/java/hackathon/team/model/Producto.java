package hackathon.team.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad Producto SIMPLIFICADA
 * Solo campos esenciales para clasificación
 */
@Entity
@Table(name = "productos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String nombre;

    @Column(length = 200)
    private String marca;

    @Column(name = "precio_referencia", precision = 10, scale = 2)
    private BigDecimal precioReferencia;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /**
     * Método helper para obtener el nombre completo
     */
    @Transient
    public String getNombreCompleto() {
        return nombre + (marca != null ? " - " + marca : "");
    }
}