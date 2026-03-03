package edu.ustb.eldercarebackend.controller.common;

import edu.ustb.eldercarebackend.entity.User;
import edu.ustb.eldercarebackend.entity.guardian.ResultVO;
import edu.ustb.eldercarebackend.service.common.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/user")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    /**
     * 发送注册验证码
     */
    @PostMapping("/register/send-code")
    public ResultVO sendRegisterCode(@RequestBody Map<String, String> param) {
        String phone = param.get("phone");
        return userService.sendRegisterCode(phone);
    }
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> registerData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String phone = registerData.get("phone");
            String password = registerData.get("password");
            String verifyCode = registerData.get("verifyCode");
            
            // 参数验证
            if (phone == null || phone.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "手机号不能为空");
                return result;
            }
            
            if (password == null || password.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "密码不能为空");
                return result;
            }
            
            if (verifyCode == null || verifyCode.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "验证码不能为空");
                return result;
            }
            
            // 验证手机号格式
            if (!phone.matches("^1[3-9]\\d{9}$")) {
                result.put("success", false);
                result.put("message", "手机号格式不正确");
                return result;
            }
            
            // 验证密码格式
            if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d._]{8,16}$")) {
                result.put("success", false);
                result.put("message", "密码需8-16位，包含数字和字母，符号只能使用._");
                return result;
            }
            
            // 检查手机号是否已注册
            User existUser = userService.findByPhone(phone);
            if (existUser != null) {
                result.put("success", false);
                result.put("message", "手机号已注册");
                return result;
            }
            
            // 创建用户对象
            User user = new User();
            user.setPhone(phone);
            user.setPassword(password);
            
            // 调用注册服务（包含验证码校验）
            String registerResult = userService.registerWithCode(user, verifyCode);
            
            if ("注册成功".equals(registerResult)) {
                result.put("success", true);
                result.put("message", registerResult);
            } else {
                result.put("success", false);
                result.put("message", registerResult);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "注册失败：系统错误");
        }
        
        return result;
    }
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> loginData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String phone = loginData.get("phone");
            String password = loginData.get("password");
            
            // 参数验证
            if (phone == null || phone.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "手机号不能为空");
                return result;
            }
            
            if (password == null || password.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "密码不能为空");
                return result;
            }
            
            // 验证手机号格式
            if (!phone.matches("^1[3-9]\\d{9}$")) {
                result.put("success", false);
                result.put("message", "手机号格式不正确");
                return result;
            }
            
            User users = userService.login(phone, password);
            
            if (users != null) {
                result.put("success", true);
                result.put("message", "登录成功");
                result.put("user", users);
            } else {
                result.put("success", false);
                result.put("message", "手机号或密码错误");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "登录失败：系统错误");
        }
        
        return result;
    }
    
    /**
     * 根据手机号查询用户信息
     */
    @GetMapping("/info/{phone}")
    public Map<String, Object> getUserInfo(@PathVariable String phone) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            User users = userService.findByPhone(phone);
            
            if (users != null) {
                result.put("success", true);
                result.put("user", users);
            } else {
                result.put("success", false);
                result.put("message", "用户不存在");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "查询失败：系统错误");
        }
        
        return result;
    }
    
    /**
     * 更新用户角色
     */
    @PostMapping("/updateRole")
    public Map<String, Object> updateRole(@RequestBody Map<String, String> roleData) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String phone = roleData.get("phone");
            String role = roleData.get("role");
            
            // 参数验证
            if (phone == null || phone.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "手机号不能为空");
                return result;
            }
            
            if (role == null || role.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "用户角色不能为空");
                return result;
            }
            
            // 验证用户角色
            if (!role.equals("elder") && !role.equals("guardian") && !role.equals("volunteer")) {
                result.put("success", false);
                result.put("message", "用户角色必须是elder、guardian或volunteer");
                return result;
            }
            
            String updateResult = userService.updateUserRole(phone, role);
            
            if ("角色更新成功".equals(updateResult)) {
                result.put("success", true);
                result.put("message", updateResult);
                
                // 返回更新后的用户信息
                User updatedUser = userService.findByPhone(phone);
                result.put("user", updatedUser);
            } else {
                result.put("success", false);
                result.put("message", updateResult);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "角色更新失败：系统错误");
        }
        
        return result;
    }
    /**
     * 1. 发送忘记密码验证码（前端getVerifyCode方法调用）
     * 请求地址：/api/user/forgot-password/send-code
     * 请求体：{ "phone": "用户输入的手机号" }
     */
    @PostMapping("/forgot-password/send-code")
    public Map<String, Object> sendForgotPwdCode(@RequestBody Map<String, String> param) {
        Map<String, Object> result = new HashMap<>();
        try {
            String phone = param.get("phone");
            ResultVO vo = userService.sendForgotPwdCode(phone);

            result.put("success", vo.getCode() == 200);
            result.put("message", vo.getMsg());
            result.put("data", vo.getData());  // 返回验证码（用于开发/测试环境）
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "发送验证码失败：系统错误");
        }
        return result;
    }
    /**
     * 2. 验证验证码并重置密码（前端resetPassword方法调用）
     * 请求地址：/api/user/forgot-password/reset
     * 请求体：{ "phone": "手机号", "verifyCode": "验证码", "newPassword": "新密码" }
     */
    @PostMapping("/forgot-password/reset")
    public Map<String, Object> resetPassword(@RequestBody Map<String, String> param) {
        Map<String, Object> result = new HashMap<>();
        try {
            String phone = param.get("phone");
            String verifyCode = param.get("verifyCode");
            String newPassword = param.get("newPassword");

            ResultVO vo = userService.resetPassword(phone, verifyCode, newPassword);

            result.put("success", vo.getCode() == 200);
            result.put("message", vo.getMsg());
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "重置密码失败：系统错误");
        }
        return result;
    }

    /**
     * 更新用户的推送设备ID
     * @param request 包含 userId 和 pushClientId
     * @return 更新结果
     */
    @PostMapping("/updatePushClientId")
    public Map<String, Object> updatePushClientId(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Integer userId = (Integer) request.get("userId");
            String pushClientId = (String) request.get("pushClientId");
            
            if (userId == null) {
                result.put("success", false);
                result.put("message", "用户ID不能为空");
                return result;
            }
            
            if (pushClientId == null || pushClientId.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "推送设备ID不能为空");
                return result;
            }
            
            System.out.println("📱 更新用户推送设备ID：userId=" + userId + ", pushClientId=" + pushClientId);
            
            // 查询用户
            User user = userService.findById(userId);
            if (user == null) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }
            
            // 更新 pushClientId
            user.setPushClientId(pushClientId);
            boolean updateResult = userService.updateUser(user);
            
            if (updateResult) {
                System.out.println("✅ 推送设备ID更新成功");
                result.put("success", true);
                result.put("message", "推送设备ID更新成功");
            } else {
                result.put("success", false);
                result.put("message", "更新失败");
            }
            
        } catch (Exception e) {
            System.err.println("❌ 更新推送设备ID失败：" + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "更新失败：系统错误");
        }
        
        return result;
    }
}