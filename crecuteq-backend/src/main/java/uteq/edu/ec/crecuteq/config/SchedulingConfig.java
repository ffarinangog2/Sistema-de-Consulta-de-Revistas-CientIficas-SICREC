package uteq.edu.ec.crecuteq.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

// INICIO - Descarga automática Springer
@Configuration
@EnableScheduling
public class SchedulingConfig {
    /** Exclusión mutua para todas las descargas automáticas de catálogos. */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        // Cada proveedor ya impide sus propios solapamientos; varios hilos
        // evitan que una descarga lenta bloquee todos los demás catálogos.
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("catalog-download-");
        return scheduler;
    }
}
// FIN - Descarga automática Springer
