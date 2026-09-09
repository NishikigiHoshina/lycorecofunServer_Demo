package com.lycorisfun.fun.util;

import com.lycorisfun.fun.Exception.BusinessException;

/**
 * 用户输入文本入库前的字符校验。
 * 背景：当前库表均为 utf8mb3（旧 utf8，单字符最长 3 字节），只支持 BMP（码点 ≤ U+FFFF）；
 * emoji（≥ U+1F300）与扩展 CJK 等 4 字节字符会触发 MySQL "Incorrect string value" 报错。
 * 故在入库前拦截，返回友好错误而非 500。（若日后库升级 utf8mb4 可放宽/移除。）
 */
public final class TextValidator {

    private TextValidator() {
    }

    /** 校验字符串中不含数据库无法存储的字符；field 仅用于错误提示。空/空串直接放行。 */
    public static void requireStorable(String text, String field) {
        if (text == null || text.isEmpty()) {
            return;
        }
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            if (cp > 0xFFFF) {
                throw new BusinessException(400,
                        field + "：包含数据库无法存储的特殊字符（如 emoji），请移除后重试");
            }
            i += Character.charCount(cp);
        }
    }
}
