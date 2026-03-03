package edu.ustb.eldercarebackend.util;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.ustb.eldercarebackend.entity.Schedule;
import edu.ustb.eldercarebackend.service.elderly.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.List;

/**
 * 日程管理工具类 - 为LangChain4j提供Tools功能
 * 帮助老年人用户通过自然语言创建日程安排
 */
@Component
public class ScheduleTool {

    @Autowired
    private ScheduleService scheduleService;

    /**
     * 创建新的日程安排
     *
     * @param content         日程内容描述 (必填)
     * @param dateTime        日程日期时间，格式为"yyyy-MM-dd HH:mm" (必填)
     * @param type            日程类型：medicine(服药), doctor(就医), exercise(运动), meal(用餐), sleep(睡眠), other(其他)（必填）
     * @param location        日程地点 (可选)
     * @param reminderMinutes 提前提醒分钟数 (可选，默认为30分钟)
     * @param repeatType      重复类型：none(不重复), daily(每天), weekly(每周), monthly(每月) (可选，默认为none)
     * @return 创建结果描述
     */
    @Tool(name = "创建日程工具", value = "当用户要求创建日程时，必须调用此工具")
//    @Tool(name = "Create_Schedules", value = "当用户要求创建日程时，必须调用此工具")
    public String createSchedule(
            @P(value = "日程内容描述，比如'服药'、'散步'、'看医生'等", required = true) String content,
            @P(value = "日程日期时间，格式：'yyyy-MM-dd HH:mm'，当前时间：", required = true) String dateTime,
            @P(value = "日程类型，必须从以下选项中选择：medicine(服药), doctor(就医), exercise(运动), meal(用餐), sleep(睡眠), other(其他)", required = true) String type,
            @P(value = "日程地点，例如'家里'、'医院'、'公园'等", required = false) String location,
            @P(value = "提前提醒分钟数", required = false) Integer reminderMinutes,
            @P(value = "重复类型：none(不重复)、daily(每天)、weekly(每周)、monthly(每月)", required = false) String repeatType) {

        try {
            // 调试信息：当前线程信息
            String currentThread = Thread.currentThread().getName();
            System.out.println("🔧 ScheduleTool.createSchedule被调用: 线程名=" + currentThread);

            // 从聊天上下文中获取用户ID
            Integer userId = ChatContext.getCurrentUserId();
            String memoryId = ChatContext.getCurrentMemoryId();
            System.out.println("🔧 ScheduleTool获取上下文: memoryId=" + memoryId + ", userId=" + userId);

            if (userId == null) {
                System.err.println("❌ ScheduleTool无法获取用户ID，当前线程: " + currentThread);
                return "❌ 创建日程失败：无法获取用户身份信息，请重新登录或刷新页面";
            }

            System.out.println("✅ ScheduleTool成功获取用户ID: " + userId);

            if (content == null || content.trim().isEmpty()) {
                return "❌ 创建日程失败：请提供日程内容描述";
            }

            if (dateTime == null || dateTime.trim().isEmpty()) {
                return "❌ 创建日程失败：请提供日程时间，格式为'yyyy-MM-dd HH:mm'";
            }
            
            // 解析日期时间（标准格式）
            Date scheduleTime;
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                sdf.setLenient(false); // 严格模式
                scheduleTime = sdf.parse(dateTime.trim());

                // 检查时间是否在过去
                if (scheduleTime.before(new Date())) {
                    return "❌ 创建日程失败：不能为过去的时间创建日程，请选择未来的时间";
                }
            } catch (Exception e) {
                System.err.println("❌ 时间解析失败: " + e.getMessage());
                return "❌ 创建日程失败：时间格式不正确，请使用标准格式：'yyyy-MM-dd HH:mm'，例如'2025-01-16 14:30'";
            }

            // 创建日程对象
            Schedule schedule = new Schedule();
            schedule.setUserId(userId);
            schedule.setContent(content.trim());
            schedule.setTime(scheduleTime);

            // 设置可选参数的默认值
            schedule.setLocation(location != null ? location.trim() : null);
            
            // 验证并设置日程类型
            String validatedType = validateScheduleType(type);
            schedule.setType(validatedType);

            // 设置提醒时间
            int reminderMin = 0;
            if(reminderMinutes == null || reminderMinutes <= 0){
                schedule.setReminderTime(null);
            }
            else{
                reminderMin = reminderMinutes;
                Calendar reminderCal = Calendar.getInstance();
                reminderCal.setTime(scheduleTime);
                reminderCal.add(Calendar.MINUTE, -reminderMin);
                schedule.setReminderTime(reminderCal.getTime());
            }

            // 设置重复类型
            String repeat = (repeatType != null) ? repeatType.trim().toLowerCase() : "none";
            if (!repeat.equals("none") && !repeat.equals("daily") &&
                    !repeat.equals("weekly") && !repeat.equals("monthly")) {
                repeat = "none";
            }
            schedule.setRepeatType(repeat);

            // 调用服务创建日程
            boolean success = scheduleService.createSchedule(schedule);

            if (success) {
                // 格式化返回信息
                SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
                String formattedTime = displayFormat.format(scheduleTime);

                StringBuilder result = new StringBuilder();
                result.append("✅ 日程创建成功！\n");
                result.append("📅 时间：").append(formattedTime).append("\n");
                result.append("📝 内容：").append(content).append("\n");

                if (location != null && !location.trim().isEmpty()) {
                    result.append("📍 地点：").append(location).append("\n");
                }

                result.append("🏷️ 类型：").append(getTypeDisplayName(schedule.getType())).append("\n");
                result.append("⏰ 提醒：提前").append(reminderMin).append("分钟\n");

                if (!"none".equals(repeat)) {
                    result.append("🔄 重复：").append(getRepeatDisplayName(repeat));
                }

                return result.toString();
            } else {
                return "❌ 创建日程失败：系统内部错误，请稍后再试";
            }

        } catch (Exception e) {
            System.err.println("创建日程工具调用失败: " + e.getMessage());
            e.printStackTrace();
            return "❌ 创建日程失败：系统出现异常，请联系技术支持";
        }
    }
    
    /**
     * 查询用户未来的日程安排
     * 
     * @return 未来日程列表的描述
     */
    @Tool(name = "查询未来日程工具", value = "当用户询问未来日程、下一个日程、明天有什么安排等问题时，调用此工具")
//    @Tool(name = "query_future_schedules", value = "当用户询问未来日程、下一个日程、明天有什么安排等问题时，调用此工具")
    public String queryUpcomingSchedules() {
        try {
            // 调试信息：当前线程信息
            String currentThread = Thread.currentThread().getName();
            System.out.println("🔍 ScheduleTool.queryUpcomingSchedules被调用: 线程名=" + currentThread);

            // 从聊天上下文中获取用户ID
            Integer userId = ChatContext.getCurrentUserId();
            String memoryId = ChatContext.getCurrentMemoryId();
            System.out.println("🔍 ScheduleTool获取上下文: memoryId=" + memoryId + ", userId=" + userId);

            if (userId == null) {
                System.err.println("❌ ScheduleTool无法获取用户ID，当前线程: " + currentThread);
                return "❌ 查询日程失败：无法获取用户身份信息，请重新登录或刷新页面";
            }

            System.out.println("✅ ScheduleTool成功获取用户ID: " + userId);

            // 查询未来10条日程
            List<Schedule> upcomingSchedules = scheduleService.getUpcomingSchedulesByUserId(userId, 10);
            
            if (upcomingSchedules == null || upcomingSchedules.isEmpty()) {
                return "📅 您目前没有安排未来的日程，可以通过语音告诉我来帮您创建新的日程安排哦！";
            }

            // 格式化输出日程列表 - 包含ID信息但不展示给用户
            StringBuilder result = new StringBuilder();
            
            SimpleDateFormat displayFormat = new SimpleDateFormat("MM月dd日 HH:mm");
            
            // === 用户可见部分（开始） ===
            result.append("📅 未来日程安排【此为工具返回结果，不是聊天记录，务必再次输出向用户展示】：\n\n");
            
            for (int i = 0; i < upcomingSchedules.size(); i++) {
                Schedule schedule = upcomingSchedules.get(i);
                
                // 用户可见的格式化内容
                result.append(String.format("%d. ", i + 1));
                
                // 时间
                String formattedTime = displayFormat.format(schedule.getTime());
                result.append(formattedTime).append(" ");
                
                // 类型标识
                result.append(getTypeDisplayName(schedule.getType())).append(" ");
                
                // 内容
                result.append(schedule.getContent());
                
                // 地点（如果有）
                if (schedule.getLocation() != null && !schedule.getLocation().trim().isEmpty()) {
                    result.append(" (").append(schedule.getLocation()).append(")");
                }
                
                result.append("\n");
            }
            
            result.append("\n如需修改或添加新的日程，请随时告诉我！");
            // === 用户可见部分（结束） ===
            
            // === 内部ID映射（仅供工具使用，不展示给用户） ===
            result.append("\n\n[内部ID映射-仅供修改工具使用:");
            for (int i = 0; i < upcomingSchedules.size(); i++) {
                result.append(" ").append(i + 1).append("->").append(upcomingSchedules.get(i).getId());
            }
            result.append("]");
            
            return result.toString();
            
        } catch (Exception e) {
            System.err.println("查询日程工具调用失败: " + e.getMessage());
            e.printStackTrace();
            return "❌ 查询日程失败：系统出现异常，请稍后再试";
        }
    }
    
    /**
     * 修改现有的日程安排
     *
     * @param scheduleId      日程ID（从查询工具的内部ID映射中获取）(必填)
     * @param newDateTime     新的日期时间，格式为"yyyy-MM-dd HH:mm" (可选)
     * @param newContent      新的日程内容描述 (可选)
     * @param newType         新的日程类型 (可选)
     * @param newLocation     新的日程地点 (可选)
     * @return 修改结果描述
     */
    @Tool(name = "修改日程工具", value = "当用户要求修改已存在的日程时调用此工具")
//    @Tool(name = "odifying_schedules", value = "当用户要求修改已存在的日程时调用此工具")
    public String modifySchedule(
            @P(value = "要修改的日程ID，从查询工具的内部ID映射中获取", required = true) Integer scheduleId,
            @P(value = "新的日期时间，格式：'yyyy-MM-dd HH:mm'，如不修改时间则传null", required = false) String newDateTime,
            @P(value = "新的日程内容描述，如不修改内容则传null", required = false) String newContent,
            @P(value = "新的日程类型：medicine(服药), doctor(就医), exercise(运动), meal(用餐), sleep(睡眠), other(其他)，如不修改类型则传null", required = false) String newType,
            @P(value = "新的日程地点，如不修改地点则传null", required = false) String newLocation) {

        try {
            // 调试信息：当前线程信息
            String currentThread = Thread.currentThread().getName();
            System.out.println("🔧 ScheduleTool.modifySchedule被调用: 线程名=" + currentThread);

            // 从聊天上下文中获取用户ID
            Integer userId = ChatContext.getCurrentUserId();
            String memoryId = ChatContext.getCurrentMemoryId();
            System.out.println("🔧 ScheduleTool获取上下文: memoryId=" + memoryId + ", userId=" + userId);

            if (userId == null) {
                System.err.println("❌ ScheduleTool无法获取用户ID，当前线程: " + currentThread);
                return "❌ 修改日程失败：无法获取用户身份信息，请重新登录或刷新页面";
            }

            if (scheduleId == null) {
                return "❌ 修改日程失败：请提供要修改的日程ID";
            }

            System.out.println("✅ ScheduleTool成功获取用户ID: " + userId + ", 日程ID: " + scheduleId);

            // 首先检查日程是否存在且属于当前用户
            Schedule existingSchedule = scheduleService.getScheduleById(scheduleId);
            if (existingSchedule == null) {
                return "❌ 修改日程失败：找不到指定的日程";
            }
            
            if (!existingSchedule.getUserId().equals(userId)) {
                return "❌ 修改日程失败：您没有权限修改此日程";
            }

            // 创建要更新的日程对象
            Schedule scheduleToUpdate = new Schedule();
            scheduleToUpdate.setId(scheduleId);
            
            StringBuilder changesSummary = new StringBuilder();
            changesSummary.append("✅ 日程修改成功！以下是更新的内容：\n");
            
            // 处理时间字段更新
            if (newDateTime != null && !newDateTime.trim().isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    sdf.setLenient(false); // 严格模式
                    Date newScheduleTime = sdf.parse(newDateTime.trim());
                    
                    // 检查时间是否在过去
                    if (newScheduleTime.before(new Date())) {
                        return "❌ 修改日程失败：不能将日程时间设置为过去的时间";
                    }
                    
                    scheduleToUpdate.setTime(newScheduleTime);
                    SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
                    changesSummary.append("📅 时间：").append(displayFormat.format(newScheduleTime)).append("\n");
                } catch (Exception e) {
                    System.err.println("❌ 时间解析失败: " + e.getMessage());
                    return "❌ 修改日程失败：时间格式不正确，请使用标准格式：'yyyy-MM-dd HH:mm'";
                }
            }
            
            // 处理内容更新
            if (newContent != null && !newContent.trim().isEmpty()) {
                scheduleToUpdate.setContent(newContent.trim());
                changesSummary.append("📝 内容：").append(newContent.trim()).append("\n");
            }
            
            // 处理类型更新
            if (newType != null && !newType.trim().isEmpty()) {
                String validatedType = validateScheduleType(newType);
                scheduleToUpdate.setType(validatedType);
                changesSummary.append("🏷️ 类型：").append(getTypeDisplayName(validatedType)).append("\n");
            }
            
            // 处理地点更新
            if (newLocation != null && !newLocation.trim().isEmpty()) {
                scheduleToUpdate.setLocation(newLocation.trim());
                changesSummary.append("📍 地点：").append(newLocation.trim()).append("\n");
            }
            
            // 检查是否有任何字段需要更新
            if (scheduleToUpdate.getTime() == null && 
                scheduleToUpdate.getContent() == null && 
                scheduleToUpdate.getType() == null && 
                scheduleToUpdate.getLocation() == null) {
                return "❌ 修改日程失败：请至少指定一个要修改的字段（时间、内容、类型或地点）";
            }

            // 调用服务更新日程
            boolean success = scheduleService.updateSchedule(scheduleToUpdate);

            if (success) {
                changesSummary.append("\n💡 如需进一步修改，请随时告诉我！");
                return changesSummary.toString();
            } else {
                return "❌ 修改日程失败：系统内部错误，请稍后再试";
            }

        } catch (Exception e) {
            System.err.println("修改日程工具调用失败: " + e.getMessage());
            e.printStackTrace();
            return "❌ 修改日程失败：系统出现异常，请联系技术支持";
        }
    }

    /**
     * 验证日程类型是否符合数据库枚举值
     */
    private String validateScheduleType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return "other";
        }
        
        String trimmedType = type.trim().toLowerCase();
        // 数据库枚举值：'medicine','doctor','exercise','meal','sleep','other'
        switch (trimmedType) {
            case "medicine":
            case "doctor":  
            case "exercise":
            case "meal":
            case "sleep":
            case "other":
                return trimmedType;
            default:
                System.out.println("⚠️ 无效的日程类型: " + type + "，已自动设置为 'other'");
                return "other";
        }
    }

    /**
     * 获取日程类型的显示名称
     */
    private String getTypeDisplayName(String type) {
        switch (type.toLowerCase()) {
            case "medicine":
                return "服药";
            case "doctor":
                return "就医";
            case "exercise":
                return "运动";
            case "meal":
                return "用餐";
            case "sleep":
                return "睡眠";
            case "other":
            default:
                return "其他";
        }
    }

    /**
     * 获取重复类型的显示名称
     */
    private String getRepeatDisplayName(String repeatType) {
        switch (repeatType.toLowerCase()) {
            case "daily":
                return "每天";
            case "weekly":
                return "每周";
            case "monthly":
                return "每月";
            default:
                return "不重复";
        }
    }
}