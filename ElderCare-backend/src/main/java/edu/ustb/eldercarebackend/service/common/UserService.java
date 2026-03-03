package edu.ustb.eldercarebackend.service.common;

import edu.ustb.eldercarebackend.entity.User;
import edu.ustb.eldercarebackend.entity.guardian.ResultVO;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 用户注册
     * @param user 用户信息
     * @return 注册结果
     */
    String register(User user);
    
    /**
     * 用户注册（带验证码校验）
     * @param user 用户信息
     * @param verifyCode 验证码
     * @return 注册结果
     */
    String registerWithCode(User user, String verifyCode);
    
    /**
     * 用户登录
     * @param phone 手机号
     * @param password 密码
     * @return 登录结果，包含用户信息
     */
    User login(String phone, String password);
    
    /**
     * 根据手机号查询用户
     * @param phone 手机号
     * @return 用户信息
     */
    User findByPhone(String phone);
    
    /**
     * 更新用户角色
     * @param phone 手机号
     * @param role 新角色
     * @return 更新结果
     */
    String updateUserRole(String phone, String role);
    /**
     * 发送注册验证码
     * @param phone 用户手机号
     * @return 发送结果（包含验证码）
     */
    ResultVO sendRegisterCode(String phone);
    
    /**
     * 发送忘记密码验证码
     * @param phone 用户手机号
     * @return 发送结果（成功/失败信息）
     */
    ResultVO sendForgotPwdCode(String phone);

    /**
     * 验证验证码并重置密码
     * @param phone 手机号
     * @param verifyCode 输入的验证码
     * @param newPassword 新密码
     * @return 重置结果（成功/失败信息）
     */
    ResultVO resetPassword(String phone, String verifyCode, String newPassword);

    /**
     * 根据用户ID查询用户
     * @param userId 用户ID
     * @return 用户信息
     */
    User findById(Integer userId);

    /**
     * 更新用户信息
     * @param user 用户对象
     * @return 更新结果
     */
    boolean updateUser(User user);
}