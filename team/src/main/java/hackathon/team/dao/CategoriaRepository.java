package hackathon.team.dao;

import hackathon.team.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para Categoria
 * Conector Semántico - OneCard
 */
@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    /**
     * Buscar categoría por nombre
     */
    Optional<Categoria> findByNombre(String nombre);

    /**
     * Buscar categorías activas
     */
    List<Categoria> findByActivaTrue();

    /**
     * Buscar categorías raíz (sin padre)
     */
    @Query("SELECT c FROM Categoria c WHERE c.categoriaPadre IS NULL AND c.activa = true")
    List<Categoria> findCategoriasRaiz();

    /**
     * Buscar subcategorías de una categoría padre
     */
    @Query("SELECT c FROM Categoria c WHERE c.categoriaPadre.id = :padreId AND c.activa = true")
    List<Categoria> findSubcategorias(@Param("padreId") Long padreId);

    /**
     * Buscar categorías por nivel
     */
    @Query("SELECT c FROM Categoria c WHERE c.nivel = :nivel AND c.activa = true")
    List<Categoria> findByNivel(@Param("nivel") Integer nivel);

    /**
     * Búsqueda semántica por palabras clave
     */
    @Query("SELECT c FROM Categoria c WHERE " +
           "LOWER(c.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(c.palabrasClave) LIKE LOWER(CONCAT('%', :busqueda, '%'))")
    List<Categoria> buscarPorPalabrasClave(@Param("busqueda") String busqueda);

    /**
     * Contar productos por categoría
     */
    @Query("SELECT COUNT(p) FROM Producto p WHERE p.categoria.id = :categoriaId")
    Long contarProductosPorCategoria(@Param("categoriaId") Long categoriaId);

    /**
     * Verificar si existe categoría con nombre
     */
    boolean existsByNombre(String nombre);
}