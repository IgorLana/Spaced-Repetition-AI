package com.spaced_repetition_ai.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudfront.cookie.SignedCookie;
import software.amazon.awssdk.services.cloudfront.model.CustomSignerRequest;

import software.amazon.awssdk.services.cloudfront.CloudFrontUtilities;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Slf4j
public class AwsService {


    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${cloudfront.domain-name}")
    private String cloudFrontDomain;

    @Value("${cloudfront.key-pair-id}")
    private String keyPairId;

    @Value("${cloudfront.private-key-path}")
    private String privateKeyPath;

    private final S3Presigner s3Presigner;

    public AwsService(S3Presigner s3Presigner) {
        this.s3Presigner = s3Presigner;
    }

    public String generatePresignedUploadUrl(Long userId, String fileName) {
        String s3Key = "users/" + userId + "/flashcards/" + UUID.randomUUID() + "-" + fileName;

        String contentType = determineContentType(fileName);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(java.time.Duration.ofMinutes(5))
                .putObjectRequest(putObjectRequest)
                .build();

        String url = s3Presigner.presignPutObject(presignRequest).url().toString();
        log.info("Presigned URL: {}", url);
        log.info("S3 Key: {}", s3Key);

        return url;
    }

    public Map<String, String> getSignedCloudFrontCookies(Long userId) {
        try {
            // A URL do recurso para a política é o domínio e o caminho base dos arquivos.
            String policyResourcePath = "https://" + cloudFrontDomain + "/users/" + userId + "/flashcards/*";

            // Carregue a chave privada a partir do arquivo
            PrivateKey privateKey = loadPrivateKeyFromFile(privateKeyPath);

            CloudFrontUtilities cloudFrontUtilities = CloudFrontUtilities.create();

            CustomSignerRequest customSignerRequest = CustomSignerRequest.builder()
                    .resourceUrl(policyResourcePath) // URL base do recurso.
                    .privateKey(privateKey) // Usa o objeto PrivateKey carregado.
                    .keyPairId(keyPairId)
                    .expirationDate(Instant.now().plus(12, ChronoUnit.HOURS))
                    .build();

            SignedCookie signedCookie = cloudFrontUtilities.getCookiesForCustomPolicy(customSignerRequest);

            Map<String, String> cookies = new HashMap<>();
            cookies.put("CloudFront-Key-Pair-Id", signedCookie.keyPairIdHeaderValue());
            cookies.put("CloudFront-Signature", signedCookie.signatureHeaderValue());
            cookies.put("CloudFront-Policy", ((software.amazon.awssdk.services.cloudfront.cookie.CookiesForCustomPolicy) signedCookie).policyHeaderValue());

            log.info("Cookies assinados do CloudFront gerados para o usuário {}", userId);
            return cookies;

        } catch (Exception e) {
            log.error("Erro ao gerar cookies assinados do CloudFront.", e);
            return Collections.emptyMap();
        }
    }

    private PrivateKey loadPrivateKeyFromFile(String keyFilePath) throws Exception {
        try {
            String keyContent = new String(Files.readAllBytes(Paths.get(keyFilePath)));

            // Remove os cabeçalhos e rodapés do arquivo PEM
            String privateKeyPEM = keyContent
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            // Decodifica a chave de base64
            byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);

            // Cria a chave privada a partir da especificação PKCS8
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);

            return keyFactory.generatePrivate(keySpec);
        } catch (IOException e) {
            log.error("Erro ao ler o arquivo de chave privada.", e);
            throw new RuntimeException("Não foi possível ler o arquivo de chave privada.", e);
        }
    }

    private String determineContentType(String fileName) {
        String extension = "";
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            extension = fileName.substring(lastDot + 1).toLowerCase();
        }

        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            default -> "application/octet-stream";
        };
    }
}


