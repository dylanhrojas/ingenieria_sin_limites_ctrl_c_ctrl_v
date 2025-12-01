package hackathon.team.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad Producto
 * Conector Semántico - OneCard
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

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 100)
    private String marca;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "codigo_barras", unique = true, length = 50)
    private String codigoBarras;

    @Column(unique = true, length = 50)
    private String sku;

    @Column(name = "unidad_medida", length = 20)
    private String unidadMedida;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(name = "imagen_url", length = 255)
    private String imagenUrl;

    @Column(name = "stock_actual", nullable = false)
    private Integer stockActual = 0;

    @Column(nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @OneToMany(mappedBy = "producto", fetch = FetchType.LAZY)
    private Set<TicketItem> ticketItems = new HashSet<>();

    /**
     * Método helper para obtener el nombre completo (nombre + marca)
     */
    @Transient
    public String getNombreCompleto() {
        return nombre + (marca != null ? " - " + marca : "");
    }

    /**
     * Método helper para verificar si hay stock disponible
     */
    @Transient
    public boolean tieneStock() {
        return stockActual != null && stockActual > 0;
    }

    /**
     * Método helper para verificar si el stock es bajo
     */
    @Transient
    public boolean esStockBajo() {
        return stockActual != null && stockActual < 10;
    }

    /**
     * Método para reducir stock
     */
    public void reducirStock(Integer cantidad) {
        if (this.stockActual >= cantidad) {
            this.stockActual -= cantidad;
        } else {
            throw new IllegalStateException("Stock insuficiente");
        }
    }

    /**
     * Método para aumentar stock
     */
    public void aumentarStock(Integer cantidad) {
        this.stockActual += cantidad;
    }
}