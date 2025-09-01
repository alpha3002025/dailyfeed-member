package click.dailyfeed.member.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI(){
        Info info = new Info()
                .title("booksfeed member")
                .version("0.8")
                .description("This is a booksfeed member API");

        return new OpenAPI()
                .components(new Components())
                .info(info);
    }
}
