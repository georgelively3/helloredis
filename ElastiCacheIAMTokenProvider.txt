package com.lithespeed.helloredis.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Generates an AWS SigV4 pre-signed auth token for ElastiCache IAM
 * authentication.
 * The token is used as the Redis AUTH password and expires after 15 minutes.
 *
 * Equivalent to Python's ElastiCacheIAMProvider.
 */
@Component
@ConditionalOnProperty(name = "aws.elasticache.enabled", havingValue = "true")
@Slf4j
public class ElastiCacheIAMTokenProvider {

    private static final String SERVICE = "elasticache";
    private static final String ALGORITHM = "AWS4-HMAC-SHA256";
    private static final String EMPTY_PAYLOAD_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Value("${aws.elasticache.cluster-name}")
    private String clusterName;

    @Value("${aws.elasticache.region}")
    private String region;

    @Value("${aws.elasticache.iam-username}")
    private String iamUsername;

    @Value("${aws.elasticache.port}")
    private int port;

    private final DefaultCredentialsProvider awsCredentialsProvider = DefaultCredentialsProvider.create();

    /**
     * Generates a fresh IAM auth token. Tokens expire after 15 minutes;
     * call this again to get a new one before reconnecting.
     */
    public String generateToken() {
        try {
            AwsCredentials credentials = awsCredentialsProvider.resolveCredentials();

            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
            String dateTimeStr = now.format(DATE_TIME_FORMATTER);
            String dateStr = now.format(DATE_FORMATTER);

            String credentialScope = String.join("/", dateStr, region, SERVICE, "aws4_request");
            String credentialParam = credentials.accessKeyId() + "/" + credentialScope;

            // TreeMap ensures query params are sorted alphabetically (required by SigV4)
            Map<String, String> rawParams = new TreeMap<>();
            rawParams.put("Action", "connect");
            rawParams.put("User", iamUsername);
            rawParams.put("X-Amz-Algorithm", ALGORITHM);
            rawParams.put("X-Amz-Credential", credentialParam);
            rawParams.put("X-Amz-Date", dateTimeStr);
            rawParams.put("X-Amz-Expires", "900");
            if (credentials instanceof AwsSessionCredentials session) {
                rawParams.put("X-Amz-Security-Token", session.sessionToken());
            }
            rawParams.put("X-Amz-SignedHeaders", "host");

            String canonicalQueryString = rawParams.entrySet().stream()
                    .map(e -> percentEncode(e.getKey()) + "=" + percentEncode(e.getValue()))
                    .collect(Collectors.joining("&"));

            // Build the SigV4 canonical request
            String host = clusterName + ":" + port;
            String canonicalRequest = String.join("\n",
                    "GET", "/", canonicalQueryString,
                    "host:" + host, "", // canonical headers; blank line = end of headers section
                    "host", // signed headers
                    EMPTY_PAYLOAD_HASH);

            // Build string-to-sign
            String stringToSign = String.join("\n",
                    ALGORITHM,
                    dateTimeStr,
                    credentialScope,
                    sha256Hex(canonicalRequest));

            // Derive signing key and compute signature
            byte[] signingKey = deriveSigningKey(credentials.secretAccessKey(), dateStr, region, SERVICE);
            String signature = hmacHex(signingKey, stringToSign);

            // The token is the presigned URL without the https:// scheme
            String token = host + "/?" + canonicalQueryString + "&X-Amz-Signature=" + signature;
            log.debug("Generated ElastiCache IAM auth token for user={}", iamUsername);
            return token;

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate ElastiCache IAM auth token", e);
        }
    }

    private byte[] deriveSigningKey(String secretKey, String date, String region, String service)
            throws Exception {
        byte[] kSecret = ("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8);
        byte[] kDate = hmac(kSecret, date);
        byte[] kRegion = hmac(kDate, region);
        byte[] kService = hmac(kRegion, service);
        return hmac(kService, "aws4_request");
    }

    private byte[] hmac(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private String hmacHex(byte[] key, String data) throws Exception {
        return bytesToHex(hmac(key, data));
    }

    private String sha256Hex(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return bytesToHex(digest.digest(data.getBytes(StandardCharsets.UTF_8)));
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /** RFC 3986 percent-encoding as required by AWS SigV4. */
    private String percentEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
    }
}
