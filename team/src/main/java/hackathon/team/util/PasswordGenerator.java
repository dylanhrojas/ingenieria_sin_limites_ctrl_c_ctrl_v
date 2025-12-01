package hackathon.team.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Generador de contraseñas hasheadas con BCrypt
 * Conector Semántico - OneCard
 */
public class PasswordGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        System.out.println("=================================================");
        System.out.println("CONTRASEÑAS HASHEADAS CON BCRYPT");
        System.out.println("=================================================\n");
        
        // Contraseñas de ejemplo
        String[] passwords = {
            "admin123",
            "marketing123",
            "inventario123",
            "cajero123"
        };
        
        String[] usuarios = {
            "admin@onecard.com",
            "gerardo.marketing@onecard.com",
            "laura.inventario@onecard.com",
            "carlos.cajero@onecard.com"
        };
        
        for (int i = 0; i < passwords.length; i++) {
            String hashedPassword = encoder.encode(passwords[i]);
            System.out.println("Usuario: " + usuarios[i]);
            System.out.println("Password: " + passwords[i]);
            System.out.println("Hash: " + hashedPassword);
            System.out.println();
        }
        
        System.out.println("=================================================");
        System.out.println("SCRIPT SQL PARA ACTUALIZAR LA BASE DE DATOS");
        System.out.println("=================================================\n");
        
        System.out.println("-- Actualizar contraseñas hasheadas");
        for (int i = 0; i < passwords.length; i++) {
            String hashedPassword = encoder.encode(passwords[i]);
            System.out.println("UPDATE usuarios SET password = '" + hashedPassword + "' WHERE email = '" + usuarios[i] + "';");
        }
        
        System.out.println("\n=================================================");
        System.out.println("¡Listo! Copia el script SQL y ejecútalo en PostgreSQL");
        System.out.println("=================================================");
    }
}