package com.mazhar.portfolio.content;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Reads the site's content from the JSON files under {@code src/main/resources/content}.
 * Those files are generated from the Angular app's data files
 * ({@code npm run export:content} in the portfolio project), so the API serves exactly
 * what the site shows.
 *
 * <p>Loading is deliberately strict. A property in the JSON that the DTO does not
 * declare would otherwise be dropped silently — the Angular side gained a field and
 * the API quietly stopped serving it — so it fails the application at startup instead,
 * with the file name in the message.
 */
@Component
public class ContentLoader {

    private static final String CONTENT_DIR = "content/";

    private final ObjectMapper mapper = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public <T> List<T> loadList(String fileName, Class<T> elementType) {
        JavaType listType = mapper.getTypeFactory().constructCollectionType(List.class, elementType);
        List<T> items = read(fileName, listType);
        return List.copyOf(items);
    }

    public <T> T loadObject(String fileName, Class<T> type) {
        return read(fileName, mapper.getTypeFactory().constructType(type));
    }

    private <T> T read(String fileName, JavaType type) {
        ClassPathResource resource = new ClassPathResource(CONTENT_DIR + fileName);
        try (InputStream in = resource.getInputStream()) {
            return mapper.readValue(in, type);
        } catch (IOException e) {
            throw new IllegalStateException(
                "Could not load content file '" + CONTENT_DIR + fileName + "': " + e.getMessage(), e);
        }
    }
}
