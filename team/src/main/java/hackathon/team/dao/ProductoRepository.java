package hackathon.team.dao;

import hackathon.team.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para Producto
 * Conector Semántico - OneCard
 */
@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    /**
     * Buscar producto por código de barras
     */
    Optional<Producto> findByCodigoBarras(String codigoBarras);

    /**
     * Buscar producto por SKU
     */
    Optional<Producto> findBySku(String sku);

    /**
     * Buscar productos activos
     */
    List<Producto> findByActivoTrue();

    /**
     * Buscar productos por categoría
     */
    @Query("SELECT p FROM Producto p WHERE p.categoria.id = :categoriaId AND p.activo = true")
    List<Producto> findByCategoria(@Param("categoriaId") Long categoriaId);

    /**
     * Buscar productos por nombre de categoría
     */
    @Query("SELECT p FROM Producto p WHERE LOWER(p.categoria.nombre) = LOWER(:nombreCategoria) AND p.activo = true")
    List<Producto> findByCategoriaNombre(@Param("nombreCategoria") String nombreCategoria);

    /**
     * Búsqueda semántica de productos
     * Busca en: nombre, marca, categoría y palabras clave
     */
    @Query("SELECT DISTINCT p FROM Producto p " +
           "LEFT JOIN p.categoria c " +
           "WHERE p.activo = true AND (" +
           "LOWER(p.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(p.marca) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(c.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(c.palabrasClave) LIKE LOWER(CONCAT('%', :busqueda, '%')))")
    List<Producto> buscarSemantico(@Param("busqueda") String busqueda);

    /**
     * Buscar productos con stock bajo (menos de 10 unidades)
     */
    @Query("SELECT p FROM Producto p WHERE p.stockActual < 10 AND p.activo = true")
    List<Producto> findProductosConStockBajo();

    /**
     * Buscar productos sin stock
     */
    @Query("SELECT p FROM Producto p WHERE p.stockActual = 0 AND p.activo = true")
    List<Producto> findProductosSinStock();

    /**
     * Buscar productos por marca
     */
    @Query("SELECT p FROM Producto p WHERE LOWER(p.marca) LIKE LOWER(CONCAT('%', :marca, '%')) AND p.activo = true")
    List<Producto> findByMarca(@Param("marca") String marca);

    /**
     * Contar productos activos
     */
    @Query("SELECT COUNT(p) FROM Producto p WHERE p.activo = true")
    Long contarProductosActivos();

    /**
     * Productos más vendidos
     */
    @Query("SELECT p FROM Producto p " +
           "JOIN p.ticketItems ti " +
           "GROUP BY p " +
           "ORDER BY SUM(ti.cantidad) DESC")
    List<Producto> findProductosMasVendidos();
}
