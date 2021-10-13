package no.cantara.security.whydah;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.TimeUnit;

/**
 * Location for JWT public key: https://whydahdev.cantara.no/oauth2/.well-known/jwks.json
 **/
public class JwtHelper {
    private static final Logger log = LoggerFactory.getLogger(JwtHelper.class);

    final String oauth2Uri;

    public JwtHelper(String oauth2Uri) {
        this.oauth2Uri = oauth2Uri;
    }

    public boolean isExpiredOrInvalid(String jwt) {
        try {
            Long time = getClaimFromJwtToken(jwt, "exp", Long.class);
            return time * 1000L <= System.currentTimeMillis();
        } catch (Exception ex) {
            return true;
        }
    }

    public String getUserNameFromJwtToken(String token) throws JwkException {
        return getClaimFromJwtToken(token, "sub", String.class);
    }

    public String getUserTokenFromJwtToken(String token) throws JwkException {
        return getClaimFromJwtToken(token, "usertoken_id", String.class);
    }

    public String getCustomerRefFromJwtToken(String token) throws JwkException {
        return getClaimFromJwtToken(token, "customer_ref", String.class);
    }


    public <T> T getClaimFromJwtToken(String authToken, String claimName, Class<T> requiredType) throws JwkException {
        String oauth2Issuer = oauth2Uri;
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
