package com.sakury.web.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sakury.component.RedisComponent;
import com.sakury.entity.constants.Constants;
import com.sakury.entity.dto.UserInfoTokenDto;
import com.sakury.entity.query.UserInfoQuery;
import com.sakury.entity.po.UserInfo;
import com.sakury.entity.vo.ResponseVO;
import com.sakury.exception.BusinessException;
import com.sakury.service.UserInfoService;
import com.sakury.utils.StringTools;
import com.wf.captcha.ArithmeticCaptcha;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 用户信息表 Controller
 */
@RestController
@RequestMapping("/account")
@Validated
public class AccountController extends ABaseController {

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private RedisComponent redisComponent;


    /**
     * 生成验证码并保存到Redis中，返回包含验证码图片和唯一标识的响应对象
     *
     * @return ResponseVO 包含验证码base64编码和唯一标识的响应对象
     */
    @RequestMapping("/checkCode")
    public ResponseVO checkCode() {
        // 创建算术验证码对象，设置宽度为100，高度为42
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(100, 42);
        // 获取验证码文本内容
        String code = captcha.text();
        // 将验证码保存到Redis中，并获取唯一标识key
        String checkCodeKey = redisComponent.saveCheckCode(code);
        // 将验证码转换为base64格式
        String checkCodeBase64 = captcha.toBase64();

        // 构建返回结果map
        Map<String, String> result = new HashMap<>();
        result.put("checkCode", checkCodeBase64);
        result.put("checkCodeKey", checkCodeKey);
        return getSuccessResponseVO(result);

    }

    /**
     * 用户注册接口
     *
     * @param email            用户邮箱，不能为空，必须是有效的邮箱格式，最大长度150字符
     * @param nickName         用户昵称，不能为空，最大长度20字符
     * @param registerPassword 注册密码，不能为空，必须符合密码正则表达式规则
     * @param checkCode        图片验证码，不能为空
     * @param checkCodeKey     图片验证码key，不能为空
     * @return ResponseVO 响应对象，注册成功返回成功响应，失败返回错误信息
     */
    @RequestMapping("/register")
    public ResponseVO register(@NotEmpty @Email @Size(max = 150) String email,
                               @NotEmpty @Size(max = 20) String nickName,
                               @NotEmpty @Pattern(regexp = Constants.REGEX_PASSWORD) String registerPassword,
                               @NotEmpty String checkCode,
                               @NotEmpty String checkCodeKey) {
        // 验证图片验证码是否正确
        try {
            if (!checkCode.equalsIgnoreCase(redisComponent.getCheckCode(checkCodeKey))) {
                throw new BusinessException("图片验证码错误");
            }
            // 执行用户注册逻辑
            userInfoService.register(email, nickName, registerPassword);
            return getSuccessResponseVO(null);
        } finally {
            // 无论注册成功与否，都需要删除Redis中的验证码记录
            redisComponent.deleteCheckCode(checkCodeKey);
        }
    }

    /**
     * 用户登录接口
     * 处理用户登录请求，验证验证码、邮箱密码，并生成登录令牌
     *
     * @param request      HTTP请求对象，用于获取客户端信息和cookie
     * @param response     HTTP响应对象，用于设置登录token到cookie
     * @param email        用户邮箱地址，不能为空且格式必须正确
     * @param password     用户登录密码，不能为空
     * @param checkCode    用户输入的图片验证码，不能为空
     * @param checkCodeKey 图片验证码的唯一标识key，不能为空
     * @return ResponseVO 包含登录成功信息的响应对象
     */
    @RequestMapping("/login")
    public ResponseVO login(HttpServletRequest request,
                            HttpServletResponse response,
                            @NotEmpty @Email String email,
                            @NotEmpty String password,
                            @NotEmpty String checkCode,
                            @NotEmpty String checkCodeKey) {
        // 验证图片验证码是否正确
        try {
            if (!checkCode.equalsIgnoreCase(redisComponent.getCheckCode(checkCodeKey))) {
                throw new BusinessException("图片验证码错误");
            }
            String ip = getIpAddress();
            // 先清理旧的token（如果存在）
            clearOldToken(request);
            UserInfoTokenDto userInfoTokenDto = userInfoService.login(email, password, ip);
            saveToken2Cookie(response, userInfoTokenDto.getToken());
            //TODO 设置粉丝数，关注数，硬币数
            return getSuccessResponseVO(userInfoTokenDto);
        } finally {
            // 清理验证码缓存并删除旧的token信息
            redisComponent.deleteCheckCode(checkCodeKey);
        }
    }

    /**
     * 自动登录接口
     * 根据用户信息token进行自动登录处理，如果token即将过期则重新保存到redis并更新cookie
     *
     * @param response HttpServletResponse对象，用于向客户端设置cookie
     * @return ResponseVO 返回用户信息token数据的响应对象
     */
    @RequestMapping("/autoLogin")
    public ResponseVO autoLogin(HttpServletResponse response) {
        // 获取用户信息token对象
        UserInfoTokenDto userInfoTokenDto = getUserInfoTokenDto();
        if (userInfoTokenDto == null) {
            return getSuccessResponseVO(null);
        }
        // 检查token是否即将过期（剩余时间小于一天），如果是则重新保存到redis并更新cookie
        if (userInfoTokenDto.getExpireTime() - System.currentTimeMillis() < Constants.REDIS_KEY_EXPIRES_ONE_DAY) {
            redisComponent.saveTokenInfo(userInfoTokenDto);
            saveToken2Cookie(response, userInfoTokenDto.getToken());
        }
        // 更新cookie中的token信息
        saveToken2Cookie(response, userInfoTokenDto.getToken());
        return getSuccessResponseVO(userInfoTokenDto);
    }

    /**
     * 用户登出接口
     * 处理用户的登出请求，删除相关cookie并返回成功响应
     *
     * @param response HTTP响应对象，用于删除cookie
     * @return ResponseVO 响应结果对象，包含登出操作的结果信息
     */
    @RequestMapping("/logout")
    public ResponseVO logout(HttpServletResponse response) {
        deleteCookie(response);
        return getSuccessResponseVO(null);
    }

    /**
     * 根据条件分页查询
     */
    @RequestMapping("/loadDataList")
    public ResponseVO loadDataList(UserInfoQuery query) {
        return getSuccessResponseVO(userInfoService.findListByPage(query));
    }

    /**
     * 新增
     */
    @RequestMapping("/add")
    public ResponseVO add(UserInfo bean) {
        userInfoService.add(bean);
        return getSuccessResponseVO(null);
    }

    /**
     * 批量新增
     */
    @RequestMapping("/addBatch")
    public ResponseVO addBatch(@RequestBody List<UserInfo> listBean) {
        userInfoService.addBatch(listBean);
        return getSuccessResponseVO(null);
    }

    /**
     * 批量新增/修改
     */
    @RequestMapping("/addOrUpdateBatch")
    public ResponseVO addOrUpdateBatch(@RequestBody List<UserInfo> listBean) {
        userInfoService.addBatch(listBean);
        return getSuccessResponseVO(null);
    }

    /**
     * 根据UserId查询对象
     */
    @RequestMapping("/getUserInfoByUserId")
    public ResponseVO getUserInfoByUserId(String userId) {
        return getSuccessResponseVO(userInfoService.getUserInfoByUserId(userId));
    }

    /**
     * 根据UserId修改对象
     */
    @RequestMapping("/updateUserInfoByUserId")
    public ResponseVO updateUserInfoByUserId(UserInfo bean, String userId) {
        userInfoService.updateUserInfoByUserId(bean, userId);
        return getSuccessResponseVO(null);
    }

    /**
     * 根据UserId删除
     */
    @RequestMapping("/deleteUserInfoByUserId")
    public ResponseVO deleteUserInfoByUserId(String userId) {
        userInfoService.deleteUserInfoByUserId(userId);
        return getSuccessResponseVO(null);
    }

    /**
     * 根据Email查询对象
     */
    @RequestMapping("/getUserInfoByEmail")
    public ResponseVO getUserInfoByEmail(String email) {
        return getSuccessResponseVO(userInfoService.getUserInfoByEmail(email));
    }

    /**
     * 根据Email修改对象
     */
    @RequestMapping("/updateUserInfoByEmail")
    public ResponseVO updateUserInfoByEmail(UserInfo bean, String email) {
        userInfoService.updateUserInfoByEmail(bean, email);
        return getSuccessResponseVO(null);
    }

    /**
     * 根据Email删除
     */
    @RequestMapping("/deleteUserInfoByEmail")
    public ResponseVO deleteUserInfoByEmail(String email) {
        userInfoService.deleteUserInfoByEmail(email);
        return getSuccessResponseVO(null);
    }

    /**
     * 根据NickName查询对象
     */
    @RequestMapping("/getUserInfoByNickName")
    public ResponseVO getUserInfoByNickName(String nickName) {
        return getSuccessResponseVO(userInfoService.getUserInfoByNickName(nickName));
    }

    /**
     * 根据NickName修改对象
     */
    @RequestMapping("/updateUserInfoByNickName")
    public ResponseVO updateUserInfoByNickName(UserInfo bean, String nickName) {
        userInfoService.updateUserInfoByNickName(bean, nickName);
        return getSuccessResponseVO(null);
    }

    /**
     * 根据NickName删除
     */
    @RequestMapping("/deleteUserInfoByNickName")
    public ResponseVO deleteUserInfoByNickName(String nickName) {
        userInfoService.deleteUserInfoByNickName(nickName);
        return getSuccessResponseVO(null);
    }
}