package uteq.edu.ec.crecuteq;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "scopus.catalog.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:crecuteq-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "spring.task.scheduling.enabled=false",
        "jwt.secret=clave-exclusiva-para-pruebas-automatizadas-1234567890",
        "scopus.api.key=clave-ficticia-pruebas",
        "spring.mail.username=pruebas@localhost",
        "spring.mail.password=clave-ficticia-pruebas"
})
class CrecuteqApplicationTests {

    @Test
    void contextLoads() {
    }

}
