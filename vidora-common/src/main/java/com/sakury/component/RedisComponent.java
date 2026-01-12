package com.sakury.component;

import com.sakury.entity.constants.Constants;
import com.sakury.entity.dto.UserInfoTokenDto;
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

    /**
     * 保存用户令牌信息到Redis中
     * 生成随机token，设置过期时间为7天，并将用户令牌信息存储到Redis
     *
     * @param userInfoTokenDto 用户信息令牌数据传输对象，包含用户相关信息
     */
    public void saveTokenInfo(UserInfoTokenDto userInfoTokenDto) {
        // 生成UUID作为token
        String token = UUID.randomUUID().toString();

        // 设置过期时间：当前时间戳 + 7天的毫秒数
        userInfoTokenDto.setExpireTime(System.currentTimeMillis() + Constants.REDIS_KEY_EXPIRES_ONE_DAY * 7);

        // 设置token值
        userInfoTokenDto.setToken(token);

        // 将用户令牌信息存储到Redis，key为常量前缀+token，过期时间为7天
        redisUtils.setex(Constants.REDIS_KEY_TOKEN_WEB + token, userInfoTokenDto, Constants.REDIS_KEY_EXPIRES_ONE_DAY * 7);
    }

    /**
     * 根据token获取用户信息
     * 从Redis缓存中查询指定token对应的用户信息对象
     *
     * @param token 用户认证令牌
     * @return UserInfoTokenDto 用户信息传输对象，包含用户的基本信息和权限等数据
     */
    public UserInfoTokenDto getTokenInfo(String token) {
        return (UserInfoTokenDto) redisUtils.get(Constants.REDIS_KEY_TOKEN_WEB + token);
    }


    /**
     * 删除指定token的信息
     * 从Redis中删除以常量REDIS_KEY_TOKEN_WEB为前缀加上传入token组成的键对应的缓存数据
     *
     * @param token 需要删除的token字符串，用于构建Redis中的键名
     */
    public void deleteTokenInfo(String token) {
        redisUtils.delete(Constants.REDIS_KEY_TOKEN_WEB + token);
    }
}
