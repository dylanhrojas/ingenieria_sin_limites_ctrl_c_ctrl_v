package hackathon.team.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Entidad TicketItem (Items de la venta)
 * Conector Semántico - OneCard
 */
@Entity
@Table(name = "ticket_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 10, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO;

    /**
     * Constructor con los campos principales
     */
    public TicketItem(Producto producto, Integer cantidad, BigDecimal precioUnitario) {
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.calcularSubtotal();
    }

    /**
     * Método para calcular el subtotal
     */
    public void calcularSubtotal() {
        BigDecimal subtotalBase = this.precioUnitario.multiply(new BigDecimal(this.cantidad));
        this.subtotal = subtotalBase.subtract(this.descuento != null ? this.descuento : BigDecimal.ZERO);
    }

    /**
     * Método helper para obtener el nombre del producto
     */
    @Transient
    public String getNombreProducto() {
        return producto != null ? producto.getNombreCompleto() : "";
    }
}