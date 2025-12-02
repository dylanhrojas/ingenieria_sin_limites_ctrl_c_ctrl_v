package hackathon.team.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO para subir tickets con productos e imagen
 * Conector Semántico - OneCard
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketUploadDTO {

    private Long id;

    @NotNull(message = "Debe seleccionar un usuario")
    private Long usuarioId;

    @NotNull(message = "El total es obligatorio")
    @DecimalMin(value = "0.01", message = "El total debe ser mayor a 0")
    private BigDecimal total;

    private BigDecimal subtotal;

    private BigDecimal impuestos;

    private BigDecimal descuentos;

    @Size(max = 50, message = "El método de pago no puede exceder 50 caracteres")
    private String metodoPago;

    @Size(max = 2000, message = "Las observaciones no pueden exceder 2000 caracteres")
    private String observaciones;

    // Imagen del ticket
    @NotNull(message = "Debe subir una imagen del ticket")
    private MultipartFile imagenTicket;

    private String rutaImagen;

    // Lista de productos del ticket
    @NotEmpty(message = "Debe agregar al menos un producto")
    private List<ProductoTicketDTO> productos = new ArrayList<>();

    /**
     * DTO interno para productos del ticket
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductoTicketDTO {
        
        @NotNull(message = "Debe ingresar el nombre del producto")
        @Size(min = 1, max = 500, message = "El nombre del producto debe tener entre 1 y 500 caracteres")
        private String nombreProducto;

        @Size(max = 200, message = "La marca no puede exceder 200 caracteres")
        private String marca;

        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "1", message = "La cantidad debe ser al menos 1")
        private Integer cantidad;

        @NotNull(message = "El precio unitario es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
        private BigDecimal precioUnitario;

        private BigDecimal descuento;

        private Long categoriaId;

        /**
         * Calcula el subtotal del producto
         */
        public BigDecimal getSubtotal() {
            if (precioUnitario == null || cantidad == null) {
                return BigDecimal.ZERO;
            }
            BigDecimal subtotalBase = precioUnitario.multiply(new BigDecimal(cantidad));
            if (descuento != null) {
                return subtotalBase.subtract(descuento);
            }
            return subtotalBase;
        }
    }

    /**
     * Agregar producto a la lista
     */
    public void agregarProducto(ProductoTicketDTO producto) {
        if (productos == null) {
            productos = new ArrayList<>();
        }
        productos.add(producto);
    }

    /**
     * Calcular total basado en productos
     */
    public void calcularTotal() {
        if (productos == null || productos.isEmpty()) {
            this.subtotal = BigDecimal.ZERO;
            this.total = BigDecimal.ZERO;
            return;
        }

        this.subtotal = productos.stream()
                .map(ProductoTicketDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.total = this.subtotal
                .add(this.impuestos != null ? this.impuestos : BigDecimal.ZERO)
                .subtract(this.descuentos != null ? this.descuentos : BigDecimal.ZERO);
    }
}