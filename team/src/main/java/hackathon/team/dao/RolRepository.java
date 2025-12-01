package hackathon.team.dao;

import hackathon.team.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para Rol
 * Conector Semántico - OneCard
 */
@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {

    /**
     * Buscar rol por nombre
     */
    Optional<Rol> findByNombre(String nombre);

    /**
     * Verificar si existe rol por nombre
     */
    boolean existsByNombre(String nombre);

    /**
     * Buscar roles activos
     */
    List<Rol> findByActivoTrue();

    /**
     * Contar usuarios por rol
     */
    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.rol.id = :rolId")
    Long contarUsuariosPorRol(Long rolId);
}