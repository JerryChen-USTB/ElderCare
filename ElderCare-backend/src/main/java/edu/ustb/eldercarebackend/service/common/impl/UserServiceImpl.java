package edu.ustb.eldercarebackend.service.common.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.ustb.eldercarebackend.entity.Elder;
import edu.ustb.eldercarebackend.entity.Guardian;
import edu.ustb.eldercarebackend.entity.User;
import edu.ustb.eldercarebackend.entity.Volunteer;
import edu.ustb.eldercarebackend.entity.guardian.ResultVO;
import edu.ustb.eldercarebackend.mapper.ElderMapper;
import edu.ustb.eldercarebackend.mapper.GuardianMapper;
import edu.ustb.eldercarebackend.mapper.UserMapper;
import edu.ustb.eldercarebackend.mapper.VolunteerMapper;
import edu.ustb.eldercarebackend.service.common.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private ElderMapper elderMapper;
    
    @Autowired
    private GuardianMapper guardianMapper;
    
    @Autowired
    private VolunteerMapper volunteerMapper;

    // ========== 新增：验证码存储与有效期相关定义 ==========
    // 存储"手机号 -> 验证码"的映射（线程安全）
    private final ConcurrentMap<String, String> forgotPwdCodes = new ConcurrentHashMap<>();
    // 存储注册验证码
    private final ConcurrentMap<String, String> registerCodes = new ConcurrentHashMap<>();
    // 存储"手机号 -> 验证码过期时间戳"的映射（线程安全）
    private final ConcurrentMap<String, Long> codeExpireTimes = new ConcurrentHashMap<>();
    // 存储注册验证码过期时间
    private final ConcurrentMap<String, Long> registerCodeExpireTimes = new ConcurrentHashMap<>();
    // 验证码有效期：5分钟（单位：毫秒）
    private static final long CODE_EXPIRE_MILLIS = 5 * 60 * 1000;
    // ========== 新增结束 ==========
    
    @Value("${app.user-data.root-path}")
    private String userDataRootPath;
    
    @Value("${app.user-data.users-dir}")
    private String usersDir;
    
    @Override
    public String register(User user) {
        // 检查手机号是否已存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone", user.getPhone());
        User existUser = userMapper.selectOne(queryWrapper);
        
        if (existUser != null) {
            return "手机号已存在";
        }
        
        // 密码加密（简单MD5加密，实际项目建议使用BCrypt）
        String encryptedPassword = DigestUtils.md5DigestAsHex(user.getPassword().getBytes());
        user.setPassword(encryptedPassword);
        
        try {
            userMapper.insert(user);
            return "注册成功";
        } catch (Exception e) {
            e.printStackTrace();
            return "注册失败：" + e.getMessage();
        }
    }
    
    @Override
    public String registerWithCode(User user, String verifyCode) {
        // 1. 校验验证码
        String phone = user.getPhone();
        String storedCode = registerCodes.get(phone);
        
        // 验证码不存在
        if (storedCode == null) {
            return "验证码已过期，请重新获取";
        }
        
        // 验证码已过期
        Long expireTime = registerCodeExpireTimes.get(phone);
        if (expireTime == null || System.currentTimeMillis() > expireTime) {
            registerCodes.remove(phone);
            registerCodeExpireTimes.remove(phone);
            return "验证码已过期，请重新获取";
        }
        
        // 验证码错误
        if (!verifyCode.equals(storedCode)) {
            return "验证码错误";
        }
        
        // 2. 验证码正确，执行注册
        // 检查手机号是否已存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone", phone);
        User existUser = userMapper.selectOne(queryWrapper);
        
        if (existUser != null) {
            return "手机号已存在";
        }
        
        // 密码加密
        String encryptedPassword = DigestUtils.md5DigestAsHex(user.getPassword().getBytes());
        user.setPassword(encryptedPassword);
        
        try {
            userMapper.insert(user);
            
            // 3. 注册成功后删除验证码（防止重复使用）
            registerCodes.remove(phone);
            registerCodeExpireTimes.remove(phone);
            
            return "注册成功";
        } catch (Exception e) {
            e.printStackTrace();
            return "注册失败：" + e.getMessage();
        }
    }
    
    @Override
    public User login(String phone, String password) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone", phone);
        User user = userMapper.selectOne(queryWrapper);
        
        if (user == null) {
            return null; // 用户不存在
        }
        
        // 验证密码
        String encryptedPassword = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!encryptedPassword.equals(user.getPassword())) {
            return null; // 密码错误
        }
        
        // 登录成功，检查用户角色记录是否存在，如果不存在则创建
        if (user.getRole() != null && !user.getRole().trim().isEmpty()) {
            try {
                String roleCheckResult = createRoleRecord(user, user.getRole());
                // 记录日志，但不影响登录流程
                if (!roleCheckResult.contains("角色记录已存在")) {
                    System.out.println("用户登录时角色记录检查：" + roleCheckResult);
                }
            } catch (Exception e) {
                // 角色记录创建失败不影响登录，只记录错误
                System.err.println("登录时角色记录检查失败：" + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // 创建用户数据目录
        try {
            createUserDataDirectory(user.getId().toString());
        } catch (Exception e) {
            // 用户目录创建失败不影响登录，只记录错误
            System.err.println("创建用户数据目录失败：" + e.getMessage());
            e.printStackTrace();
        }
        
        // 更新最后登录时间
        try {
            user.setLastLoginAt(new Date());
            userMapper.updateById(user);
        } catch (Exception e) {
            // 更新登录时间失败不影响登录
            System.err.println("更新最后登录时间失败：" + e.getMessage());
        }
        
        // 不返回密码
        user.setPassword(null);
        return user;
    }
    
    @Override
    public User findByPhone(String phone) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone", phone);
        User user = userMapper.selectOne(queryWrapper);
        if (user != null) {
            user.setPassword(null); // 不返回密码
        }
        return user;
    }
    
    @Override
    public String updateUserRole(String phone, String role) {
        try {
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("phone", phone);
            User user = userMapper.selectOne(queryWrapper);
            
            if (user == null) {
                return "用户不存在";
            }
            
            // 检查用户是否已经有角色，避免重复创建角色记录
            String oldRole = user.getRole();
            
            // 更新用户角色
            user.setRole(role);
            user.setUpdatedAt(new Date());
            userMapper.updateById(user);
            
            // 如果是第一次设置角色（从null或空字符串变为具体角色），则在对应角色表中创建记录
            if (oldRole == null || oldRole.trim().isEmpty() || !oldRole.equals(role)) {
                String createRoleResult = createRoleRecord(user, role);
                if (!createRoleResult.contains("角色记录创建成功") && !createRoleResult.contains("角色记录已存在")) {
                    return "角色更新成功，但" + createRoleResult;
                }
            }
            
            return "角色更新成功";
            
        } catch (Exception e) {
            e.printStackTrace();
            return "角色更新失败：" + e.getMessage();
        }
    }

    @Override
    public ResultVO sendRegisterCode(String phone) {
        // 1. 基础参数校验
        if (!StringUtils.hasText(phone)) {
            return ResultVO.fail("手机号不能为空");
        }
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            return ResultVO.fail("请输入正确的手机号（11位数字）");
        }

        // 2. 校验手机号是否已注册
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone", phone);
        User user = userMapper.selectOne(queryWrapper);
        if (user != null) {
            return ResultVO.fail("该手机号已注册，请直接登录");
        }

        // 3. 生成6位随机验证码
        String verifyCode = String.format("%06d", new Random().nextInt(999999));

        // 4. 存储验证码及过期时间（覆盖旧验证码）
        registerCodes.put(phone, verifyCode);
        registerCodeExpireTimes.put(phone, System.currentTimeMillis() + CODE_EXPIRE_MILLIS);

        // 5. 模拟发送验证码（打印到控制台）
        System.out.println("================ 注册验证码 ================");
        System.out.println("手机号：" + phone);
        System.out.println("验证码：" + verifyCode);
        System.out.println("有效期：5分钟");
        System.out.println("===========================================");

        // 6. 返回验证码给前端（仅模拟环境）
        ResultVO result = ResultVO.success("验证码已生成");
        result.setData(verifyCode);
        return result;
    }

    @Override
    public ResultVO sendForgotPwdCode(String phone) {
        // 1. 基础参数校验
        if (!StringUtils.hasText(phone)) {
            return ResultVO.fail("手机号不能为空");
        }
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            return ResultVO.fail("请输入正确的手机号（11位数字）");
        }

        // 2. 校验手机号是否已注册（未注册用户无法找回密码）
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone", phone);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            return ResultVO.fail("该手机号未注册，请先注册");
        }

        // 3. 生成6位随机验证码
        String verifyCode = String.format("%06d", new Random().nextInt(999999));

        // 4. 存储验证码及过期时间（覆盖旧验证码）
        forgotPwdCodes.put(phone, verifyCode);
        codeExpireTimes.put(phone, System.currentTimeMillis() + CODE_EXPIRE_MILLIS);

        // 5. 模拟发送验证码（打印到控制台，核心需求）
        System.out.println("================ 忘记密码验证码 ================");
        System.out.println("手机号：" + phone);
        System.out.println("验证码：" + verifyCode);
        System.out.println("有效期：5分钟");
        System.out.println("===============================================");

        // 6. 在开发/测试模式下，返回验证码给前端（方便自动填充）
        ResultVO result = ResultVO.success("验证码已发送至控制台（有效期5分钟）");
        result.setData(verifyCode);  // 将验证码放入data字段返回给前端
        return result;
    }

    @Override
    public ResultVO resetPassword(String phone, String verifyCode, String newPassword) {
        // 1. 基础参数校验
        if (!StringUtils.hasText(phone)) return ResultVO.fail("手机号不能为空");
        if (!StringUtils.hasText(verifyCode)) return ResultVO.fail("验证码不能为空");
        if (!StringUtils.hasText(newPassword)) return ResultVO.fail("新密码不能为空");

        // 2. 校验手机号格式和用户存在性
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            return ResultVO.fail("请输入正确的手机号");
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone", phone);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            return ResultVO.fail("该手机号未注册，无法重置密码");
        }

        // 3. 校验验证码（存在性、过期、正确性）
        // 3.1 验证码是否存在
        if (!forgotPwdCodes.containsKey(phone)) {
            return ResultVO.fail("验证码不存在，请先获取验证码");
        }
        // 3.2 验证码是否过期
        long currentTime = System.currentTimeMillis();
        if (currentTime > codeExpireTimes.get(phone)) {
            // 过期后清除无效验证码
            forgotPwdCodes.remove(phone);
            codeExpireTimes.remove(phone);
            return ResultVO.fail("验证码已过期，请重新获取");
        }
        // 3.3 验证码是否正确
        if (!verifyCode.equals(forgotPwdCodes.get(phone))) {
            return ResultVO.fail("验证码错误，请重新输入");
        }

        // 4. 校验新密码格式（与注册一致）
        if (!newPassword.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d._]{8,16}$")) {
            return ResultVO.fail("密码需8-16位，包含数字和字母，符号只能使用._");
        }

        // 5. 校验新密码与旧密码是否重复（可选，提升安全性）
        String oldPwdHash = user.getPassword();
        String newPwdHash = DigestUtils.md5DigestAsHex(newPassword.trim().getBytes());
        if (newPwdHash.equals(oldPwdHash)) {
            return ResultVO.fail("新密码不能与旧密码相同，请更换密码");
        }

        // 6. 加密新密码并更新用户信息
        user.setPassword(newPwdHash);
        user.setUpdatedAt(new Date()); // 更新修改时间
        int updateRows = userMapper.updateById(user);

        if (updateRows > 0) {
            // 重置成功后清除验证码（防止重复使用）
            forgotPwdCodes.remove(phone);
            codeExpireTimes.remove(phone);
            return ResultVO.success("密码重置成功，请使用新密码登录");
        } else {
            return ResultVO.fail("密码重置失败，请重试");
        }
    }


    /**
     * 根据角色类型创建对应的角色记录
     * @param user 用户信息
     * @param role 角色类型
     * @return 创建结果
     */
    private String createRoleRecord(User user, String role) {
        try {
            Date now = new Date();
            
            switch (role) {
                case "elder":
                    // 检查是否已存在elder记录
                    QueryWrapper<Elder> elderQuery = new QueryWrapper<>();
                    elderQuery.eq("user_id", user.getId());
                    Elder existingElder = elderMapper.selectOne(elderQuery);
                    
                    if (existingElder == null) {
                        Elder elder = new Elder();
                        elder.setUserId(user.getId());
                        elder.setName(user.getPhone()); // 使用手机号作为默认姓名
                        elder.setGender("unknown"); // 默认值
                        elder.setAvatarUrl("/uploads/avatars/default-avatar.png"); // 🖼️ 设置默认头像URL
                        elder.setCreatedAt(now);
                        elder.setUpdatedAt(now);
                        elderMapper.insert(elder);
                        return "Elder角色记录创建成功";
                    } else {
                        return "Elder角色记录已存在";
                    }
                    
                case "guardian":
                    // 检查是否已存在guardian记录
                    QueryWrapper<Guardian> guardianQuery = new QueryWrapper<>();
                    guardianQuery.eq("user_id", user.getId());
                    Guardian existingGuardian = guardianMapper.selectOne(guardianQuery);
                    
                    if (existingGuardian == null) {
                        Guardian guardian = new Guardian();
                        guardian.setUserId(user.getId());
                        guardian.setName("游客" + user.getId()); // 昵称默认"游客+userId"
                        guardian.setGender("unknown"); // 默认值
                        guardian.setVerificationStatus("pending"); // 默认待审核
                        guardian.setCreatedAt(now);
                        guardian.setUpdatedAt(now);
                        guardianMapper.insert(guardian);
                        return "Guardian角色记录创建成功";
                    } else {
                        return "Guardian角色记录已存在";
                    }
                    
                case "volunteer":
                    // 检查是否已存在volunteer记录
                    QueryWrapper<Volunteer> volunteerQuery = new QueryWrapper<>();
                    volunteerQuery.eq("user_id", user.getId());
                    Volunteer existingVolunteer = volunteerMapper.selectOne(volunteerQuery);
                    
                    if (existingVolunteer == null) {
                        Volunteer volunteer = new Volunteer();
                        volunteer.setUserId(user.getId());
                        volunteer.setName(user.getPhone()); // 使用手机号作为默认姓名
                        volunteer.setGender("unknown"); // 默认值
                        volunteer.setVerificationStatus("pending"); // 默认待审核
                        volunteer.setCreatedAt(now);
                        volunteer.setUpdatedAt(now);
                        volunteerMapper.insert(volunteer);
                        return "Volunteer角色记录创建成功";
                    } else {
                        return "Volunteer角色记录已存在";
                    }
                    
                default:
                    return "不支持的角色类型：" + role;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return "角色记录创建失败：" + e.getMessage();
        }
    }
    
    /**
     * 创建用户数据目录结构
     * @param userId 用户ID
     */
    private void createUserDataDirectory(String userId) {
        try {
            // 构建用户目录路径：data/users/user_{userId}
            String userDir = userDataRootPath + File.separator + usersDir + File.separator + "user_" + userId;
            
            // 创建用户根目录
            File userDirectory = new File(userDir);
            if (!userDirectory.exists()) {
                boolean created = userDirectory.mkdirs();
                if (created) {
                    System.out.println("✅ 创建用户目录: " + userDir);
                } else {
                    System.err.println("❌ 无法创建用户目录: " + userDir);
                    return;
                }
            }
            
            // 创建转录文件目录
            String transcriptDir = userDir + File.separator + "transcripts";
            File transcriptDirectory = new File(transcriptDir);
            if (!transcriptDirectory.exists()) {
                boolean created = transcriptDirectory.mkdirs();
                if (created) {
                    System.out.println("✅ 创建用户转录目录: " + transcriptDir);
                } else {
                    System.err.println("❌ 无法创建用户转录目录: " + transcriptDir);
                }
            }
            
            // 创建AI总结目录
            String summaryDir = userDir + File.separator + "remote_assistance_AI_summary";
            File summaryDirectory = new File(summaryDir);
            if (!summaryDirectory.exists()) {
                boolean created = summaryDirectory.mkdirs();
                if (created) {
                    System.out.println("✅ 创建AI总结目录: " + summaryDir);
                } else {
                    System.err.println("❌ 无法创建AI总结目录: " + summaryDir);
                }
            }
            
            // 创建个人知识库目录
            String ragDir = userDir + File.separator + "rag_sources";
            File ragDirectory = new File(ragDir);
            if (!ragDirectory.exists()) {
                boolean created = ragDirectory.mkdirs();
                if (created) {
                    System.out.println("✅ 创建个人知识库目录: " + ragDir);
                } else {
                    System.err.println("❌ 无法创建个人知识库目录: " + ragDir);
                }
            }
            
            System.out.println("📁 用户数据目录结构已就绪 - 用户ID: " + userId);
            
        } catch (Exception e) {
            System.err.println("❌ 创建用户数据目录异常 - 用户ID: " + userId + ", 错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public User findById(Integer userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public boolean updateUser(User user) {
        try {
            int result = userMapper.updateById(user);
            return result > 0;
        } catch (Exception e) {
            System.err.println("❌ 更新用户信息失败：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
} 