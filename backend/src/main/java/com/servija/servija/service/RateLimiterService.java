package com.servija.servija.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * H5: limitador de taxa simples, em memória (janela fixa por chave).
 *
 * Suficiente para uma instância única. Para múltiplas instâncias por trás de um
 * balanceador, troque por um backend compartilhado (ex.: Redis/Bucket4j).
 */
@Service
public class RateLimiterService {

    private static final class Window {
        long resetAtMillis;
        int count;
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * Consome 1 unidade do balde {@code key}. Retorna {@code true} se permitido,
     * {@code false} se o limite {@code maxRequests} dentro de {@code windowMillis} foi excedido.
     */
    public synchronized boolean tryConsume(String key, int maxRequests, long windowMillis) {
        long now = System.currentTimeMillis();
        pruneIfNeeded(now);

        Window window = windows.get(key);
        if (window == null || now >= window.resetAtMillis) {
            window = new Window();
            window.resetAtMillis = now + windowMillis;
            windows.put(key, window);
        }

        if (window.count >= maxRequests) {
            return false;
        }
        window.count++;
        return true;
    }

    /** Evita crescimento ilimitado do mapa (proteção contra flood de chaves distintas). */
    private void pruneIfNeeded(long now) {
        if (windows.size() > 50_000) {
            windows.entrySet().removeIf(entry -> now >= entry.getValue().resetAtMillis);
        }
    }
}
