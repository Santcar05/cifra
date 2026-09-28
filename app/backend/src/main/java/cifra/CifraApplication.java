package cifra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

@Modulithic(systemName = "CIFRA")
@SpringBootApplication
public class CifraApplication {

    public static void main(String[] args) {
        SpringApplication.run(CifraApplication.class, args);
    }
}
