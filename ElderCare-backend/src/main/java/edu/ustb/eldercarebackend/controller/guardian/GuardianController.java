package edu.ustb.eldercarebackend.controller.guardian;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.ustb.eldercarebackend.entity.Elder;
import edu.ustb.eldercarebackend.entity.Guardian;
import edu.ustb.eldercarebackend.entity.Relation;
import edu.ustb.eldercarebackend.entity.guardian.*;
import edu.ustb.eldercarebackend.mapper.RelationMapper;
import edu.ustb.eldercarebackend.service.guardian.GuardianService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 监护人控制器
 */
@RestController
@RequestMapping("/api/guardian")
public class GuardianController {
    @Autowired
    private GuardianService guardianService;
    @Autowired
    private RelationMapper relationMapper;

    // 从配置文件获取上传路径（修改点1：使用新配置的路径）
    @Value("${app.upload.avatar-path}")
    private String avatarUploadPath;  // 现在值为 D:/Desktop/avatarstest

    @Value("${app.upload.static-mapping}")
    private String staticMapping;  // 值为 /upload/**

    /**
     * 监护人头像上传接口
     */
    @PostMapping("/avatar/upload")
    public ResultVO uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Integer userId
    ) {
        // 1. 参数校验
        if (file.isEmpty()) {
            return ResultVO.fail("请选择图片文件");
        }
        if (userId == null || userId <= 0) {
            return ResultVO.fail("用户ID无效");
        }

        // 2. 验证文件类型（仅允许图片）
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResultVO.fail("仅支持图片格式（jpg、png、jpeg等）");
        }

        // 3. 验证文件大小（限制5MB）
        long maxSize = 5 * 1024 * 1024;  // 5MB
        if (file.getSize() > maxSize) {
            return ResultVO.fail("文件大小不能超过5MB");
        }

        try {
            // 4. 确保上传目录存在（修改点2：适配新路径，不创建子目录）
            File uploadDir = new File(avatarUploadPath);
            if (!uploadDir.exists()) {
                boolean mkdirs = uploadDir.mkdirs();  // 直接创建配置的根目录
                if (!mkdirs) {
                    return ResultVO.fail("上传目录创建失败，请检查权限：" + avatarUploadPath);
                }
            }

            // 5. 生成唯一文件名（修改点3：文件名直接存放在根目录，不包含子目录）
            String originalFilename = file.getOriginalFilename();
            String fileExt = originalFilename != null
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".png";  // 默认扩展名
            // 文件名格式：guardian_用户ID_UUID.扩展名
            String filename = "guardian_" + userId + "_" + UUID.randomUUID() + fileExt;

            // 6. 保存文件到指定路径（修改点4：使用新路径保存）
            Path filePath = Paths.get(avatarUploadPath + File.separator + filename);
            Files.write(filePath, file.getBytes());

            // 7. 构建可访问的图片URL（修改点5：URL不包含子目录）
            String avatarUrl = staticMapping.replace("**", "") + filename;
            // 例如：staticMapping为/upload/** → 最终URL为/upload/guardian_123.png

            // 8. 先查询旧头像URL（用于删除）
            Guardian guardian = guardianService.getGuardianByUserId(userId);
            String oldAvatarUrl = guardian != null ? guardian.getAvatarUrl() : null;

            // 9. 更新数据库中的头像URL
            String updateResult = guardianService.updateGuardianAvatar(userId, avatarUrl);
            if (!"头像更新成功".equals(updateResult)) {
                // 数据库更新失败，删除刚上传的文件
                Files.deleteIfExists(filePath);
                return ResultVO.fail(updateResult);
            }

            // 10. 删除旧头像（修改点6：适配新路径的删除逻辑）
            if (oldAvatarUrl != null && !oldAvatarUrl.contains("default")) {  // 跳过默认头像
                deleteOldAvatar(oldAvatarUrl);
            }

            // 11. 返回成功结果
            return ResultVO.success(new AvatarResult(avatarUrl, filename));

        } catch (IOException e) {
            return ResultVO.fail("文件上传失败：" + e.getMessage());
        } catch (Exception e) {
            return ResultVO.fail("系统异常：" + e.getMessage());
        }
    }

    /**
     * 删除旧头像文件（复用老人头像的删除逻辑，确保安全）
     */
    private void deleteOldAvatar(String oldAvatarUrl) {
        try {
            // 从URL解析文件名（例如：/upload/guardian_123.png → guardian_123.png）
            String filename = oldAvatarUrl.substring(oldAvatarUrl.lastIndexOf("/") + 1);
            Path oldFilePath = Paths.get(avatarUploadPath + File.separator + filename);

            // 安全校验：确保删除的是上传目录内的文件
            if (Files.exists(oldFilePath) &&
                    oldFilePath.toAbsolutePath().startsWith(Paths.get(avatarUploadPath).toAbsolutePath())) {
                Files.delete(oldFilePath);
            }
        } catch (Exception e) {
            System.err.println("删除旧头像失败：" + e.getMessage());
        }
    }


    // 头像上传结果DTO
    static class AvatarResult {
        private String url;  // 可访问的URL
        private String filename;  // 服务器保存的文件名

        public AvatarResult(String url, String filename) {
            this.url = url;
            this.filename = filename;
        }

        // getter方法
        public String getUrl() { return url; }
        public String getFilename() { return filename; }
    }

    /**
     * 返回监护人完整信息（包含姓名、手机号、user_id等所有字段）
     */
    @GetMapping("/info/{userId}")
    public Guardian getGuardianInfo(@PathVariable Integer userId) {
        return guardianService.getGuardianByUserId(userId);
    }

    /**
     * 更新监护人信息（支持部分字段更新：昵称、性别、出生日期）
     */
    @PostMapping("/updateinfo")
    public ResultVO updateGuardianInfo(@RequestBody UpdateGuardianDTO updateDTO) {
        try {
            // 调用服务层更新信息
            Guardian updatedGuardian = guardianService.updateGuardianInfo(updateDTO);
            // 用ResultVO包装成功响应（code=200，data为更新后的实体）
            return ResultVO.success(updatedGuardian);
        } catch (Exception e) {
            // 异常时返回失败信息（code=400，msg为错误提示）
            return ResultVO.fail("更新失败：" + e.getMessage());
        }
    }

    /**
     * 返回监护人关联老人信息
     */
    @GetMapping("/related-elders/{guardianUserId}")
    public List<ElderDTO> getRelatedElders(@PathVariable Integer guardianUserId) {
        return guardianService.getRelatedElders(guardianUserId);
    }
    /**
     * 发送验证码（用于修改密码）
     */
    @PostMapping("/updatepw/send-code")
    public ResultVO sendVerificationCode(@RequestBody SendCodeDTO sendCodeDTO) {
        try {
            if (sendCodeDTO.getUserId() == null) {
                return ResultVO.fail("用户ID不能为空");
            }
            return guardianService.sendVerificationCode(sendCodeDTO.getUserId());
        } catch (Exception e) {
            e.printStackTrace();
            return ResultVO.fail("发送验证码失败：" + e.getMessage());
        }
    }

    /**
     * 修改密码（基于验证码验证）
     */
    @PostMapping("/update-password")
    public ResultVO updatePassword(@RequestBody UpdatePasswordDTO updatePasswordDTO) {
        try {
            return guardianService.modifyPassword(updatePasswordDTO);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultVO.fail("修改密码失败：" + e.getMessage());
        }
    }
    /**
     * 获取用户旧密码的加密哈希（用于前端比对新旧密码）
     * 注意：必须验证用户身份，确保只能查询自己的密码哈希
     */
    @GetMapping("/getoldpassword")
    public ResultVO getOldPasswordHash(@RequestParam Integer userId) {
        try {
            // 【关键】此处需添加身份验证（如通过Token判断当前登录用户是否为userId对应的用户）
            // 示例：假设从Token中解析出当前登录用户ID为loginUserId
            // if (!loginUserId.equals(userId)) {
            //     return ResultVO.fail("无权访问");
            // }

            String oldPasswordHash = guardianService.getOldPasswordHash(userId);
            return ResultVO.success(oldPasswordHash);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultVO.fail("获取旧密码信息失败：" + e.getMessage());
        }
    }
    @PostMapping("/updatephone/send-code")
    public ResultVO sendUpdatePhoneCode(@RequestBody Map<String, Object> params) {
        try {
            Integer userId = (Integer) params.get("userId");
            String newPhone = (String) params.get("newPhone");

            if (userId == null) {
                return ResultVO.fail("用户ID不能为空");
            }
            if (newPhone == null) {
                return ResultVO.fail("新手机号不能为空");
            }

            return guardianService.sendUpdatePhoneCode(userId, newPhone);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultVO.fail("发送验证码失败：" + e.getMessage());
        }
    }
    /**
     * 通过验证码更新手机号
     */
    @PostMapping("/update-phone-by-code")
    public ResultVO updatePhoneByCode(@RequestBody UpdatePhoneDTO dto) {
        try {
            return guardianService.updatePhoneByCode(dto);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultVO.fail("更新手机号失败：" + e.getMessage());
        }
    }
    /**
     * 通过密码更新手机号
     */
    @PostMapping("/update-phone-by-password")
    public ResultVO updatePhoneByPassword(@RequestBody Map<String, Object> params) {
        try {
            Integer userId = (Integer) params.get("userId");
            String password = (String) params.get("password");
            String newPhone = (String) params.get("newPhone");

            if (userId == null) {
                return ResultVO.fail("用户ID不能为空");
            }
            if (password == null) {
                return ResultVO.fail("密码不能为空");
            }
            if (newPhone == null) {
                return ResultVO.fail("新手机号不能为空");
            }

            return guardianService.updatePhoneByPassword(userId, password, newPhone);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultVO.fail("更新手机号失败：" + e.getMessage());
        }
    }

}
