package com.servija.servija.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IbgeLocationServiceTest {

    private static final String STATES_URL =
            "https://servicodados.ibge.gov.br/api/v1/localidades/estados?orderBy=nome";
    private static final String SP_CITIES_URL =
            "https://servicodados.ibge.gov.br/api/v1/localidades/estados/SP/municipios?orderBy=nome";

    @Mock
    private RestTemplate restTemplate;

    @Test
    @DisplayName("Deve cachear a lista de estados")
    void isValidState_usesCachedStates() {
        IbgeLocationService.IbgeState saoPaulo = new IbgeLocationService.IbgeState();
        saoPaulo.setId(35L);
        saoPaulo.setSigla("SP");
        saoPaulo.setNome("Sao Paulo");

        IbgeLocationService.IbgeState rioDeJaneiro = new IbgeLocationService.IbgeState();
        rioDeJaneiro.setId(33L);
        rioDeJaneiro.setSigla("RJ");
        rioDeJaneiro.setNome("Rio de Janeiro");

        when(restTemplate.getForObject(STATES_URL, IbgeLocationService.IbgeState[].class))
                .thenReturn(new IbgeLocationService.IbgeState[] { saoPaulo, rioDeJaneiro });

        IbgeLocationService ibgeLocationService = new IbgeLocationService(restTemplate);

        assertThat(ibgeLocationService.isValidState("SP")).isTrue();
        assertThat(ibgeLocationService.isValidState("rj")).isTrue();
        assertThat(ibgeLocationService.isValidState("XX")).isFalse();

        verify(restTemplate, times(1)).getForObject(STATES_URL, IbgeLocationService.IbgeState[].class);
    }

    @Test
    @DisplayName("Deve cachear cidades por UF e retornar o nome canônico")
    void findCanonicalCity_usesCachedCitiesByState() {
        IbgeLocationService.IbgeCity saoPaulo = new IbgeLocationService.IbgeCity();
        saoPaulo.setId(3550308L);
        saoPaulo.setNome("Sao Paulo");

        IbgeLocationService.IbgeCity campinas = new IbgeLocationService.IbgeCity();
        campinas.setId(3509502L);
        campinas.setNome("Campinas");

        when(restTemplate.getForObject(SP_CITIES_URL, IbgeLocationService.IbgeCity[].class))
                .thenReturn(new IbgeLocationService.IbgeCity[] { saoPaulo, campinas });

        IbgeLocationService ibgeLocationService = new IbgeLocationService(restTemplate);

        Optional<String> firstLookup = ibgeLocationService.findCanonicalCity("SP", "sao paulo");
        Optional<String> secondLookup = ibgeLocationService.findCanonicalCity("SP", "Campinas");

        assertThat(firstLookup).contains("Sao Paulo");
        assertThat(secondLookup).contains("Campinas");

        verify(restTemplate, times(1)).getForObject(SP_CITIES_URL, IbgeLocationService.IbgeCity[].class);
    }
}
