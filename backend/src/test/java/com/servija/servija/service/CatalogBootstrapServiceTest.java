package com.servija.servija.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CatalogBootstrapServiceTest {

    @Mock
    private CategoryCatalogService categoryCatalogService;

    @InjectMocks
    private CatalogBootstrapService service;

    @Test
    @DisplayName("run executa seed e backfill do catalogo")
    void runExecutesSeedAndBackfill() {
        service.run(null);

        verify(categoryCatalogService).seedCatalogIfNeeded();
        verify(categoryCatalogService).backfillLegacyAssignments();
    }
}
