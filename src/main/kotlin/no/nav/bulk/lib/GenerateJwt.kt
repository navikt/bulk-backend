package no.nav.bulk.lib

import com.auth0.jwk.JwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.RSAKeyProvider
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*


fun generateJwt(jwtDecodedTokenPreviousAud: DecodedJWT): String {
//    jwtDecodedTokenPreviousAud.audience[0] = AuthConfig.CLIENT_ID
//    val keyProvider = RSAKeyProvider(AuthConfig.JWT_PUBLIC_KEY)
//val publicKey: JwkProvider = jwkProvider.get("6f8856ed-9189-488f-9011-0ff4b6c08edc").publicKeyval keySpecPKCS8 = PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyString))val privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpecPKval jwtBuilder = JWT.create()val jwtEncoded = jwtBuilder.withKeyId(jwtDecodedTokenPreviousAud.keyId).withJWTId(jwtDecodedTokenPreviousAud.getHeaderClaim("typ").asString()).withIssuer(jwtDecodedTokenPreviousAud.issuer).withSubject(jwtDecodedTokenPreviousAud.subject).withAudience(AuthConfig.CLIENT_ID).withExpiresAt(jwtDecodedTokenPreviousAud.expiresAt)
//   .withNotBefore(jwtDecodedTokenPreviousAud.notBefore)
//   .withIssuedAt(jwtDecodedTokenPreviousAud.issuedAt)
////        .withClaim("aio", jwtDecodedTokenPreviousAud.getClaim("aio").toString())
//   .withClaim("groups", jwtDecodedTokenPreviousAud.getClaim("groups").asList(String::class.java))
//   .withClaim("azp", jwtDecodedTokenPreviousAud.getClaim("azp").toString())
////        .withClaim("azpacr", jwtDecodedTokenPreviousAud.getClaim("azpacr").toString())
//   .withClaim("name", jwtDecodedTokenPreviousAud.getClaim("name").toString())
//   .withClaim("oid", jwtDecodedTokenPreviousAud.getClaim("oid").toString())
//   .withClaim("rh", jwtDecodedTokenPreviousAud.getClaim("rh").toString())
////                    .withClaim("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
////                    .withClaim("token_type", "Bearer")
////                    .withClaim("token_id", jwtDecodedTokenPreviousAud.id)
//        .withClaim("jti", jwtDecodedTokenPreviousAud.id)
//        .withClaim("tid", jwtDecodedTokenPreviousAud.getClaim("tid").toString())
//        .withClaim("uti", jwtDecodedTokenPreviousAud.getClaim("uti").toString())
//        .sign(Algorithm.RSA256(AuthConfig.PRIVATE_KEY, AuthConfig.PUBLIC_KEY))

    return ""
}