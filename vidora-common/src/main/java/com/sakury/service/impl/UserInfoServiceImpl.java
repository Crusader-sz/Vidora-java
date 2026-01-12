package com.sakury.service.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import com.sakury.component.RedisComponent;
import com.sakury.entity.constants.Constants;
import com.sakury.entity.dto.UserInfoTokenDto;
import com.sakury.entity.enums.UserSexEnum;
import com.sakury.entity.enums.UserStatusEnum;
import com.sakury.exception.BusinessException;
import com.sakury.utils.CopyTools;
import org.springframework.stereotype.Service;

import com.sakury.entity.enums.PageSize;
import com.sakury.entity.query.UserInfoQuery;
import com.sakury.entity.po.UserInfo;
import com.sakury.entity.vo.PaginationResultVO;
import com.sakury.entity.query.SimplePage;
import com.sakury.mappers.UserInfoMapper;
import com.sakury.service.UserInfoService;
import com.sakury.utils.StringTools;


/**
 * 用户信息表 业务接口实现
 */
@Service("userInfoService")
public class UserInfoServiceImpl implements UserInfoService {

    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

    @Resource
    private RedisComponent redisComponent;

    /**
     * 用户注册功能
     * 验证邮箱和昵称的唯一性，创建新用户并保存到数据库
     *
     * @param email            用户注册邮箱
     * @param nickName         用户昵称
     * @param registerPassword 用户注册密码
     */
    @Override
    public void register(String email, String nickName, String registerPassword) {
        // 检查邮箱是否已存在
        UserInfo userInfo = this.userInfoMapper.selectByEmail(email);
        if (null != userInfo) {
            throw new BusinessException("邮箱账号已存在");
        }
        // 检查昵称是否已存在
        userInfo = this.userInfoMapper.selectByNickName(nickName);
        if (null != userInfo) {
            throw new BusinessException("昵称已存在");
        }
        // 创建新用户信息对象
        userInfo = new UserInfo();
        String userId = StringTools.getRandomNumber(Constants.LENGTH_USERID);
        userInfo.setUserId(userId);
        userInfo.setEmail(email);
        userInfo.setNickName(nickName);
        userInfo.setPassword(StringTools.encodeByMD5(registerPassword));
        userInfo.setStatus(UserStatusEnum.ENABLE.getStatus());
        userInfo.setSex(UserSexEnum.UNKNOWN.getType());
        userInfo.setTheme(Constants.ONE);
        userInfo.setRegisterTime(new Date());
        //TODO 初始化用户的硬币数
        userInfo.setTotalCoinCount(Constants.ZERO);
        userInfo.setCurrentCoinCount(Constants.ZERO);
        // 将新用户信息插入数据库
        this.userInfoMapper.insert(userInfo);
    }

    /**
     * 用户登录方法
     * 验证用户邮箱和密码，更新登录信息，并生成用户令牌
     *
     * @param email    用户邮箱地址
     * @param password 用户密码
     * @param ip       用户登录IP地址
     * @return 包含用户信息的令牌DTO对象
     * @throws BusinessException 当账号密码错误或用户被禁用时抛出异常
     */
    @Override
    public UserInfoTokenDto login(String email, String password, String ip) {
        // 查询用户信息并验证账号密码
        UserInfo userInfo = this.userInfoMapper.selectByEmail(email);
        if (null == userInfo || !userInfo.getPassword().equals(password)) {
            throw new BusinessException("账号或密码错误");
        }
        // 检查用户状态是否被禁用
        if (UserStatusEnum.DISABLE.getStatus().equals(userInfo.getStatus())) {
            throw new BusinessException("用户已被禁用");
        }
        // 更新用户的最后登录时间和IP地址
        UserInfo updateInfo = new UserInfo();
        updateInfo.setLastLoginTime(new Date());
        updateInfo.setLastLoginIp(ip);
        this.userInfoMapper.updateByUserId(updateInfo, userInfo.getUserId());

        // 复制用户信息到令牌DTO并保存到Redis
        UserInfoTokenDto userInfoTokenDto = CopyTools.copy(userInfo, UserInfoTokenDto.class);
        redisComponent.saveTokenInfo(userInfoTokenDto);
        return userInfoTokenDto;
    }


    /**
     * 根据条件查询列表
     */
    @Override
    public List<UserInfo> findListByParam(UserInfoQuery param) {
        return this.userInfoMapper.selectList(param);
    }

    /**
     * 根据条件查询列表
     */
    @Override
    public Integer findCountByParam(UserInfoQuery param) {
        return this.userInfoMapper.selectCount(param);
    }

    /**
     * 分页查询方法
     */
    @Override
    public PaginationResultVO<UserInfo> findListByPage(UserInfoQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<UserInfo> list = this.findListByParam(param);
        PaginationResultVO<UserInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    @Override
    public Integer add(UserInfo bean) {
        return this.userInfoMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<UserInfo> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.userInfoMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或者修改
     */
    @Override
    public Integer addOrUpdateBatch(List<UserInfo> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.userInfoMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 多条件更新
     */
    @Override
    public Integer updateByParam(UserInfo bean, UserInfoQuery param) {
        StringTools.checkParam(param);
        return this.userInfoMapper.updateByParam(bean, param);
    }

    /**
     * 多条件删除
     */
    @Override
    public Integer deleteByParam(UserInfoQuery param) {
        StringTools.checkParam(param);
        return this.userInfoMapper.deleteByParam(param);
    }

    /**
     * 根据UserId获取对象
     */
    @Override
    public UserInfo getUserInfoByUserId(String userId) {
        return this.userInfoMapper.selectByUserId(userId);
    }

    /**
     * 根据UserId修改
     */
    @Override
    public Integer updateUserInfoByUserId(UserInfo bean, String userId) {
        return this.userInfoMapper.updateByUserId(bean, userId);
    }

    /**
     * 根据UserId删除
     */
    @Override
    public Integer deleteUserInfoByUserId(String userId) {
        return this.userInfoMapper.deleteByUserId(userId);
    }

    /**
     * 根据Email获取对象
     */
    @Override
    public UserInfo getUserInfoByEmail(String email) {
        return this.userInfoMapper.selectByEmail(email);
    }

    /**
     * 根据Email修改
     */
    @Override
    public Integer updateUserInfoByEmail(UserInfo bean, String email) {
        return this.userInfoMapper.updateByEmail(bean, email);
    }

    /**
     * 根据Email删除
     */
    @Override
    public Integer deleteUserInfoByEmail(String email) {
        return this.userInfoMapper.deleteByEmail(email);
    }

    /**
     * 根据NickName获取对象
     */
    @Override
    public UserInfo getUserInfoByNickName(String nickName) {
        return this.userInfoMapper.selectByNickName(nickName);
    }

    /**
     * 根据NickName修改
     */
    @Override
    public Integer updateUserInfoByNickName(UserInfo bean, String nickName) {
        return this.userInfoMapper.updateByNickName(bean, nickName);
    }

    /**
     * 根据NickName删除
     */
    @Override
    public Integer deleteUserInfoByNickName(String nickName) {
        return this.userInfoMapper.deleteByNickName(nickName);
    }

}