package com.servija.servija.service;

import com.servija.servija.exception.IbgeServiceUnavailableException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class IbgeLocationService {

    private static final String IBGE_BASE_URL = "https://servicodados.ibge.gov.br/api/v1/localidades";
    private static final String STATES_URL = IBGE_BASE_URL + "/estados?orderBy=nome";
    private static final String CITIES_BY_STATE_URL = IBGE_BASE_URL + "/estados/%s/municipios?orderBy=nome";

    private final RestTemplate restTemplate;

    private volatile List<IbgeState> cachedStates;
    private final ConcurrentMap<String, Map<String, String>> cachedCitiesByState = new ConcurrentHashMap<>();

    public List<IbgeState> getStates() {
        List<IbgeState> localCache = cachedStates;
        if (localCache != null) {
            return localCache;
        }

        synchronized (this) {
            if (cachedStates != null) {
                return cachedStates;
            }

            try {
                IbgeState[] response = restTemplate.getForObject(STATES_URL, IbgeState[].class);
                if (response == null) {
                    throw new IbgeServiceUnavailableException("Não foi possível consultar os estados do IBGE.");
                }

                cachedStates = Collections.unmodifiableList(Arrays.stream(response)
                        .filter(Objects::nonNull)
                        .filter(state -> StringUtils.hasText(state.getSigla()))
                        .toList());
                return cachedStates;
            } catch (RestClientException ex) {
                throw new IbgeServiceUnavailableException("Não foi possível consultar os estados do IBGE.", ex);
            }
        }
    }

    public boolean isValidState(String state) {
        if (!StringUtils.hasText(state)) {
            return false;
        }

        String normalizedState = state.trim().toUpperCase(Locale.ROOT);
        return getStates().stream().anyMatch(ibgeState -> ibgeState.getSigla().equalsIgnoreCase(normalizedState));
    }

    public Optional<String> findCanonicalCity(String state, String city) {
        if (!StringUtils.hasText(state) || !StringUtils.hasText(city)) {
            return Optional.empty();
        }

        String normalizedState = state.trim().toUpperCase(Locale.ROOT);
        Map<String, String> citiesByNormalizedName = getCitiesByState(normalizedState);
        return Optional.ofNullable(citiesByNormalizedName.get(normalize(city)));
    }

    private Map<String, String> getCitiesByState(String state) {
        String normalizedState = state.trim().toUpperCase(Locale.ROOT);
        return cachedCitiesByState.computeIfAbsent(normalizedState, this::fetchCitiesByState);
    }

    private Map<String, String> fetchCitiesByState(String state) {
        try {
            IbgeCity[] response = restTemplate.getForObject(CITIES_BY_STATE_URL.formatted(state), IbgeCity[].class);
            if (response == null) {
                throw new IbgeServiceUnavailableException(
                        "Não foi possível consultar as cidades do IBGE para a UF " + state + ".");
            }

            Map<String, String> canonicalCities = new LinkedHashMap<>();
            Arrays.stream(response)
                    .filter(Objects::nonNull)
                    .map(IbgeCity::getNome)
                    .filter(StringUtils::hasText)
                    .forEach(cityName -> canonicalCities.putIfAbsent(normalize(cityName), cityName.trim()));

            return canonicalCities;
        } catch (RestClientException ex) {
            log.warn("Falha ao consultar cidades do IBGE para UF {}", state, ex);
            throw new IbgeServiceUnavailableException(
                    "Não foi possível consultar as cidades do IBGE para a UF " + state + ".", ex);
        }
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    @Data
    public static class IbgeState {
        private Long id;
        private String sigla;
        private String nome;
    }

    @Data
    public static class IbgeCity {
        private Long id;
        private String nome;
    }
}
