package lk.avix.http;

import lk.avix.key.PrivateKey;
import lk.avix.key.PublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.UUID;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class Client {

    private static final String truststore = "pkcs12Truststore.p12";
    private static final String truststorePassword = "ballerina";
    private static final String keystore = "pkcs12Keystore.p12";
    private static final String keystorePassword = "ballerina";
    private static final String trustedCert = "x509Public.crt";
    private static final String privateKey = "pkcs8Private.key";
    private static final String publicCert = "x509Public.crt";

    public static void main(String[] args) throws Exception {
        String truststoreFile = Objects.requireNonNull(Client.class.getClassLoader().getResource(truststore)).getFile();
        String truststorePath = new File(truststoreFile).getAbsolutePath();
        String keystoreFile = Objects.requireNonNull(Client.class.getClassLoader().getResource(keystore)).getFile();
        String keystorePath = new File(keystoreFile).getAbsolutePath();
        String trustedCertFile = Objects.requireNonNull(Client.class.getClassLoader().getResource(trustedCert)).getFile();
        String trustedCertPath = new File(trustedCertFile).getAbsolutePath();
        String privateKeyFile = Objects.requireNonNull(Client.class.getClassLoader().getResource(privateKey)).getFile();
        String privateKeyPath = new File(privateKeyFile).getAbsolutePath();
        String publicCertFile = Objects.requireNonNull(Client.class.getClassLoader().getResource(publicCert)).getFile();
        String publicCertPath = new File(publicCertFile).getAbsolutePath();

        SSLContext sslDisabledSslContext = initSslContext();
        sendRequest(sslDisabledSslContext);

        SSLContext truststoreSslContext = initSslContext(truststorePath, truststorePassword.toCharArray());
        sendRequest(truststoreSslContext);

        SSLContext trustedCertSslContext = initSslContext(trustedCertPath);
        sendRequest(trustedCertSslContext);

        SSLContext mTlsSslContext1 = initSslContext(truststorePath, truststorePassword.toCharArray(), keystorePath,
                                                    keystorePassword.toCharArray());
        sendRequest(mTlsSslContext1);

        SSLContext mTlsSslContext2 = initSslContext(trustedCertPath, privateKeyPath, publicCertPath);
        sendRequest(mTlsSslContext2);
    }

    private static SSLContext initSslContext() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new SecureRandom());
        return sslContext;
    }

    private static SSLContext initSslContext(String truststorePath, char[] truststorePassword) throws Exception {
        try (FileInputStream is = new FileInputStream(truststorePath)) {
            KeyStore truststore = KeyStore.getInstance("PKCS12");
            truststore.load(is, truststorePassword);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(truststore);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());
            return sslContext;
        }
    }

    private static SSLContext initSslContext(String trustedCertPath) throws Exception {
        X509Certificate trustedCert = PublicKey.decodeX509PublicCertificate(trustedCertPath);
        KeyStore truststore = KeyStore.getInstance("PKCS12");
        truststore.load(null, truststorePassword.toCharArray());
        truststore.setCertificateEntry(UUID.randomUUID().toString(), trustedCert);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(truststore);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());
        return sslContext;
    }

    private static SSLContext initSslContext(String truststorePath, char[] truststorePassword, String keystorePath,
                                             char[] keystorePassword) throws Exception {
        try (FileInputStream truststoreIs = new FileInputStream(truststorePath);
             FileInputStream keystoreIs = new FileInputStream(keystorePath)) {
            KeyStore truststore = KeyStore.getInstance("PKCS12");
            truststore.load(truststoreIs, truststorePassword);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(truststore);

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(keystoreIs, keystorePassword);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, keystorePassword);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
            return sslContext;
        }
    }

    private static SSLContext initSslContext(String trustedCertPath, String privateKeyPath, String publicCertPath)
            throws Exception {
        X509Certificate trustedCert = PublicKey.decodeX509PublicCertificate(trustedCertPath);
        KeyStore truststore = KeyStore.getInstance("PKCS12");
        truststore.load(null, truststorePassword.toCharArray());
        truststore.setCertificateEntry(UUID.randomUUID().toString(), trustedCert);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(truststore);

        Security.addProvider(new BouncyCastleProvider());
        java.security.PrivateKey privateKey = PrivateKey.decodePkcs8PrivateKey(privateKeyPath);
        X509Certificate publicCert = PublicKey.decodeX509PublicCertificate(publicCertPath);
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, keystorePassword.toCharArray());
        keyStore.setKeyEntry(UUID.randomUUID().toString(), privateKey, keystorePassword.toCharArray(),
                             new X509Certificate[]{publicCert});
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keystorePassword.toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
        return sslContext;
    }

    private static void sendRequest(SSLContext sslContext) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .sslContext(sslContext)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://localhost:9090/oauth2/jwks"))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Status Code: " + response.statusCode());
        System.out.println("Response Body: " + response.body());
    }
}
