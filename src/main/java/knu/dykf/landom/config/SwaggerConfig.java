package knu.dykf.landom.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Collections;
import java.util.List;

@Configuration
@Profile({"dev", "local"})
public class SwaggerConfig {

    @Value("${server.url}")
    private String serverUrl;

    @Bean
    public OpenAPI openAPI() {
        String jwtSchemeName = "jwtAuth";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));

        return new OpenAPI()
                .components(components)
                .info(apiInfo())
                .addSecurityItem(securityRequirement)
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("로컬 개발 서버"),
                        new Server().url(serverUrl).description("배포 서버")
                ));
    }

    @Bean
    public OperationCustomizer operationCustomizer() {
        return (operation, handlerMethod) -> {
            String className = handlerMethod.getBeanType().getSimpleName();
            if ("AuthController".equals(className)) {
                operation.setSecurity(Collections.emptyList());
            }
            return operation;
        };
    }

    private Info apiInfo() {
        return new Info()
                .title("Land-Om API 명세서")
                .description("Land-Om API 문서입니다.")
                .version("1.0.0");
    }
}