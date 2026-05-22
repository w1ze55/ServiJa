package com.servija.servija;

import io.sentry.Sentry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ServijaApplication {

	public static void main(String[] args) {
		initSentry();
		SpringApplication.run(ServijaApplication.class, args);
	}

	private static void initSentry() {
		String dsn = System.getenv("SENTRY_DSN");
		if (dsn == null || dsn.isBlank()) {
			return;
		}
		Sentry.init(options -> {
			options.setDsn(dsn);
			options.setEnvironment(orDefault(System.getenv("SENTRY_ENVIRONMENT"), "local"));
			String release = System.getenv("SENTRY_RELEASE");
			if (release != null && !release.isBlank()) {
				options.setRelease(release);
			}
			options.setTracesSampleRate(parseDoubleOrDefault(System.getenv("SENTRY_TRACES_SAMPLE_RATE"), 0.01));
			options.setDebug("true".equalsIgnoreCase(System.getenv("SENTRY_DEBUG")));
			options.setEnableAutoSessionTracking(false);
			options.setSendDefaultPii(false);
		});
	}

	private static String orDefault(String value, String fallback) {
		return (value == null || value.isBlank()) ? fallback : value;
	}

	private static double parseDoubleOrDefault(String value, double fallback) {
		if (value == null || value.isBlank()) {
			return fallback;
		}
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

}
