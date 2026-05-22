package com.servija.servija.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

// Endpoint temporario para validar o pipeline Sentry/GlitchTip. Remover apos confirmar evento no dashboard.
@RestController
@RequestMapping("/api/__sentry-test")
public class SentryTestController {

	@GetMapping
	public void trigger() {
		throw new RuntimeException("GlitchTip pipeline check " + Instant.now());
	}
}
