package Chruch_Of_God_Dindigul.Bible_quize;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
// Add this annotation to explicitly tell Spring where to find your repository interfaces.
@EnableJpaRepositories(basePackages = "Chruch_Of_God_Dindigul.Bible_quize.repository")
// Add this annotation to explicitly tell Spring where to find your @Entity classes.
@EntityScan(basePackages = "Chruch_Of_God_Dindigul.Bible_quize.model")
public class BibleQuizeApplication {

	public static void main(String[] args) {
		SpringApplication.run(BibleQuizeApplication.class, args);
	}
}