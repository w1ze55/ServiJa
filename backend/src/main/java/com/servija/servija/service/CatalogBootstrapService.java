package com.servija.servija.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
@RequiredArgsConstructor
@Slf4j
public class CatalogBootstrapService implements ApplicationRunner {

    private final CategoryCatalogService categoryCatalogService;

    @Override
    public void run(ApplicationArguments args) {
        categoryCatalogService.seedCatalogIfNeeded();
        categoryCatalogService.backfillLegacyAssignments();
        log.info("Catálogo de categorias inicializado");
    }
}
