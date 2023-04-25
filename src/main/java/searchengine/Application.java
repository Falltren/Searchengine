package searchengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);

    }

//    @Bean
//    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
//        return args -> {
//
//            System.out.println("Let's inspect the beans provided by Spring Boot:");
//
//            String[] beanNames = ctx.getBeanDefinitionNames();
//            Object myBean = ctx.getBean(Crawler.class);
//            System.out.println(myBean.getClass());
//            Field[] beanFields = myBean.getClass().getDeclaredFields();
//            for (Field field : beanFields) {
//                System.out.println(field);
//            }
//        };
//    }
}
