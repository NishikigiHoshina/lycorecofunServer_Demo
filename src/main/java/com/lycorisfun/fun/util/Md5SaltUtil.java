package com.lycorisfun.fun.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * MD5 加盐工具类
 */
public final class Md5SaltUtil {

    /**
     * 生成带盐密文，格式：salt$md5(salt+password)
     */
    public static String encrypt(String rawPassword) {
//        String salt = UUID.randomUUID().toString().replace("-", "");
        String salt = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String hash = new String(
                org.apache.commons.codec.binary.Hex.encodeHex(
                        DigestUtils.md5(salt + rawPassword)
                )
        );
        return salt + "$" + hash;   // 共 49 位（16+1+32）
    }

    /**
     * 验证明文密码是否与数据库存储的 salt$hash 匹配
     * @param rawPassword  用户输入的明文
     * @param dbPassword   数据库中的 salt$hash
     */
    public static boolean verify(String rawPassword, String dbPassword) {
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(dbPassword)) {
            return false;
        }
        String[] arr = dbPassword.split("\\$");
        if (arr.length != 2) {
            return false;
        }
        String salt = arr[0];
        String hash = arr[1];
        String calc = new String(
                org.apache.commons.codec.binary.Hex.encodeHex(
                        DigestUtils.md5(salt + rawPassword)
                )
        );
        return calc.equals(hash);
    }

    /* ------ 简单自测 ------ */
//    public static void main(String[] args) {
//        String pwd = "123456";
//        String stored = encrypt(pwd);
//        System.out.println("加密后 = " + stored);
//        System.out.println("验证结果 = " + verify(pwd, stored));
//    }
}