package no.cantara.security.whydah;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import no.cantara.config.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.TimeUnit;

/**
 * Location for JWT public key: https://whydahdev.cantara.no/oauth2/.well-known/jwks.json
 **/
public class JwtUtils {
    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);

    public boolean isExpiredOrInvalid(String jwt) {
        try {
            Long time = getClaimFromJwtToken(jwt, "exp", Long.class);
            return time * 1000L <= System.currentTimeMillis();
        } catch (Exception ex) {
            return true;
        }
    }

    public static String getUserNameFromJwtToken(String token) throws JwkException {
        return getClaimFromJwtToken(token, "sub", String.class);
    }

    public static String getUserTokenFromJwtToken(String token) throws JwkException {
        return getClaimFromJwtToken(token, "usertoken_id", String.class);
    }

    public static String getCustomerRefFromJwtToken(String token) throws JwkException {
        return getClaimFromJwtToken(token, "customer_ref", String.class);
    }


    public static <T> T getClaimFromJwtToken(String authToken, String claimName, Class<T> requiredType) throws JwkException {
        final ApplicationProperties properties = ApplicationProperties.getInstance();
        String oauth2Issuer = properties.get(WhydahSecurityProperties.WHYDAH_OAUTH2_URI);
        DecodedJWT jwt = JWT.decode(authToken);
        JwkProvider provider = new JwkProviderBuilder(oauth2Issuer)
                .cached(10, 24, TimeUnit.HOURS)
                .rateLimited(10, 1, TimeUnit.MINUTES).build();
        Jwk jwk = provider.get(jwt.getKeyId());

        Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(oauth2Issuer)
                .build();
        DecodedJWT djwt = verifier.verify(authToken);
        return djwt.getClaim(claimName).as(requiredType);
    }
}
