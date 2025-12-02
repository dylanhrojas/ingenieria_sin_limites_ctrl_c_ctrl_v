package hackathon.team.dao;

import hackathon.team.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para Producto SIMPLIFICADO
 */
@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

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
     * Buscar producto por nombre exacto (para evitar duplicados)
     */
    @Query("SELECT p FROM Producto p WHERE LOWER(p.nombre) = LOWER(:nombre) AND p.activo = true")
    List<Producto> findByNombreIgnoreCase(@Param("nombre") String nombre);
}