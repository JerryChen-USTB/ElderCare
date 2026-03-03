package edu.ustb.eldercarebackend.service.elderly.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import edu.ustb.eldercarebackend.entity.Assistance;
import edu.ustb.eldercarebackend.mapper.AssistanceMapper;
import edu.ustb.eldercarebackend.service.elderly.AssistanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 远程协助服务实现类
 */
@Service
public class AssistanceServiceImpl implements AssistanceService {

    @Autowired
    private AssistanceMapper assistanceMapper;

    @Override
    public Assistance getActiveAssistanceByElderId(Integer elderId) {
        return assistanceMapper.findActiveAssistanceByElderId(elderId);
    }

    @Override
    public Assistance submitAssistanceRequest(Integer elderId, String content) {
        try {
            // 检查是否已有活跃的协助项目
            Assistance existingAssistance = getActiveAssistanceByElderId(elderId);
            if (existingAssistance != null) {
                throw new RuntimeException("用户已有活跃的协助申请，无法重复提交");
            }

            // 创建新的协助申请
            Assistance assistance = new Assistance();
            assistance.setElderId(elderId);
            assistance.setAppointmentContent(content);
            assistance.setStatus("waiting_response");
            assistance.setApplyTime(new Date());
            assistance.setCreatedAt(new Date());
            assistance.setUpdatedAt(new Date());

            int result = assistanceMapper.insert(assistance);
            if (result > 0) {
                return assistance;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("提交协助申请失败: " + e.getMessage());
        }
    }

    @Override
    public Assistance updateAssistanceStatus(Integer assistanceId, String status) {
        try {
            UpdateWrapper<Assistance> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", assistanceId);
            updateWrapper.set("status", status);
            updateWrapper.set("updated_at", new Date());

            int result = assistanceMapper.update(null, updateWrapper);
            if (result > 0) {
                return assistanceMapper.selectById(assistanceId);
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("更新协助状态失败: " + e.getMessage());
        }
    }

    @Override
    public Assistance startCall(Integer assistanceId) {
        try {
            Assistance assistance = assistanceMapper.selectById(assistanceId);
            if (assistance == null) {
                throw new RuntimeException("协助项目不存在");
            }
            
            // 允许从 waiting_call 或 waiting_call_again 状态发起呼叫
            if (!"waiting_call".equals(assistance.getStatus()) && 
                !"waiting_call_again".equals(assistance.getStatus())) {
                throw new RuntimeException("当前状态不允许发起呼叫");
            }

            return updateAssistanceStatus(assistanceId, "calling");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("发起呼叫失败: " + e.getMessage());
        }
    }

    @Override
    public Assistance callFailed(Integer assistanceId) {
        // 默认回到 waiting_call 状态，保持向后兼容
        return callFailedWithStatus(assistanceId, "waiting_call");
    }

    @Override
    public Assistance callFailedWithStatus(Integer assistanceId, String targetStatus) {
        try {
            Assistance assistance = assistanceMapper.selectById(assistanceId);
            if (assistance == null) {
                throw new RuntimeException("协助项目不存在");
            }
            
            System.out.println("当前状态: " + assistance.getStatus());
            if (!"calling".equals(assistance.getStatus())) {
                throw new RuntimeException("当前状态不是呼叫中");
            }

            // 验证目标状态是否有效
            if (!"waiting_call".equals(targetStatus) && !"waiting_call_again".equals(targetStatus)) {
                throw new RuntimeException("无效的目标状态: " + targetStatus);
            }

            return updateAssistanceStatus(assistanceId, targetStatus);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("处理呼叫失败失败: " + e.getMessage());
        }
    }

    @Override
    public Assistance startAssistance(Integer assistanceId) {
        try {
            Assistance assistance = assistanceMapper.selectById(assistanceId);
            if (assistance == null) {
                throw new RuntimeException("协助项目不存在");
            }
            
            if (!"calling".equals(assistance.getStatus())) {
                throw new RuntimeException("当前状态不允许开始协助");
            }

            UpdateWrapper<Assistance> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", assistanceId);
            updateWrapper.set("status", "in_progress");
            updateWrapper.set("start_time", new Date());
            updateWrapper.set("updated_at", new Date());

            int result = assistanceMapper.update(null, updateWrapper);
            if (result > 0) {
                return assistanceMapper.selectById(assistanceId);
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("开始协助失败: " + e.getMessage());
        }
    }

    @Override
    public Assistance endAssistance(Integer assistanceId) {
        try {
            Assistance assistance = assistanceMapper.selectById(assistanceId);
            if (assistance == null) {
                throw new RuntimeException("协助项目不存在");
            }
            
            // 只有在 waiting_call_again 状态下才能完成协助服务
            // in_progress 状态只能结束通话，不能直接完成协助
            if (!"waiting_call_again".equals(assistance.getStatus())) {
                throw new RuntimeException("当前状态不允许完成协助，请先结束通话");
            }

            UpdateWrapper<Assistance> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", assistanceId);
            updateWrapper.set("status", "completed");
            updateWrapper.set("end_time", new Date());
            updateWrapper.set("updated_at", new Date());

            int result = assistanceMapper.update(null, updateWrapper);
            if (result > 0) {
                return assistanceMapper.selectById(assistanceId);
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("完成协助失败: " + e.getMessage());
        }
    }

    @Override
    public Assistance cancelAssistance(Integer assistanceId) {
        try {
            Assistance assistance = assistanceMapper.selectById(assistanceId);
            if (assistance == null) {
                throw new RuntimeException("协助项目不存在");
            }
            
            // 只有特定状态才能取消
            if (!("waiting_response".equals(assistance.getStatus()) || 
                  "waiting_call".equals(assistance.getStatus()))) {
                throw new RuntimeException("当前状态不允许取消");
            }

            return updateAssistanceStatus(assistanceId, "cancelled");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("取消协助失败: " + e.getMessage());
        }
    }

    @Override
    public Assistance updateToWaitingCallAgain(Integer assistanceId) {
        try {
            Assistance assistance = assistanceMapper.selectById(assistanceId);
            if (assistance == null) {
                throw new RuntimeException("协助项目不存在");
            }
            
            // 检查当前状态是否允许更新为 waiting_call_again
            // 通常这个状态转换发生在 in_progress 之后
            if (!"in_progress".equals(assistance.getStatus())) {
                throw new RuntimeException("当前状态不允许更新为可再次呼叫状态");
            }

            return updateAssistanceStatus(assistanceId, "waiting_call_again");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("更新状态失败: " + e.getMessage());
        }
    }

    @Override
    public AssistanceDetailVO getAssistanceDetailByElderId(Integer elderId) {
        try {
            // 首先尝试获取活跃的协助项目
            Assistance assistance = getActiveAssistanceByElderId(elderId);
            
            // 如果没有活跃项目，获取最新的协助记录（包括completed和cancelled）
            if (assistance == null) {
                assistance = assistanceMapper.findLatestAssistanceByElderId(elderId);
                
                // 如果最新记录不是completed或cancelled状态，则不返回
                // 只有当最新记录是completed或cancelled时才显示，其他情况返回null
                if (assistance != null && 
                    !"completed".equals(assistance.getStatus()) && 
                    !"cancelled".equals(assistance.getStatus())) {
                    return null;
                }
            }
            
            if (assistance == null) {
                return null;
            }

            String volunteerName = null;
            if (assistance.getVolunteerId() != null) {
                volunteerName = assistanceMapper.getVolunteerNameById(assistance.getVolunteerId());
            }

            return new AssistanceDetailVO(assistance, volunteerName);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("获取协助详情失败: " + e.getMessage());
        }
    }

    @Override
    public boolean updateAssistanceContent(Integer elderId, String content) {
        try {
            // 获取用户当前活跃的协助项目
            Assistance assistance = getActiveAssistanceByElderId(elderId);
            if (assistance == null) {
                throw new RuntimeException("没有找到活跃的协助项目");
            }

            // 更新协助内容
            assistance.setAppointmentContent(content.trim());
            assistance.setUpdatedAt(new java.util.Date());
            
            // 保存到数据库
            int result = assistanceMapper.updateById(assistance);
            return result > 0;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("更新协助内容失败: " + e.getMessage());
        }
    }
}
