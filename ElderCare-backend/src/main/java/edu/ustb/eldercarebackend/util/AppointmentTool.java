package edu.ustb.eldercarebackend.util;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.ustb.eldercarebackend.entity.Appointment;
import edu.ustb.eldercarebackend.service.elderly.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 预约志愿者服务工具类 - 为LangChain4j提供Tools功能
 * 帮助老年人用户通过自然语言预约志愿者服务
 */
@Component
public class AppointmentTool {

    @Autowired
    private AppointmentService appointmentService;

    /**
     * 创建志愿者服务预约
     *
     * @param appointmentType    预约类型：doctor(就医陪同), nurse(护理服务), rehab(康复陪护), therapy(理疗陪同), other(其他服务) (必填)
     * @param appointmentContent 服务内容描述 (必填)
     * @param startDateTime      预约开始日期时间，格式为"yyyy-MM-dd HH:mm" (必填)
     * @param endDateTime        预约结束日期时间，格式为"yyyy-MM-dd HH:mm" (可选)
     * @param location           预约地点描述 (可选)
     * @return 预约创建结果描述
     */
     @Tool(name = "预约志愿者服务工具", value = "当用户要求预约志愿者服务时，必须调用此工具")
//    @Tool(name = "Booking_volunteer_service", value = "当用户要求预约志愿者服务时，必须调用此工具")
    public String createAppointment(
            @P(value = "预约类型，必须从以下选项中选择：doctor(医生问诊), nurse(护理服务), rehab(康复指导), therapy(心理治疗), other(其他服务)", required = true) String appointmentType,
            @P(value = "服务内容描述，详细说明需要志愿者提供的具体服务", required = true) String appointmentContent,
            @P(value = "预约开始日期时间，格式：'yyyy-MM-dd HH:mm'", required = true) String startDateTime,
            @P(value = "预约结束日期时间，格式：'yyyy-MM-dd HH:mm'，如果不确定结束时间可以不填", required = false) String endDateTime,
            @P(value = "预约地点描述，比如具体的医院名称、地址等，如果不确定地点可以不填", required = false) String location) {

        try {
            // 调试信息：当前线程信息
            String currentThread = Thread.currentThread().getName();
            System.out.println("🔧 AppointmentTool.createAppointment被调用: 线程名=" + currentThread);

            // 从聊天上下文中获取用户ID
            Integer userId = ChatContext.getCurrentUserId();
            String memoryId = ChatContext.getCurrentMemoryId();
            System.out.println("🔧 AppointmentTool获取上下文: memoryId=" + memoryId + ", userId=" + userId);

            if (userId == null) {
                System.err.println("❌ AppointmentTool无法获取用户ID，当前线程: " + currentThread);
                return "❌ 创建预约失败：无法获取用户身份信息，请重新登录或刷新页面";
            }

            System.out.println("✅ AppointmentTool成功获取用户ID: " + userId);

            // 验证必填参数
            if (appointmentType == null || appointmentType.trim().isEmpty()) {
                return "❌ 创建预约失败：请选择预约类型（就医陪同、护理服务、康复陪护、理疗陪同或其他服务）";
            }

            if (appointmentContent == null || appointmentContent.trim().isEmpty()) {
                return "❌ 创建预约失败：请详细描述您需要的服务内容";
            }

            if (startDateTime == null || startDateTime.trim().isEmpty()) {
                return "❌ 创建预约失败：请提供预约开始时间，格式为'yyyy-MM-dd HH:mm'";
            }

            // 验证并转换预约类型
            String validatedType = validateAppointmentType(appointmentType);
            if (validatedType == null) {
                return "❌ 创建预约失败：无效的预约类型，请选择：就医陪同(doctor)、护理服务(nurse)、康复陪护(rehab)、理疗陪同(therapy)或其他服务(other)";
            }

            // 解析开始时间
            Date startTime;
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                sdf.setLenient(false); // 严格模式
                startTime = sdf.parse(startDateTime.trim());

                // 检查时间是否在过去
                if (startTime.before(new Date())) {
                    return "❌ 创建预约失败：不能为过去的时间创建预约，请选择未来的时间";
                }
            } catch (Exception e) {
                System.err.println("❌ 开始时间解析失败: " + e.getMessage());
                return "❌ 创建预约失败：开始时间格式不正确，请使用标准格式：'yyyy-MM-dd HH:mm'，例如'2025-01-16 14:30'";
            }

            // 解析结束时间（可选）
            Date endTime = null;
            if (endDateTime != null && !endDateTime.trim().isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    sdf.setLenient(false); // 严格模式
                    endTime = sdf.parse(endDateTime.trim());

                    // 检查结束时间是否在开始时间之后
                    if (endTime.before(startTime) || endTime.equals(startTime)) {
                        return "❌ 创建预约失败：结束时间必须晚于开始时间";
                    }

                    // 检查服务时长是否合理（不超过8小时）
                    long timeDiff = endTime.getTime() - startTime.getTime();
                    long hoursDiff = timeDiff / (1000 * 60 * 60);
                    if (hoursDiff > 8) {
                        return "❌ 创建预约失败：单次服务时长不能超过8小时，如需更长时间请分多次预约";
                    }
                } catch (Exception e) {
                    System.err.println("❌ 结束时间解析失败: " + e.getMessage());
                    return "❌ 创建预约失败：结束时间格式不正确，请使用标准格式：'yyyy-MM-dd HH:mm'";
                }
            }

            // 处理地点信息（可选）
            String processedLocation = null;
            if (location != null && !location.trim().isEmpty()) {
                processedLocation = location.trim();
                // 验证地点信息长度
                if (processedLocation.length() > 255) {
                    return "❌ 创建预约失败：地点描述不能超过255个字符";
                }
            }

            // 创建预约对象
            Appointment appointment = new Appointment();
            appointment.setElderId(userId); // 设置老人ID
            appointment.setAppointmentType(validatedType);
            appointment.setAppointmentContent(appointmentContent.trim());
            appointment.setStartTime(startTime);
            appointment.setEndTime(endTime);
            appointment.setLocation(processedLocation); // 设置地点信息
            appointment.setStatus("pending"); // 默认状态为待接单
            appointment.setCreatedAt(new Date());
            appointment.setUpdatedAt(new Date());
            // volunteerId 暂时为null，等待志愿者接单

            // 验证预约数据
            String validationResult = appointmentService.validateAppointment(appointment);
            if (validationResult != null) {
                return "❌ 创建预约失败：" + validationResult;
            }

            // 调用服务创建预约
            Appointment createdAppointment = appointmentService.createAppointment(appointment);

            if (createdAppointment != null) {
                // 格式化返回信息
                SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
                String formattedStartTime = displayFormat.format(startTime);

                StringBuilder result = new StringBuilder();
                result.append("✅ 志愿者服务预约创建成功！\n\n");
                result.append("📋 预约详情：\n");
                result.append("🏷️ 服务类型：").append(getTypeDisplayName(validatedType)).append("\n");
                result.append("📝 服务内容：").append(appointmentContent).append("\n");
                result.append("📅 开始时间：").append(formattedStartTime).append("\n");

                if (endTime != null) {
                    String formattedEndTime = displayFormat.format(endTime);
                    result.append("⏰ 结束时间：").append(formattedEndTime).append("\n");
                }

                if (processedLocation != null) {
                    result.append("📍 服务地点：").append(processedLocation).append("\n");
                }

                result.append("📊 预约状态：等待志愿者接单\n");
                result.append("🆔 预约编号：").append(createdAppointment.getId()).append("\n\n");
                result.append("💡 温馨提示：\n");
                result.append("• 预约已提交，系统会通知合适的志愿者\n");
                result.append("• 志愿者接单后会联系您确认具体服务细节\n");
                result.append("• 您可以在「我的」-「预约管理」中查看预约状态");

                return result.toString();
            } else {
                return "❌ 创建预约失败：系统内部错误，请稍后再试或联系客服";
            }

        } catch (Exception e) {
            System.err.println("预约志愿者服务工具调用失败: " + e.getMessage());
            e.printStackTrace();
            return "❌ 创建预约失败：系统出现异常，请稍后再试或联系技术支持";
        }
    }

    /**
     * 验证预约类型是否符合枚举值
     */
    private String validateAppointmentType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return null;
        }

        String trimmedType = type.trim().toLowerCase();
        // 数据库枚举值：'doctor','nurse','rehab','therapy','other'
        switch (trimmedType) {
            case "doctor":
            case "就医陪同":
            case "就医":
                return "doctor";
            case "nurse":
            case "护理服务":
            case "护理":
                return "nurse";
            case "rehab":
            case "康复陪护":
            case "康复":
                return "rehab";
            case "therapy":
            case "理疗陪同":
            case "理疗":
                return "therapy";
            case "other":
            case "其他服务":
            case "其他":
                return "other";
            default:
                // 尝试根据关键词匹配
                if (trimmedType.contains("医") || trimmedType.contains("看病") || trimmedType.contains("就诊")) {
                    return "doctor";
                } else if (trimmedType.contains("护理") || trimmedType.contains("照护")) {
                    return "nurse";
                } else if (trimmedType.contains("康复") || trimmedType.contains("康复训练")) {
                    return "rehab";
                } else if (trimmedType.contains("理疗") || trimmedType.contains("按摩") || trimmedType.contains("推拿")) {
                    return "therapy";
                } else {
                    return null; // 无法识别的类型
                }
        }
    }

    /**
     * 获取预约类型的显示名称
     */
    private String getTypeDisplayName(String type) {
        switch (type.toLowerCase()) {
            case "doctor":
                return "就医陪同";
            case "nurse":
                return "护理服务";
            case "rehab":
                return "康复陪护";
            case "therapy":
                return "理疗陪同";
            case "other":
            default:
                return "其他服务";
        }
    }
}
