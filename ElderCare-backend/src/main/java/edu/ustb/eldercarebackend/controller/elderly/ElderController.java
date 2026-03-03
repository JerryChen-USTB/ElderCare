package edu.ustb.eldercarebackend.controller.elderly;

import edu.ustb.eldercarebackend.entity.Elder;
import edu.ustb.eldercarebackend.entity.guardian.ElderDTO;
import edu.ustb.eldercarebackend.service.elderly.ElderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
        import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 老人信息控制器
 */
@RestController
@RequestMapping("/api/elder")
@CrossOrigin(origins = "*")
public class ElderController {

    @Autowired
    private ElderService elderService;

    // 头像上传目录（Spring Boot静态资源目录）
    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/avatars/";

    /**
     * 根据用户ID获取老人信息
     */
    @GetMapping("/info/{userId}")
    public Map<String, Object> getElderInfo(@PathVariable Integer userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            Elder elder = elderService.getElderByUserId(userId);

            if (elder != null) {
                result.put("success", true);
                result.put("elder", elder);
            } else {
                result.put("success", false);
                result.put("message", "老人信息不存在");
            }

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "查询失败：系统错误");
        }

        return result;
    }

    /**
     * 更新老人姓名
     */
    @PostMapping("/updateName")
    public Map<String, Object> updateElderName(@RequestBody Map<String, Object> requestData) {
        Map<String, Object> result = new HashMap<>();

        try {
            Integer userId = (Integer) requestData.get("userId");
            String name = (String) requestData.get("name");

            // 参数验证
            if (userId == null) {
                result.put("success", false);
                result.put("message", "用户ID不能为空");
                return result;
            }

            if (name == null || name.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "姓名不能为空");
                return result;
            }

            String updateResult = elderService.updateElderName(userId, name);

            if ("姓名更新成功".equals(updateResult)) {
                result.put("success", true);
                result.put("message", updateResult);

                // 返回更新后的老人信息
                Elder updatedElder = elderService.getElderByUserId(userId);
                result.put("elder", updatedElder);
            } else {
                result.put("success", false);
                result.put("message", updateResult);
            }

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "更新失败：系统错误");
        }

        return result;
    }

    /**
     * 上传头像
     */
    @PostMapping("/uploadAvatar")
    public Map<String, Object> uploadAvatar(@RequestParam("file") MultipartFile file, @RequestParam("userId") Integer userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 参数验证
            if (file.isEmpty()) {
                result.put("success", false);
                result.put("message", "请选择要上传的文件");
                return result;
            }

            if (userId == null) {
                result.put("success", false);
                result.put("message", "用户ID不能为空");
                return result;
            }

            // 检查文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                result.put("success", false);
                result.put("message", "只能上传图片文件");
                return result;
            }

            // 检查文件大小（限制为5MB）
            if (file.getSize() > 5 * 1024 * 1024) {
                result.put("success", false);
                result.put("message", "文件大小不能超过5MB");
                return result;
            }

            // 🔍 获取用户当前的头像URL（用于后续删除旧头像）
            String oldAvatarUrl = null;
            try {
                Elder currentElder = elderService.getElderByUserId(userId);
                if (currentElder != null && currentElder.getAvatarUrl() != null) {
                    oldAvatarUrl = currentElder.getAvatarUrl();
                }
            } catch (Exception e) {
                System.out.println("⚠️ 获取旧头像信息失败，但不影响上传: " + e.getMessage());
            }

            // 创建上传目录
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = "avatar_" + userId + "_" + UUID.randomUUID().toString() + fileExtension;

            // 保存文件
            Path filePath = Paths.get(UPLOAD_DIR + filename);
            Files.write(filePath, file.getBytes());

            // 构建访问URL
            String avatarUrl = "/uploads/avatars/" + filename;

            // 更新数据库
            String updateResult = elderService.updateElderAvatar(userId, avatarUrl);

            if ("头像更新成功".equals(updateResult)) {
                // 🗑️ 数据库更新成功后，删除旧头像文件
                if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty()) {
                    deleteOldAvatarFile(oldAvatarUrl);
                }

                result.put("success", true);
                result.put("message", "头像上传成功");
                result.put("avatarUrl", avatarUrl);

                // 返回更新后的老人信息
                Elder updatedElder = elderService.getElderByUserId(userId);
                result.put("elder", updatedElder);
            } else {
                // 数据库更新失败，删除刚上传的文件
                try {
                    Files.deleteIfExists(filePath);
                    System.out.println("🗑️ 数据库更新失败，已清理临时文件: " + filename);
                } catch (IOException e) {
                    System.err.println("❌ 清理临时文件失败: " + e.getMessage());
                }

                result.put("success", false);
                result.put("message", updateResult);
            }

        } catch (IOException e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "文件上传失败：" + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "上传失败：系统错误");
        }

        return result;
    }

    /**
     * 删除旧头像文件
     * @param oldAvatarUrl 旧头像的URL路径
     */
    private void deleteOldAvatarFile(String oldAvatarUrl) {
        try {
            // 安全检查：只删除 /uploads/avatars/ 目录下的文件
            if (!oldAvatarUrl.startsWith("/uploads/avatars/")) {
                System.out.println("⚠️ 跳过删除：头像路径不在允许的目录范围内 - " + oldAvatarUrl);
                return;
            }

            // 安全检查：不删除默认头像
            if (oldAvatarUrl.contains("default-avatar")) {
                System.out.println("⚠️ 跳过删除：不删除默认头像文件 - " + oldAvatarUrl);
                return;
            }

            // 提取文件名（去除URL前缀）
            String filename = oldAvatarUrl.substring("/uploads/avatars/".length());

            // 安全检查：防止路径遍历攻击
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                System.err.println("❌ 危险的文件路径，拒绝删除: " + filename);
                return;
            }

            // 构建完整的文件路径
            Path oldFilePath = Paths.get(UPLOAD_DIR + filename);

            // 确保文件在预期的目录内
            Path uploadDirPath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            Path targetFilePath = oldFilePath.toAbsolutePath().normalize();

            if (!targetFilePath.startsWith(uploadDirPath)) {
                System.err.println("❌ 文件路径不在安全目录内，拒绝删除: " + targetFilePath);
                return;
            }

            // 删除文件
            if (Files.deleteIfExists(oldFilePath)) {
                System.out.println("🗑️ 成功删除旧头像文件: " + filename);
            } else {
                System.out.println("⚠️ 旧头像文件不存在或已被删除: " + filename);
            }

        } catch (Exception e) {
            System.err.println("❌ 删除旧头像文件时出错: " + e.getMessage());
            // 不抛出异常，因为删除失败不应该影响头像上传的成功
        }
    }

    /**
     * 更新老人基本信息
     */
    @PostMapping("/updateInfo")
    public Map<String, Object> updateElderInfo(@RequestBody Elder elder) {
        Map<String, Object> result = new HashMap<>();

        try {
            String updateResult = elderService.updateElderInfo(elder);

            if ("信息更新成功".equals(updateResult)) {
                result.put("success", true);
                result.put("message", updateResult);

                // 返回更新后的老人信息
                Elder updatedElder = elderService.getElderByUserId(elder.getUserId());
                result.put("elder", updatedElder);
            } else {
                result.put("success", false);
                result.put("message", updateResult);
            }

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "更新失败：系统错误");
        }

        return result;
    }


    /**
     * 通过 user_id 查询老人信息（直接返回Service处理结果）
     * @param userId 老人的 users.id
     * @return 老人信息Map（无额外封装，同GuardianController风格）
     */
    @GetMapping("/selfinfo/{userId}")
    public ElderDTO getElderByUserId(@PathVariable Integer userId) {
        return elderService.getElderInfoByUserId(userId);
    }
}
