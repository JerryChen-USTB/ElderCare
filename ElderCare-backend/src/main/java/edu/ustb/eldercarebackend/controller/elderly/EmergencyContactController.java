package edu.ustb.eldercarebackend.controller.elderly;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.ustb.eldercarebackend.entity.Guardian;
import edu.ustb.eldercarebackend.entity.Relation;
import edu.ustb.eldercarebackend.entity.User;
import edu.ustb.eldercarebackend.mapper.GuardianMapper;
import edu.ustb.eldercarebackend.mapper.RelationMapper;
import edu.ustb.eldercarebackend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 紧急联系人管理接口
 */
@RestController
@RequestMapping("/api/emergency-contact")
@CrossOrigin(origins = "*")
public class EmergencyContactController {

    @Autowired
    private RelationMapper relationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private GuardianMapper guardianMapper;

    /**
     * 获取老年人的所有紧急联系人
     * @param elderId 老年人ID
     * @return 紧急联系人列表
     */
    @GetMapping("/list")
    public Map<String, Object> getEmergencyContacts(@RequestParam Integer elderId) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (elderId == null) {
                response.put("success", false);
                response.put("message", "老年人ID不能为空");
                return response;
            }

            // 查询所有监护关系
            List<Relation> relations = relationMapper.selectByElderlyId(elderId);
            List<Map<String, Object>> contacts = new ArrayList<>();

            for (Relation relation : relations) {
                // 获取监护人用户信息
                User guardianUser = userMapper.selectById(relation.getGuardianId());
                if (guardianUser == null) {
                    continue;
                }

                // 获取监护人详细信息
                Guardian guardian = guardianMapper.selectOne(
                    new QueryWrapper<Guardian>().eq("user_id", guardianUser.getId())
                );

                Map<String, Object> contact = new HashMap<>();
                contact.put("relationId", relation.getId());
                contact.put("guardianId", guardianUser.getId());
                contact.put("name", guardian != null ? guardian.getName() : guardianUser.getPhone());
                contact.put("phone", guardianUser.getPhone());
                contact.put("remarks", relation.getRemarks());
                contact.put("relationship", relation.getRelationship());
                contact.put("isPrimary", relation.getIsPrimary());

                contacts.add(contact);
            }

            response.put("success", true);
            response.put("contacts", contacts);
            response.put("count", contacts.size());

        } catch (Exception e) {
            System.err.println("❌ 获取紧急联系人失败：" + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "获取联系人失败：" + e.getMessage());
        }

        return response;
    }

    /**
     * 验证监护人电话号码
     * @param phone 监护人电话
     * @param elderId 老年人ID
     * @return 验证结果和监护人信息
     */
    @GetMapping("/verify")
    public Map<String, Object> verifyGuardian(@RequestParam String phone, 
                                               @RequestParam Integer elderId) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (phone == null || phone.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "电话号码不能为空");
                return response;
            }

            // 1. 查找用户
            User user = userMapper.selectOne(
                new QueryWrapper<User>().eq("phone", phone)
            );

            if (user == null) {
                response.put("success", false);
                response.put("message", "未找到该电话号码对应的用户");
                return response;
            }

            // 2. 检查是否为监护人
            if (!"guardian".equals(user.getRole())) {
                response.put("success", false);
                response.put("message", "该用户不是监护人，无法添加为紧急联系人");
                return response;
            }

            // 3. 检查是否已经是紧急联系人
            Relation existingRelation = relationMapper.selectByGuardianAndElderly(
                user.getId(), 
                elderId
            );

            if (existingRelation != null) {
                response.put("success", false);
                response.put("message", "该监护人已是您的紧急联系人");
                return response;
            }

            // 4. 获取监护人详细信息
            Guardian guardian = guardianMapper.selectOne(
                new QueryWrapper<Guardian>().eq("user_id", user.getId())
            );

            Map<String, Object> guardianInfo = new HashMap<>();
            guardianInfo.put("guardianId", user.getId());
            guardianInfo.put("name", guardian != null ? guardian.getName() : phone);
            guardianInfo.put("phone", phone);

            response.put("success", true);
            response.put("guardianInfo", guardianInfo);
            response.put("message", "找到监护人，请确认是否添加");

        } catch (Exception e) {
            System.err.println("❌ 验证监护人失败：" + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "验证失败：" + e.getMessage());
        }

        return response;
    }

    /**
     * 添加紧急联系人
     * @param request 包含 elderId, guardianId, remarks
     * @return 添加结果
     */
    @PostMapping("/add")
    public Map<String, Object> addEmergencyContact(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            Integer elderId = (Integer) request.get("elderId");
            Integer guardianId = (Integer) request.get("guardianId");
            String remarks = (String) request.get("remarks");

            if (elderId == null || guardianId == null) {
                response.put("success", false);
                response.put("message", "参数不完整");
                return response;
            }

            // 再次检查是否已存在
            Relation existing = relationMapper.selectByGuardianAndElderly(guardianId, elderId);
            if (existing != null) {
                response.put("success", false);
                response.put("message", "该紧急联系人已存在");
                return response;
            }

            // 创建监护关系
            Relation relation = new Relation();
            relation.setGuardianId(guardianId);
            relation.setElderlyId(elderId);
            relation.setRelationship("other"); // 默认关系类型
            relation.setRemarks(remarks);
            relation.setIsPrimary(false);

            int result = relationMapper.insert(relation);

            if (result > 0) {
                response.put("success", true);
                response.put("message", "紧急联系人添加成功");
                response.put("relationId", relation.getId());
            } else {
                response.put("success", false);
                response.put("message", "添加失败");
            }

        } catch (Exception e) {
            System.err.println("❌ 添加紧急联系人失败：" + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "添加失败：" + e.getMessage());
        }

        return response;
    }

    /**
     * 删除紧急联系人
     * @param relationId 关系ID
     * @param elderId 老年人ID（用于验证权限）
     * @return 删除结果
     */
    @DeleteMapping("/delete")
    public Map<String, Object> deleteEmergencyContact(@RequestParam Integer relationId,
                                                       @RequestParam Integer elderId) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (relationId == null || elderId == null) {
                response.put("success", false);
                response.put("message", "参数不完整");
                return response;
            }

            // 验证关系是否属于该老年人
            Relation relation = relationMapper.selectById(relationId);
            if (relation == null) {
                response.put("success", false);
                response.put("message", "关系记录不存在");
                return response;
            }

            if (!relation.getElderlyId().equals(elderId)) {
                response.put("success", false);
                response.put("message", "无权删除该联系人");
                return response;
            }

            // 删除关系
            int result = relationMapper.deleteById(relationId);

            if (result > 0) {
                response.put("success", true);
                response.put("message", "紧急联系人已删除");
            } else {
                response.put("success", false);
                response.put("message", "删除失败");
            }

        } catch (Exception e) {
            System.err.println("❌ 删除紧急联系人失败：" + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "删除失败：" + e.getMessage());
        }

        return response;
    }

    /**
     * 获取紧急联系人的推送信息（用于一键救助）
     * @param elderId 老年人ID
     * @return 紧急联系人的推送信息列表
     */
    @GetMapping("/push-info")
    public Map<String, Object> getEmergencyContactsPushInfo(@RequestParam Integer elderId) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (elderId == null) {
                response.put("success", false);
                response.put("message", "老年人ID不能为空");
                return response;
            }

            // 查询所有监护关系
            List<Relation> relations = relationMapper.selectByElderlyId(elderId);
            List<Map<String, Object>> pushInfoList = new ArrayList<>();

            for (Relation relation : relations) {
                // 获取监护人用户信息
                User guardianUser = userMapper.selectById(relation.getGuardianId());
                if (guardianUser == null) {
                    continue;
                }

                // 只推送给有设备ID的监护人
                if (guardianUser.getPushClientId() != null && !guardianUser.getPushClientId().trim().isEmpty()) {
                    // 获取监护人详细信息
                    Guardian guardian = guardianMapper.selectOne(
                        new QueryWrapper<Guardian>().eq("user_id", guardianUser.getId())
                    );

                    Map<String, Object> pushInfo = new HashMap<>();
                    pushInfo.put("guardianId", guardianUser.getId());
                    pushInfo.put("pushClientId", guardianUser.getPushClientId());
                    pushInfo.put("name", guardian != null ? guardian.getName() : guardianUser.getPhone());
                    pushInfo.put("phone", guardianUser.getPhone());
                    pushInfo.put("remarks", relation.getRemarks());

                    pushInfoList.add(pushInfo);
                }
            }

            response.put("success", true);
            response.put("pushInfoList", pushInfoList);
            response.put("count", pushInfoList.size());

            System.out.println("✅ 获取到 " + pushInfoList.size() + " 个可推送的紧急联系人");

        } catch (Exception e) {
            System.err.println("❌ 获取紧急联系人推送信息失败：" + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "获取推送信息失败：" + e.getMessage());
        }

        return response;
    }
}

