package hackathon.team.service;

import hackathon.team.dao.ProductoRepository;
import hackathon.team.model.Producto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio para gestión de Productos SIMPLIFICADO
 */
@Service
@Transactional
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    /**
     * Obtener todos los productos activos
     */
    public List<Producto> findAll() {
        return productoRepository.findByActivoTrue();
    }

    /**
     * Buscar producto por ID
     */
    public Producto findById(Long id) {
        return productoRepository.findById(id).orElse(null);
    }

    /**
     * Guardar producto
     */
    public Producto guardar(Producto producto) {
        // Validaciones básicas
        if (producto.getNombre() == null || producto.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del producto es obligatorio");
        }

        if (producto.getCategoria() == null || producto.getCategoria().getId() == null) {
            throw new IllegalArgumentException("Debe seleccionar una categoría");
        }

        // Si es nuevo producto, asegurar que esté activo
        if (producto.getId() == null) {
            producto.setActivo(true);
        }

        return productoRepository.save(producto);
    }

    /**
     * Eliminar producto (desactivar)
     */
    public void eliminar(Long id) {
        Producto producto = findById(id);
        if (producto != null) {
            producto.setActivo(false);
            productoRepository.save(producto);
        }
    }

    /**
     * Búsqueda semántica
     */
    public List<Producto> buscarSemantico(String busqueda) {
        if (busqueda == null || busqueda.trim().isEmpty()) {
            return findAll();
        }
        return productoRepository.buscarSemantico(busqueda.trim());
    }

    /**
     * Buscar por categoría
     */
    public List<Producto> findByCategoria(Long categoriaId) {
        return productoRepository.findByCategoria(categoriaId);
    }

    /**
     * Verificar si ya existe un producto con ese nombre
     */
    public boolean existeProductoConNombre(String nombre) {
        List<Producto> productos = productoRepository.findByNombreIgnoreCase(nombre);
        return !productos.isEmpty();
    }
}