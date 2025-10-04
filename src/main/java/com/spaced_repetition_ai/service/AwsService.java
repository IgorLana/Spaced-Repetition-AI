package com.spaced_repetition_ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.cloudfront.cookie.CookiesForCustomPolicy;
import software.amazon.awssdk.services.cloudfront.model.CustomSignerRequest;

import software.amazon.awssdk.services.cloudfront.CloudFrontUtilities;
import software.amazon.awssdk.services.s3.S3Client;
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

    private final S3Client s3Client;


    public AwsService(S3Presigner s3Presigner, S3Client s3Client) {
        this.s3Presigner = s3Presigner;
        this.s3Client = s3Client;
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
            String policyResourcePath = "https://" + cloudFrontDomain + "/users/" + userId + "/*";

            PrivateKey privateKey = loadPrivateKeyFromFile(privateKeyPath);

            CloudFrontUtilities cloudFrontUtilities = CloudFrontUtilities.create();

            CustomSignerRequest customSignerRequest = CustomSignerRequest.builder()
                    .resourceUrl(policyResourcePath)
                    .privateKey(privateKey)
                    .keyPairId(keyPairId)
                    .expirationDate(Instant.now().plus(12, ChronoUnit.HOURS))
                    .build();

            CookiesForCustomPolicy signedCookie = cloudFrontUtilities.getCookiesForCustomPolicy(customSignerRequest);

            Map<String, String> cookies = new HashMap<>();


            String keyPairIdValue = extractCookieValue(signedCookie.keyPairIdHeaderValue());
            String signatureValue = extractCookieValue(signedCookie.signatureHeaderValue());
            String policyValue = extractCookieValue(signedCookie.policyHeaderValue());


            cookies.put("CloudFront-Key-Pair-Id", keyPairIdValue);
            cookies.put("CloudFront-Signature", signatureValue);
            cookies.put("CloudFront-Policy", policyValue);

            log.info("Policy Resource Path: {}", policyResourcePath);
            log.info("Key Pair ID: {}", keyPairId);
            log.info("CloudFront Domain: {}", cloudFrontDomain);

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

            byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);

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

    private String extractCookieValue(String headerValue) {
        if (headerValue == null || !headerValue.contains("=")) {
            return "";
        }
        return headerValue.substring(headerValue.indexOf('=') + 1);
    }


    public String uploadPublicMedia(byte[] mediaBytes, String mediaType, String extension) {
        try {
            String fileName = UUID.randomUUID().toString() + "." + extension;
            String s3Key = "public/" + mediaType + "/" + fileName;
            String contentType = determineContentType(fileName);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(mediaBytes));

            log.info("Mídia pública salva no S3: {}", s3Key);

            return "/" + s3Key;

        } catch (Exception e) {
            log.error("Erro ao fazer upload de mídia pública no S3.", e);
            return null;
        }
    }


    public String downloadFileAsBase64(String s3Key) {
        try {
            // Remove a barra inicial se houver
            String cleanKey = s3Key.startsWith("/") ? s3Key.substring(1) : s3Key;

            byte[] fileBytes = s3Client.getObjectAsBytes(builder -> builder
                    .bucket(bucketName)
                    .key(cleanKey)
            ).asByteArray();

            String base64 = Base64.getEncoder().encodeToString(fileBytes);
            log.info("Arquivo baixado do S3 e convertido para base64: {}", cleanKey);

            return base64;

        } catch (Exception e) {
            log.error("Erro ao baixar arquivo do S3: {}", s3Key, e);
            return null;
        }
    }



}


