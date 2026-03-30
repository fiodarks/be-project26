package com.github.fiodarks.project26.adapter.in.web.api;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.NativeWebRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class ApiUtil {

    private ApiUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void setExampleResponse(NativeWebRequest request, String contentType, String example) {
        if (request == null || contentType == null || example == null) {
            return;
        }

        try {
            HttpServletResponse response = request.getNativeResponse(HttpServletResponse.class);
            if (response == null || response.isCommitted()) {
                return;
            }

            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(contentType);
            response.getWriter().print(example);
        } catch (IOException ignored) {
            // This is best-effort for Swagger/OpenAPI example responses in generated interfaces.
        }
    }
}

