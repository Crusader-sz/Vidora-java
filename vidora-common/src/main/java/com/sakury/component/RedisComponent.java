package com.sakury.component;

import com.sakury.entity.constants.Constants;
import com.sakury.redis.RedisUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.UUID;

@Component
public class RedisComponent {

    @Resource
    private RedisUtils redisUtils;

    /**
     * 保存验证码到Redis中
     *
     * @param code 需要保存的验证码内容
     * @return 返回生成的唯一校验码，用于后续验证时的key
     */
    public String saveCheckCode(String code) {
        // 生成唯一的校验码作为Redis的键
        String checkCode = UUID.randomUUID().toString();

        // 将验证码存储到Redis中，设置过期时间为10分钟
        redisUtils.setex(Constants.REDIS_KEY_CHECK_CODE + checkCode, code,
                Constants.REDIS_KEY_EXPIRES_ONE_MIN * 10);

        return checkCode;
    }

    /**
     * 获取验证码
     * 从Redis缓存中根据验证码键获取对应的验证码值
     *
     * @param checkCodeKey 验证码的唯一标识键
     * @return 返回与checkCodeKey对应的验证码字符串，如果不存在则返回null
     */
    public String getCheckCode(String checkCodeKey) {
        return (String) redisUtils.get(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey);
    }

    /**
     * 删除验证码缓存
     *
     * @param checkCodeKey 验证码键值，用于标识要删除的验证码缓存项
     */
    public void deleteCheckCode(String checkCodeKey) {
        redisUtils.delete(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey);
    }
}
