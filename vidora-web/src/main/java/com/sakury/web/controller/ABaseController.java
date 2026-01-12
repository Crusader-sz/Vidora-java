package com.sakury.web.controller;

import com.sakury.component.RedisComponent;
import com.sakury.entity.constants.Constants;
import com.sakury.entity.dto.UserInfoTokenDto;
import com.sakury.entity.enums.ResponseCodeEnum;
import com.sakury.entity.vo.ResponseVO;
import com.sakury.exception.BusinessException;
import com.sakury.utils.StringTools;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class ABaseController {

    protected static final String STATUC_SUCCESS = "success";

    protected static final String STATUC_ERROR = "error";

    @Resource
    private RedisComponent redisComponent;

    /**
     * 获取成功ResponseVO
     *
     * @param t
     * @param <T>
     * @return
     */
    protected <T> ResponseVO getSuccessResponseVO(T t) {
        ResponseVO<T> responseVO = new ResponseVO<>();
        responseVO.setStatus(STATUC_SUCCESS);
        responseVO.setCode(ResponseCodeEnum.CODE_200.getCode());
        responseVO.setInfo(ResponseCodeEnum.CODE_200.getMsg());
        responseVO.setData(t);
        return responseVO;
    }

    /**
     * 获取业务错误ResponseVO
     *
     * @param e
     * @param t
     * @param <T>
     * @return
     */
    protected <T> ResponseVO getBusinessErrorResponseVO(BusinessException e, T t) {
        ResponseVO vo = new ResponseVO();
        vo.setStatus(STATUC_ERROR);
        if (e.getCode() == null) {
            vo.setCode(ResponseCodeEnum.CODE_600.getCode());
        } else {
            vo.setCode(e.getCode());
        }
        vo.setInfo(e.getMessage());
        vo.setData(t);
        return vo;
    }

    /**
     * 获取服务错误ResponseVO
     *
     * @param t
     * @param <T>
     * @return
     */
    protected <T> ResponseVO getServerErrorResponseVO(T t) {
        ResponseVO vo = new ResponseVO();
        vo.setStatus(STATUC_ERROR);
        vo.setCode(ResponseCodeEnum.CODE_500.getCode());
        vo.setInfo(ResponseCodeEnum.CODE_500.getMsg());
        vo.setData(t);
        return vo;
    }

    /**
     * 获取客户端真实IP地址
     * 该方法通过多种HTTP头信息来获取客户端IP，主要用于处理经过代理服务器或负载均衡器的情况
     *
     * @return 客户端的真实IP地址字符串，如果无法获取则返回本地地址
     */
    protected String getIpAddress() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String ip = request.getHeader("x-forwarded-for");
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
            // 多次反向代理后会有多个ip值，第一个ip才是真实ip
            if (ip.indexOf(",") != -1) {
                ip = ip.split(",")[0];
            }
        }

        // 按优先级依次检查各种代理头信息
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 将token保存到cookie中
     *
     * @param response HTTP响应对象，用于添加cookie
     * @param token    要保存的token字符串
     */
    protected void saveToken2Cookie(HttpServletResponse response, String token) {
        // 创建cookie对象并设置token值
        Cookie cookie = new Cookie(Constants.TOKEN_WEB, token);
        // 设置cookie有效期为7天
        cookie.setMaxAge(Constants.TIME_SECOND_DAY * 7);
        // 设置cookie路径为根路径，使整个应用都能访问该cookie
        cookie.setPath("/");
        // 将cookie添加到HTTP响应中
        response.addCookie(cookie);
    }

    /**
     * 获取用户信息令牌DTO
     * 从当前HTTP请求中获取令牌，并通过Redis组件查询对应的用户信息
     *
     * @return UserInfoTokenDto 用户信息令牌数据传输对象，包含用户相关信息
     */
    protected UserInfoTokenDto getUserInfoTokenDto() {
        // 获取当前请求上下文中的HttpServletRequest对象
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        // 从请求头中获取令牌信息
        String token = request.getHeader(Constants.TOKEN_WEB);
        // 通过Redis组件根据令牌获取用户信息
        return redisComponent.getTokenInfo(token);
    }


    /**
     * 删除用户登录相关的Cookie，并清理Redis中的Token信息
     * 该方法会查找名为Constants.TOKEN_WEB的Cookie，将其设置为过期状态并从请求中移除，
     * 同时删除Redis中对应的Token信息
     *
     * @param response HTTP响应对象，用于添加过期的Cookie以删除客户端的Token
     */
    protected void deleteCookie(HttpServletResponse response) {
        // 获取当前请求的HttpServletRequest对象
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        // 获取请求中的所有Cookie
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return;
        }

        // 遍历所有Cookie，查找Token相关的Cookie
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(Constants.TOKEN_WEB)) {
                // 从Redis中删除对应的Token信息
                redisComponent.deleteTokenInfo(cookie.getValue());

                // 设置Cookie过期时间为0，路径为根目录，然后添加到响应中
                cookie.setMaxAge(0);
                cookie.setPath("/");
                response.addCookie(cookie);
                break;
            }
        }
    }

    /**
     * 清理旧的用户token信息
     * 从请求中获取现有的token并从Redis中删除对应的信息
     */
    protected void clearOldToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return; // 没有cookie则直接返回
        }

        String token = null;
        for (Cookie cookie : cookies) {
            if (cookie != null && Constants.TOKEN_WEB.equals(cookie.getName())) {
                token = cookie.getValue();
                break; // 找到token后立即退出循环
            }
        }

        if (!StringTools.isEmpty(token)) {
            redisComponent.deleteTokenInfo(token);
        }
    }

}
