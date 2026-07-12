package com.kobe.warehouse.batch.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.JobOperatorFactoryBean;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.time.LocalTime;

@Configuration
public class BatchConfiguration {

    @Bean
    public JobOperator asyncJobOperator(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        ApplicationContext applicationContext
    ) throws Exception {
        // JobOperatorFactoryBean implémente ApplicationContextAware : Spring n'appelle
        // setApplicationContext(...) automatiquement que pour les beans qu'il instancie
        // lui-même. Ici l'objet est construit à la main (new), donc ce callback ne se
        // déclenche jamais si on ne le fait pas explicitement — afterPropertiesSet()
        // plantait avec "this.applicationContext" null (JobOperator interne cherchant
        // les JobExecutionListener via applicationContext.getBeansOfType(...)).
        JobOperatorFactoryBean factory = new JobOperatorFactoryBean();
        factory.setJobRepository(jobRepository);
        factory.setTransactionManager(transactionManager);
        factory.setTaskExecutor(new SimpleAsyncTaskExecutor("batch-"));
        factory.setApplicationContext(applicationContext);
        factory.afterPropertiesSet();
        return factory.getObject();
    }



    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JavaTimeModule javaTimeModule() {
        final JavaTimeModule javaTime = new JavaTimeModule();
        javaTime.addSerializer(
            LocalTime.class,
            new JsonSerializer<>() {
                @Override
                public void serialize(LocalTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                    gen.writeString(value.toString());
                }
            }
        );
        return javaTime;
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
            .addModule(javaTimeModule())
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
    }
}
