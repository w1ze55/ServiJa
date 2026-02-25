package com.servija.servija.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.access-key}")
    private String accessKey;

    @Value("${aws.secret-key}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        String cleanAccessKey = accessKey != null ? accessKey.trim() : null;
        String cleanSecretKey = secretKey != null ? secretKey.trim() : null;
        String cleanRegion = region != null ? region.trim() : "sa-east-1";

        System.out.println("S3 Config - Region: " + cleanRegion);
        System.out.println(
                "S3 Config - Access Key: " + (cleanAccessKey != null ? cleanAccessKey.substring(0, 4) + "..." : "null")
                        + " (Length: " + (cleanAccessKey != null ? cleanAccessKey.length() : 0) + ")");
        System.out.println("S3 Config - Secret Key Length: " + (cleanSecretKey != null ? cleanSecretKey.length() : 0));

        AwsBasicCredentials credentials = AwsBasicCredentials.create(cleanAccessKey, cleanSecretKey);

        return S3Client.builder()
                .region(Region.of(cleanRegion))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
}
