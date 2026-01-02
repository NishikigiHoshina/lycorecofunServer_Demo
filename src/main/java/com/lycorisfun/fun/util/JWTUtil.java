package com.lycorisfun.fun.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Calendar;
import java.util.Map;
import java.util.Set;

public class JWTUtil {
    public static final String secretKey = "chisato2808!saikodesu@$%";//密钥

    /** 生成令牌 create an auth token **/
    public static String createToken(Map<String, String> payload) {
        Calendar calendar = Calendar.getInstance();//获取当前时间
        calendar.add(Calendar.SECOND, 60*60*24*7);//将时间延长7天，以获取到失效时间点
        JWTCreator.Builder builder = JWT.create()
                .withExpiresAt(calendar.getTime());//设置令牌过期时间

        Set<String> keys = payload.keySet();
        for(String key : keys) {
            String value = payload.get(key);
            builder.withClaim(key, value);
        }

        String token = builder.sign(Algorithm.HMAC256(secretKey));
        return token;
    }

    //校验令牌
    public static DecodedJWT verifyToken(String token) {
        JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(secretKey)).build();
        DecodedJWT decodedJWT = jwtVerifier.verify(token);
        return decodedJWT;
    }
}