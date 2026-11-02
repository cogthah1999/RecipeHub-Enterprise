package recipehub.security;

import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {
    @Test void issuedJwtHasValidSignatureSubjectAndExpiration() {
        byte[] secret = "0123456789abcdef0123456789abcdef".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var key = new SecretKeySpec(secret, "HmacSHA256");
        var service = new TokenService(NimbusJwtEncoder.withSecretKey(key).build(), 30);
        var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        var jwt = decoder.decode(service.issue("thanh"));
        assertEquals("thanh", jwt.getSubject());
        assertTrue(jwt.getExpiresAt().isAfter(Instant.now()));
        assertEquals("recipehub", jwt.getClaimAsString("iss"));
        assertEquals(0, ((Number) jwt.getClaim("tokenVersion")).longValue());
    }
}
