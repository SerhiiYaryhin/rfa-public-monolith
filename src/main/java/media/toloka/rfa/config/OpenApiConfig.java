package media.toloka.rfa.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenApiCustomizer customizeTags() {
        return openApi -> {
            if (openApi.getPaths() != null) {
                openApi.getPaths().forEach((path, pathItem) -> {
                    pathItem.readOperations().forEach(operation -> {
                        // Замість kebab-case залишаємо оригінальне ім'я класу з Java
                        // наприклад, міняємо "admin-post-controller" на "AdminPostController"
                        java.util.List<String> updatedTags = operation.getTags().stream()
                                .map(tag -> {
                                    // Перетворюємо назад у CamelCase (робимо першу літеру великою)
                                    String[] words = tag.split("-");
                                    StringBuilder sb = new StringBuilder();
                                    for (String word : words) {
                                        if (!word.isEmpty()) {
                                            sb.append(Character.toUpperCase(word.charAt(0)))
                                                    .append(word.substring(1));
                                        }
                                    }
                                    return sb.toString(); // Поверне "AdminPostController"
                                })
                                .toList();
                        operation.setTags(updatedTags);
                    });
                });
            }
        };
    }
}

