package com.github.fiodarks.project26.config;

import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatMultipartLimitsConfig {

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatMultipartLimitsCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            // Some environments end up with an unexpectedly low max-part-count which causes
            // multipart parsing to fail with FileCountLimitExceededException.
            connector.setMaxPartCount(1000);
        });
    }
}
