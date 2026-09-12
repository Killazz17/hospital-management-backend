package hospital.example.DataAccess;

import hospital.example.Domain.models.*;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.io.IOException;
import java.util.Properties;

public class HibernateUtil {

    private static final SessionFactory sessionFactory;

    static {
        try {
            Properties properties = new Properties();
            properties.load(HibernateUtil.class.getClassLoader()
                .getResourceAsStream("hibernate.properties"));

            // Las credenciales no se versionan en el repositorio: cuando estas
            // variables de entorno estan definidas, tienen prioridad sobre el
            // archivo de propiedades.
            aplicarVariableDeEntorno(properties, "hibernate.connection.url", "DB_URL");
            aplicarVariableDeEntorno(properties, "hibernate.connection.username", "DB_USER");
            aplicarVariableDeEntorno(properties, "hibernate.connection.password", "DB_PASSWORD");

            sessionFactory = new Configuration()
                    .setProperties(properties)
                    .addAnnotatedClass(Usuario.class)
                    .addAnnotatedClass(Admin.class)
                    .addAnnotatedClass(Paciente.class)
                    .addAnnotatedClass(Medico.class)
                    .addAnnotatedClass(Farmaceuta.class)
                    .addAnnotatedClass(Medicamento.class)
                    .addAnnotatedClass(MedicamentoPrescrito.class)
                    .addAnnotatedClass(Receta.class)
                    .buildSessionFactory();

        } catch (IOException e) {
            throw new ExceptionInInitializerError(
                "Could not load hibernate.properties: " + e.getMessage());
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(
                "Initial SessionFactory creation failed: " + ex);
        }
    }

    private static void aplicarVariableDeEntorno(Properties properties, String propiedad, String variable) {
        String valor = System.getenv(variable);
        if (valor != null && !valor.isBlank()) {
            properties.setProperty(propiedad, valor);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}
