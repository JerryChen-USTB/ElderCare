package edu.ustb.eldercarebackend.controller.volunteer;
import edu.ustb.eldercarebackend.entity.guardian.ResultVO;
import edu.ustb.eldercarebackend.entity.Volunteer;
import edu.ustb.eldercarebackend.service.volunteer.VolunteerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/volunteer")
public class VolunteerController {

    @Autowired
    private VolunteerService volunteerService;

    /**
     * 新增接口：根据userId查询志愿者信息（推荐用这个，无需改UserMapper）
     * 请求方式：GET
     * 请求地址：http://localhost:8080/api/volunteer/infoByUserId
     * 请求参数：userId（必传，如1001，前端从登录信息中获取）
     * 返回示例：和之前的/info接口一致，包含volunteerInfo
     */
    @GetMapping("/infoByUserId")
    public Map<String, Object> getVolunteerByUserId(@RequestParam Integer userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 直接调用Service的getVolunteerByUserId方法
            Volunteer volunteer = volunteerService.getVolunteerByUserId(userId);
            if (volunteer != null) {
                result.put("success", true);
                result.put("volunteerInfo", volunteer); // 返回完整志愿者信息
            } else {
                result.put("success", false);
                result.put("message", "未查询到该用户ID对应的志愿者信息");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "查询失败：系统错误");
        }
        return result;
    }
    @PostMapping("/update")
    public Map<String, Object> updateVolunteerInfo(@RequestBody Volunteer volunteer) {
        Map<String, Object> result = new HashMap<>();

        try {
            // -------------------------- 1. 必传参数校验 --------------------------
            if (volunteer.getId() == null) {
                result.put("success", false);
                result.put("message", "志愿者记录ID不能为空，请刷新页面后重试");
                return result;
            }

            if (volunteer.getUserId() == null) {
                result.put("success", false);
                result.put("message", "用户ID不能为空，无法确认身份，请重新登录");
                return result;
            }

            // -------------------------- 2. 身份合法性校验 --------------------------
            // 调用Service的getVolunteerById获取原记录（替代volunteerMapper.selectById）
            Volunteer originalVolunteer = volunteerService.getVolunteerById(volunteer.getId());
            if (originalVolunteer == null) {
                result.put("success", false);
                result.put("message", "未找到ID为" + volunteer.getId() + "的志愿者记录，修改失败");
                return result;
            }

            if (!originalVolunteer.getUserId().equals(volunteer.getUserId())) {
                result.put("success", false);
                result.put("message", "身份校验失败，你无权修改他人的志愿者信息");
                return result;
            }

            // -------------------------- 3. 补充不可修改/自动更新的字段 --------------------------
            volunteer.setVerificationStatus(originalVolunteer.getVerificationStatus());
            volunteer.setCreatedAt(originalVolunteer.getCreatedAt());
            volunteer.setUpdatedAt(new Date());

            // -------------------------- 4. 执行修改操作（调用Service的方法） --------------------------
            int updateRows = volunteerService.updateVolunteerById(volunteer);

            // -------------------------- 5. 处理修改结果 --------------------------
            if (updateRows > 0) {
                Volunteer latestVolunteer = volunteerService.getVolunteerById(volunteer.getId());
                result.put("success", true);
                result.put("message", "志愿者信息修改成功");
                result.put("volunteerInfo", latestVolunteer);
            } else {
                result.put("success", true);
                result.put("message", "志愿者信息未变更");
                result.put("volunteerInfo", originalVolunteer);
            }

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "志愿者信息修改失败：" + e.getMessage().substring(0, 100));
        }

        return result;
    }
    // 从配置文件获取上传路径
    @Value("${app.upload.avatar-path}")
    private String avatarUploadPath;

    @Value("${app.upload.static-mapping}")
    private String staticMapping;

    @PostMapping("/uploadAvatar")
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

            String filename = "volunteer_" + userId + "_" + UUID.randomUUID() + fileExt;

            // 6. 保存文件到指定路径（修改点4：使用新路径保存）
            Path filePath = Paths.get(avatarUploadPath + File.separator + filename);
            Files.write(filePath, file.getBytes());

            // 7. 构建可访问的图片URL（修改点5：URL不包含子目录）
            String avatarUrl = staticMapping.replace("**", "") + filename;

            // 8. 先查询旧头像URL（用于删除）
            Volunteer volunteer = volunteerService.getVolunteerByUserId(userId);
            String oldAvatarUrl =volunteer != null ? volunteer.getAvatarUrl() : null;

            // 9. 更新数据库中的头像URL
            String updateResult = volunteerService.updateVolunteerAvatar(userId, avatarUrl);
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
     * 删除旧头像文件
     */
    private void deleteOldAvatar(String oldAvatarUrl) {
        try {

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
}