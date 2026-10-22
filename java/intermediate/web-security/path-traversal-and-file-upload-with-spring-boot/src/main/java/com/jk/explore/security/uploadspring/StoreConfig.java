package com.jk.explore.security.uploadspring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Which versions run is chosen by profile, so one controller serves all three. The layout is the demo's own temporary folder. */
@Configuration
class StoreConfig implements WebMvcConfigurer {

    private final Layout layout;

    StoreConfig(Layout layout) {
        this.layout = layout;
    }

    /** Spring's own handler for static files: the invoice folder as it would be served by "just map the folder". */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static-invoices/**").addResourceLocations(layout.invoices().toUri().toString());
    }

    @Bean
    @Profile("naive")
    Downloads naiveDownloads() {
        return new NaiveDownloads(layout);
    }

    @Bean
    @Profile("prefix")
    Downloads prefixDownloads() {
        return new PrefixDownloads(layout);
    }

    @Bean
    @Profile("safe")
    Downloads safeDownloads() {
        return new SafeDownloads(layout);
    }

    @Bean
    @Profile("naive | prefix")
    Uploads naiveUploads() {
        return new NaiveUploads(layout);
    }

    @Bean
    @Profile("safe")
    Uploads safeUploads() {
        return new SafeUploads(layout);
    }

    @Bean
    @Profile("naive | prefix")
    Redirects naiveRedirects() {
        return Redirects.NAIVE;
    }

    @Bean
    @Profile("safe")
    Redirects safeRedirects() {
        return Redirects.SAFE;
    }
}
