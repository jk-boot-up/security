package com.jk.explore.security.ssrfspring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Which importer the shop runs is chosen by profile, so one controller serves all four versions. */
@Configuration
class ImporterConfig {

    @Bean
    Fetcher fetcher() {
        return new Fetcher();
    }

    @Bean
    @Profile("naive")
    Importer naive(Dns dns, Router router, Fetcher fetcher) {
        return new NaiveImporter(dns, router, fetcher);
    }

    @Bean
    @Profile("blocklist")
    Importer blocklist(Dns dns, Router router, Fetcher fetcher) {
        return new BlocklistImporter(dns, router, fetcher);
    }

    @Bean
    @Profile("two-step")
    Importer twoStep(Dns dns, Router router, Fetcher fetcher) {
        return new TwoStepImporter(dns, router, fetcher);
    }

    @Bean
    @Profile("guarded")
    Importer guarded(Dns dns, Router router, Fetcher fetcher) {
        return new GuardedImporter(dns, router, fetcher);
    }
}
