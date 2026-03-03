package edu.ustb.eldercarebackend.controller.guardian;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;  // 这两行缺失会导致 Logger 类标红

import edu.ustb.eldercarebackend.entity.guardian.ResultVO;
import edu.ustb.eldercarebackend.service.guardian.RelationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guardian/relation")
public class RelationController {

    private static final Logger log = LoggerFactory.getLogger(RelationController.class);  // 若标红，检查导入语句

    @Autowired
    private RelationService relationService;
    /**
     * 发送验证码
     */
    @PostMapping("/send-code")
    public ResultVO sendVerificationCode(@RequestParam String targetPhone) {
        return relationService.sendVerificationCode(targetPhone);
    }

    /**
     * 绑定用户
     */
    @PostMapping("/addbind")
    public ResultVO bindElder(
            HttpServletRequest request,
            @RequestParam Integer guardianId,
            @RequestParam String targetPhone,
            @RequestParam String targetName,
            @RequestParam String relation,
            @RequestParam String verifyCode) {

        log.info("Controller 接收的 guardianId: {}", guardianId); // 验证是否为 null
        return relationService.bindElder(guardianId, targetPhone, targetName, relation, verifyCode
        );
    }
    /**
     * 解除绑定
     */
    @PostMapping("/unbind")
    public ResultVO unbindElder(
            @RequestParam Integer guardianId,
            @RequestParam Integer elderlyId) {

        log.info("接收解除绑定请求 - guardianId: {}, elderlyId: {}", guardianId, elderlyId);
        return relationService.unbindElder(guardianId, elderlyId);
    }
}
