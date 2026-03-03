package edu.ustb.eldercarebackend.service.elderly;

import edu.ustb.eldercarebackend.entity.Assistance;

/**
 * 远程协助服务接口
 */
public interface AssistanceService {

    /**
     * 根据老人ID获取活跃的协助项目
     * @param elderId 老人ID
     * @return 活跃的协助项目
     */
    Assistance getActiveAssistanceByElderId(Integer elderId);

    /**
     * 提交远程协助申请
     * @param elderId 老人ID
     * @param content 协助内容
     * @return 创建的协助项目
     */
    Assistance submitAssistanceRequest(Integer elderId, String content);

    /**
     * 更新协助状态
     * @param assistanceId 协助ID
     * @param status 新状态
     * @return 更新后的协助项目
     */
    Assistance updateAssistanceStatus(Integer assistanceId, String status);

    /**
     * 开始呼叫
     * @param assistanceId 协助ID
     * @return 更新后的协助项目
     */
    Assistance startCall(Integer assistanceId);

    /**
     * 呼叫失败，返回waiting_call状态
     * @param assistanceId 协助ID
     * @return 更新后的协助项目
     */
    Assistance callFailed(Integer assistanceId);

    /**
     * 呼叫失败，返回指定状态
     * @param assistanceId 协助ID
     * @param targetStatus 目标状态（waiting_call 或 waiting_call_again）
     * @return 更新后的协助项目
     */
    Assistance callFailedWithStatus(Integer assistanceId, String targetStatus);

    /**
     * 开始协助服务
     * @param assistanceId 协助ID
     * @return 更新后的协助项目
     */
    Assistance startAssistance(Integer assistanceId);

    /**
     * 结束协助服务
     * @param assistanceId 协助ID
     * @return 更新后的协助项目
     */
    Assistance endAssistance(Integer assistanceId);

    /**
     * 取消协助服务
     * @param assistanceId 协助ID
     * @return 更新后的协助项目
     */
    Assistance cancelAssistance(Integer assistanceId);

    /**
     * 更新状态为 waiting_call_again（通话结束后可再次呼叫）
     * @param assistanceId 协助ID
     * @return 更新后的协助项目
     */
    Assistance updateToWaitingCallAgain(Integer assistanceId);

    /**
     * 获取完整的协助信息（包含志愿者姓名）
     * @param elderId 老人ID
     * @return 包含志愿者信息的协助详情
     */
    AssistanceDetailVO getAssistanceDetailByElderId(Integer elderId);
    
    /**
     * 更新协助内容
     * @param elderId 老人ID
     * @param content 新的协助内容
     * @return 是否更新成功
     */
    boolean updateAssistanceContent(Integer elderId, String content);

    /**
     * 协助详情VO类
     */
    class AssistanceDetailVO {
        private Assistance assistance;
        private String volunteerName;
        private String volunteerNumber;
        private String location;
        private Long waitingMinutes;
        private String responseTime;

        // 构造函数
        public AssistanceDetailVO(Assistance assistance, String volunteerName) {
            this.assistance = assistance;
            this.volunteerName = volunteerName != null ? volunteerName : "暂未匹配志愿者";
            this.volunteerNumber = assistance.getVolunteerId() != null ? "V" + assistance.getVolunteerId() : null;
            this.location = "无法获取定位";
            
            // 计算等待时间
            if (assistance.getApplyTime() != null && assistance.getStartTime() != null) {
                this.waitingMinutes = (assistance.getStartTime().getTime() - assistance.getApplyTime().getTime()) / (1000 * 60);
            }
            
            // 设置应答时间（志愿者接单时间，这里暂时使用startTime作为示例）
            this.responseTime = assistance.getStartTime() != null ? 
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(assistance.getStartTime()) : null;
        }

        // Getters and Setters
        public Assistance getAssistance() { return assistance; }
        public void setAssistance(Assistance assistance) { this.assistance = assistance; }
        
        public String getVolunteerName() { return volunteerName; }
        public void setVolunteerName(String volunteerName) { this.volunteerName = volunteerName; }
        
        public String getVolunteerNumber() { return volunteerNumber; }
        public void setVolunteerNumber(String volunteerNumber) { this.volunteerNumber = volunteerNumber; }
        
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        
        public Long getWaitingMinutes() { return waitingMinutes; }
        public void setWaitingMinutes(Long waitingMinutes) { this.waitingMinutes = waitingMinutes; }
        
        public String getResponseTime() { return responseTime; }
        public void setResponseTime(String responseTime) { this.responseTime = responseTime; }
    }
}
