package hackathon.team.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad Categoria
 * Conector Semántico - OneCard
 */
@Entity
@Table(name = "categoria")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_padre_id")
    private Categoria categoriaPadre;

    @OneToMany(mappedBy = "categoriaPadre", fetch = FetchType.LAZY)
    private Set<Categoria> subcategorias = new HashSet<>();

    @Column(nullable = false)
    private Integer nivel = 1;

    @Column(name = "palabras_clave", columnDefinition = "TEXT")
    private String palabrasClave;

    @Column(nullable = false)
    private Boolean activa = true;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "categoria", fetch = FetchType.LAZY)
    private Set<Producto> productos = new HashSet<>();

    /**
     * Constructor con nombre
     */
    public Categoria(String nombre) {
        this.nombre = nombre;
        this.activa = true;
        this.nivel = 1;
    }

    /**
     * Constructor con nombre y palabras clave
     */
    public Categoria(String nombre, String palabrasClave) {
        this.nombre = nombre;
        this.palabrasClave = palabrasClave;
        this.activa = true;
        this.nivel = 1;
    }

    /**
     * Método helper para verificar si tiene subcategorías
     */
    @Transient
    public boolean tieneSubcategorias() {
        return subcategorias != null && !subcategorias.isEmpty();
    }

    /**
     * Método helper para verificar si es categoría raíz
     */
    @Transient
    public boolean esRaiz() {
        return categoriaPadre == null;
    }
}