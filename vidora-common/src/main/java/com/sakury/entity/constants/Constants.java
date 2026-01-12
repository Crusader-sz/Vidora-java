package com.sakury.entity.constants;

public class Constants {
    /**
     * 数字常量：1
     */
    public static final Integer ONE = 1;

    /**
     * 数字常量：0
     */
    public static final Integer ZERO = 0;

    /**
     * 用户ID长度限制常量：10位
     */
    public static final Integer LENGTH_USERID = 10;

    /**
     * 密码正则表达式常量
     * 规则：至少包含一个数字和一个字母，长度在8-18位之间，允许数字、字母及特殊字符(~!@#$%^&*_)
     */
    public static final String REGEX_PASSWORD = "^(?=.*\\d)(?=.*[a-zA-Z])[\\da-zA-Z~!@#$%^&*_]{8,18}$";

    /**
     * Redis键过期时间常量：1分钟（单位：毫秒）
     */
    public static final Integer REDIS_KEY_EXPIRES_ONE_MIN = 60000;

    /**
     * Redis键过期时间常量：1天（单位：毫秒）
     */
    public static final Integer REDIS_KEY_EXPIRES_ONE_DAY = 86400000;

    /**
     * 时间常量：1天（单位：秒）
     */
    public static final Integer TIME_SECOND_DAY = 86400;
    /**
     * Redis键前缀常量
     */
    public static final String REDIS_KEY_PREFIX = "vidora:";

    /**
     * Redis验证码键前缀
     */
    public static String REDIS_KEY_CHECK_CODE = REDIS_KEY_PREFIX + "checkCode:";

    /**
     * Redis信息键前缀
     */
    public static String REDIS_KEY_TOKEN_WEB = REDIS_KEY_PREFIX + "token:web:";

    /**
     * Token信息键
     */
    public static String TOKEN_WEB = "token";
}
