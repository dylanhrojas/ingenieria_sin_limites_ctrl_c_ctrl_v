package hackathon.team.dao;

import hackathon.team.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository para Categoria
 * Conector Semántico - OneCard
 */
@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    /**
     * Buscar categoría por nombre (case insensitive)
     */
    Optional<Categoria> findByNombreIgnoreCase(String nombre);

    /**
     * Verificar si existe una categoría con ese nombre
     */
    boolean existsByNombreIgnoreCase(String nombre);

    /**
     * Verificar si existe una categoría con ese nombre excluyendo un ID específico
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Categoria c WHERE LOWER(c.nombre) = LOWER(:nombre) AND c.id != :id")
    boolean existsByNombreIgnoreCaseAndIdNot(@Param("nombre") String nombre, @Param("id") Long id);

    /**
     * Obtener todas las categorías activas
     */
    List<Categoria> findByActivaTrue();

    /**
     * Obtener todas las categorías ordenadas por nombre
     */
    List<Categoria> findAllByOrderByNombreAsc();

    /**
     * Obtener categorías raíz (sin padre)
     */
    @Query("SELECT c FROM Categoria c WHERE c.categoriaPadre IS NULL ORDER BY c.nombre ASC")
    List<Categoria> findCategoriasRaiz();

    /**
     * Obtener categorías raíz activas
     */
    @Query("SELECT c FROM Categoria c WHERE c.categoriaPadre IS NULL AND c.activa = true ORDER BY c.nombre ASC")
    List<Categoria> findCategoriasRaizActivas();

    /**
     * Obtener subcategorías de una categoría padre
     */
    @Query("SELECT c FROM Categoria c WHERE c.categoriaPadre.id = :padreId ORDER BY c.nombre ASC")
    List<Categoria> findSubcategoriasByPadreId(@Param("padreId") Long padreId);

    /**
     * Obtener categorías por nivel
     */
    List<Categoria> findByNivel(Integer nivel);

    /**
     * Obtener categorías activas por nivel
     */
    List<Categoria> findByNivelAndActivaTrue(Integer nivel);

    /**
     * Buscar categorías por palabra clave (búsqueda en nombre, descripción y palabras clave)
     */
    @Query("SELECT c FROM Categoria c WHERE " +
           "LOWER(c.nombre) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.descripcion) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.palabrasClave) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Categoria> searchByKeyword(@Param("keyword") String keyword);

    /**
     * Contar productos por categoría
     */
    @Query("SELECT COUNT(p) FROM Producto p WHERE p.categoria.id = :categoriaId")
    Long contarProductosPorCategoria(@Param("categoriaId") Long categoriaId);

    /**
     * Contar subcategorías de una categoría
     */
    @Query("SELECT COUNT(c) FROM Categoria c WHERE c.categoriaPadre.id = :padreId")
    Long contarSubcategorias(@Param("padreId") Long padreId);

    /**
     * Obtener categorías con sus contadores de productos
     */
    @Query("SELECT c, COUNT(p) FROM Categoria c LEFT JOIN c.productos p GROUP BY c ORDER BY c.nombre ASC")
    List<Object[]> findAllWithProductCount();

    /**
     * Obtener categorías más utilizadas (con más productos)
     */
    @Query("SELECT c FROM Categoria c LEFT JOIN c.productos p WHERE c.activa = true GROUP BY c ORDER BY COUNT(p) DESC")
    List<Categoria> findMostUsedCategories();

    /**
     * Obtener todas las palabras clave únicas
     */
    @Query("SELECT DISTINCT c.palabrasClave FROM Categoria c WHERE c.activa = true AND c.palabrasClave IS NOT NULL")
    List<String> findAllPalabrasClave();

    /**
     * Buscar categorías que contengan una palabra clave específica
     */
    @Query("SELECT c FROM Categoria c WHERE c.activa = true AND LOWER(c.palabrasClave) LIKE LOWER(CONCAT('%', :palabra, '%'))")
    List<Categoria> findByPalabraClaveContaining(@Param("palabra") String palabra);
}