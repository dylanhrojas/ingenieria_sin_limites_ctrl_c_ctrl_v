package hackathon.team.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad Ticket (Venta)
 * Conector Semántico - OneCard
 */
@Entity
@Table(name = "ticket")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_ticket", nullable = false, unique = true, length = 50)
    private String numeroTicket;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @CreationTimestamp
    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 10, scale = 2)
    private BigDecimal impuestos = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal descuentos = BigDecimal.ZERO;

    @Column(name = "metodo_pago", length = 50)
    private String metodoPago;

    @Column(length = 20)
    private String estado = "completado";

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TicketItem> items = new ArrayList<>();

    /**
     * Método helper para agregar un item al ticket
     */
    public void agregarItem(TicketItem item) {
        items.add(item);
        item.setTicket(this);
    }

    /**
     * Método helper para remover un item del ticket
     */
    public void removerItem(TicketItem item) {
        items.remove(item);
        item.setTicket(null);
    }

    /**
     * Método helper para calcular el total del ticket
     */
    public void calcularTotal() {
        this.subtotal = items.stream()
                .map(TicketItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        this.total = this.subtotal
                .add(this.impuestos != null ? this.impuestos : BigDecimal.ZERO)
                .subtract(this.descuentos != null ? this.descuentos : BigDecimal.ZERO);
    }

    /**
     * Método helper para obtener el número de items
     */
    @Transient
    public int getCantidadItems() {
        return items != null ? items.size() : 0;
    }

    /**
     * Método helper para verificar si el ticket está completado
     */
    @Transient
    public boolean estaCompletado() {
        return "completado".equalsIgnoreCase(estado);
    }

    /**
     * Método helper para verificar si el ticket está cancelado
     */
    @Transient
    public boolean estaCancelado() {
        return "cancelado".equalsIgnoreCase(estado);
    }
}