package searchengine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ForkJoinPoolFactoryBean;

@Configuration
public class ForkJoinPoolConfiguration {

    @Bean
    public ForkJoinPoolFactoryBean forkJoinPoolFactoryBean() {
        return new ForkJoinPoolFactoryBean();
    }
}
