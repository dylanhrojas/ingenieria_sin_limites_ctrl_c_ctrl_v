package hackathon.team.dao;

import hackathon.team.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para Ticket
 * Conector Semántico - OneCard
 */
@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /**
     * Buscar ticket por número
     */
    Optional<Ticket> findByNumeroTicket(String numeroTicket);

    /**
     * Buscar tickets por usuario
     */
    @Query("SELECT t FROM Ticket t WHERE t.usuario.id = :usuarioId ORDER BY t.fechaHora DESC")
    List<Ticket> findByUsuario(@Param("usuarioId") Long usuarioId);

    /**
     * Buscar tickets por estado
     */
    List<Ticket> findByEstado(String estado);

    /**
     * Buscar tickets por rango de fechas
     */
    @Query("SELECT t FROM Ticket t WHERE t.fechaHora BETWEEN :fechaInicio AND :fechaFin ORDER BY t.fechaHora DESC")
    List<Ticket> findByRangoFechas(@Param("fechaInicio") LocalDateTime fechaInicio, 
                                    @Param("fechaFin") LocalDateTime fechaFin);

    /**
     * Buscar tickets del día actual (versión compatible)
     */
    @Query("SELECT t FROM Ticket t WHERE CAST(t.fechaHora AS LocalDate) = :fecha ORDER BY t.fechaHora DESC")
    List<Ticket> findTicketsPorFecha(@Param("fecha") LocalDate fecha);

    /**
     * Calcular ventas totales del día (versión compatible)
     */
    @Query("SELECT COALESCE(SUM(t.total), 0) FROM Ticket t WHERE CAST(t.fechaHora AS LocalDate) = :fecha AND t.estado = 'completado'")
    BigDecimal calcularVentasPorFecha(@Param("fecha") LocalDate fecha);

    /**
     * Calcular ventas totales por rango de fechas
     */
    @Query("SELECT COALESCE(SUM(t.total), 0) FROM Ticket t WHERE t.fechaHora BETWEEN :fechaInicio AND :fechaFin AND t.estado = 'completado'")
    BigDecimal calcularVentasPorRango(@Param("fechaInicio") LocalDateTime fechaInicio, 
                                      @Param("fechaFin") LocalDateTime fechaFin);

    /**
     * Contar tickets del día (versión compatible)
     */
    @Query("SELECT COUNT(t) FROM Ticket t WHERE CAST(t.fechaHora AS LocalDate) = :fecha")
    Long contarTicketsPorFecha(@Param("fecha") LocalDate fecha);

    /**
     * Buscar últimos tickets
     */
    @Query("SELECT t FROM Ticket t ORDER BY t.fechaHora DESC")
    List<Ticket> findUltimosTickets();

    /**
     * Buscar tickets por método de pago
     */
    @Query("SELECT t FROM Ticket t WHERE t.metodoPago = :metodoPago AND t.estado = 'completado'")
    List<Ticket> findByMetodoPago(@Param("metodoPago") String metodoPago);

    /**
     * Usuarios que compraron productos de una categoría
     */
    @Query("SELECT DISTINCT t.usuario FROM Ticket t " +
           "JOIN t.items ti " +
           "JOIN ti.producto p " +
           "JOIN p.categoria c " +
           "WHERE LOWER(c.nombre) LIKE LOWER(CONCAT('%', :categoria, '%')) " +
           "AND t.estado = 'completado'")
    List<Object[]> findUsuariosPorCategoria(@Param("categoria") String categoria);

    /**
     * Obtener los últimos números de ticket
     */
    @Query("SELECT t.numeroTicket FROM Ticket t " +
           "WHERE t.numeroTicket LIKE :prefijo " +
           "ORDER BY t.id DESC")
    List<String> findTicketsPorPrefijo(@Param("prefijo") String prefijo);
}