/*
 ElderCare Project - Schedules Data Insert Script
 
 时间范围: 2025-10-08 至 2025-10-31
 用户ID: 16 (张爷爷)
 
 包含日程类型:
 - medicine (服药)
 - doctor (就医)
 - exercise (锻炼)
 - meal (饮食)
 - sleep (睡眠)
 - appointment (志愿者预约)
 - other (其他活动)
 
 Date: 2025-10-09
*/

SET NAMES utf8mb4;

-- ----------------------------
-- 2025年10月日程数据插入
-- ----------------------------

-- 10月8日 - 周二
INSERT INTO `schedules` (`parent_schedule_id`, `user_id`, `type`, `content`, `time`, `location`, `status`, `reminder_time`, `repeat_type`, `appointment_id`, `created_at`) VALUES
(NULL, 16, 'exercise', '晨练太极拳', '2025-10-08 06:30:00', '社区公园', 'pending', '06:15:00', 'none', NULL, '2025-10-08 00:00:00'),
(NULL, 16, 'other', '社区老年大学书法课', '2025-10-08 09:30:00', '社区活动中心', 'pending', '09:00:00', 'none', NULL, '2025-10-08 00:00:00');

-- 10月10日 - 周四
INSERT INTO `schedules` (`parent_schedule_id`, `user_id`, `type`, `content`, `time`, `location`, `status`, `reminder_time`, `repeat_type`, `appointment_id`, `created_at`) VALUES
(NULL, 16, 'medicine', '早晨服用降压药（缬沙坦片）', '2025-10-10 08:00:00', '家中', 'pending', '07:45:00', 'none', NULL, '2025-10-10 00:00:00'),
(NULL, 16, 'other', '与老友下象棋', '2025-10-10 15:00:00', '社区棋牌室', 'pending', '14:45:00', 'none', NULL, '2025-10-10 00:00:00');

-- 10月12日 - 周六
INSERT INTO `schedules` (`parent_schedule_id`, `user_id`, `type`, `content`, `time`, `location`, `status`, `reminder_time`, `repeat_type`, `appointment_id`, `created_at`) VALUES
(NULL, 16, 'meal', '周末家庭聚餐', '2025-10-12 12:00:00', '女儿家', 'pending', '11:30:00', 'none', NULL, '2025-10-12 00:00:00'),
(NULL, 16, 'other', '观看京剧表演（网络直播）', '2025-10-12 15:00:00', '家中', 'pending', '14:45:00', 'none', NULL, '2025-10-12 00:00:00');

-- 10月14日 - 周一
INSERT INTO `schedules` (`parent_schedule_id`, `user_id`, `type`, `content`, `time`, `location`, `status`, `reminder_time`, `repeat_type`, `appointment_id`, `created_at`) VALUES
(NULL, 16, 'doctor', '心内科复查心电图', '2025-10-14 10:00:00', '北京安贞医院', 'pending', '09:00:00', 'none', NULL, '2025-10-14 00:00:00'),
(NULL, 16, 'sleep', '午休时间', '2025-10-14 13:30:00', '家中卧室', 'pending', '13:15:00', 'none', NULL, '2025-10-14 00:00:00');

-- 10月15日 - 周二
INSERT INTO `schedules` (`parent_schedule_id`, `user_id`, `type`, `content`, `time`, `location`, `status`, `reminder_time`, `repeat_type`, `appointment_id`, `created_at`) VALUES
(NULL, 16, 'exercise', '老年体操课', '2025-10-15 09:00:00', '社区活动中心', 'pending', '08:45:00', 'none', NULL, '2025-10-15 00:00:00'),
(NULL, 16, 'medicine', '餐后服用钙片', '2025-10-15 12:30:00', '家中', 'pending', '12:15:00', 'none', NULL, '2025-10-15 00:00:00');

-- 10月17日 - 周四
INSERT INTO `schedules` (`parent_schedule_id`, `user_id`, `type`, `content`, `time`, `location`, `status`, `reminder_time`, `repeat_type`, `appointment_id`, `created_at`) VALUES
(NULL, 16, 'doctor', '眼科常规检查（眼压测量）', '2025-10-17 14:00:00', '同仁医院眼科', 'pending', '13:00:00', 'none', NULL, '2025-10-17 00:00:00'),
(NULL, 16, 'meal', '养生粥品（小米红枣粥）', '2025-10-17 18:00:00', '家中', 'pending', '17:45:00', 'none', NULL, '2025-10-17 00:00:00');

-- 10月19日 - 周六
INSERT INTO `schedules` (`parent_schedule_id`, `user_id`, `type`, `content`, `time`, `location`, `status`, `reminder_time`, `repeat_type`, `appointment_id`, `created_at`) VALUES
(NULL, 16, 'other', '社区义诊活动参与', '2025-10-19 09:00:00', '社区卫生服务中心', 'pending', '08:30:00', 'none', NULL, '2025-10-19 00:00:00'),
(NULL, 16, 'exercise', '散步锻炼', '2025-10-19 17:00:00', '小区花园', 'pending', '16:45:00', 'none', NULL, '2025-10-19 00:00:00');

-- 10月20日 - 周日
INSERT INTO `schedules` (`parent_schedule_id`, `user_id`, `type`, `content`, `time`, `location`, `status`, `reminder_time`, `repeat_type`, `appointment_id`, `created_at`) VALUES
(NULL, 16, 'meal', '家庭聚餐（孙子生日）', '2025-10-20 12:00:00', '儿子家', 'pending', '11:30:00', 'none', NULL, '2025-10-20 00:00:00'),
(NULL, 16, 'sleep', '下午休息', '2025-10-20 14:00:00', '家中', 'pending', '13:45:00', 'none', NULL, '2025-10-20 00:00:00');

-- 10月21日 - 周一
INSERT INTO `schedules` (`parent_schedule_id`, `user_id`, `type`, `content`, `time`, `location`, `status`, `reminder_time`, `repeat_type`, `appointment_id`, `created_at`) VALUES
(NULL, 16, 'doctor', '中医推拿理疗（腰椎保健）', '2025-10-21 10:00:00', '中医院推拿科', 'pending', '09:00:00', 'none', NULL, '2025-10-21 00:00:00'),
(NULL, 16, 'other', '社区老年电脑培训课', '2025-10-21 14:30:00', '社区活动中心', 'pending', '14:00:00', 'none', NULL, '2025-10-21 00:00:00');

-- 10月23日 - 周三
INSERT INTO `schedules` (`parent_schedule_id`, `user_id`, `type`, `content`, `time`, `location`, `status`, `reminder_time`, `repeat_type`, `appointment_id`, `created_at`) VALUES
(NULL, 16, 'medicine', '晚间服用维生素D', '2025-10-23 20:00:00', '家中', 'pending', '19:45:00', 'none', NULL, '2025-10-23 00:00:00'),
(NULL, 16, 'other', '听养生健康讲座', '2025-10-23 10:00:00', '社区卫生服务中心', 'pending', '09:30:00', 'none', NULL, '2025-10-23 00:00:00');

-- 10月24日 - 周四
INSERT INTO `schedules` (`parent_schedule_id`, `user_id`, `type`, `content`, `time`, `location`, `status`, `reminder_time`, `repeat_type`, `appointment_id`, `created_at`) VALUES
(NULL, 16, 'exercise', '太极剑晨练', '2025-10-24 07:00:00', '社区公园', 'pending', '06:45:00', 'none', NULL, '2025-10-24 00:00:00'),
(NULL, 16, 'doctor', '内分泌科血糖检测', '2025-10-24 09:30:00', '协和医院内分泌科', 'pending', '08:30:00', 'none', NULL, '2025-10-24 00:00:00');

-- 10月26日 - 周六
INSERT INTO `schedules` (`parent_schedule_id`, `user_id`, `type`, `content`, `time`, `location`, `status`, `reminder_time`, `repeat_type`, `appointment_id`, `created_at`) VALUES
(NULL, 16, 'meal', '周末特色早茶', '2025-10-26 08:00:00', '老字号茶楼', 'pending', '07:30:00', 'none', NULL, '2025-10-26 00:00:00'),
(NULL, 16, 'other', '与老友品茶聊天', '2025-10-26 14:00:00', '翠湖茶楼', 'pending', '13:30:00', 'none', NULL, '2025-10-26 00:00:00');

-- 10月28日 - 周一
INSERT INTO `schedules` (`parent_schedule_id`, `user_id`, `type`, `content`, `time`, `location`, `status`, `reminder_time`, `repeat_type`, `appointment_id`, `created_at`) VALUES
(NULL, 16, 'doctor', '骨科关节健康检查', '2025-10-28 10:00:00', '积水潭医院骨科', 'pending', '09:00:00', 'none', NULL, '2025-10-28 00:00:00'),
(NULL, 16, 'exercise', '广场舞练习', '2025-10-28 17:00:00', '社区广场', 'pending', '16:45:00', 'none', NULL, '2025-10-28 00:00:00');

-- 10月29日 - 周二
INSERT INTO `schedules` (`parent_schedule_id`, `user_id`, `type`, `content`, `time`, `location`, `status`, `reminder_time`, `repeat_type`, `appointment_id`, `created_at`) VALUES
(NULL, 16, 'medicine', '早晨服用降压药（缬沙坦片）', '2025-10-29 08:00:00', '家中', 'pending', '07:45:00', 'none', NULL, '2025-10-29 00:00:00'),
(NULL, 16, 'other', '练习毛笔字', '2025-10-29 15:00:00', '家中书房', 'pending', '14:45:00', 'none', NULL, '2025-10-29 00:00:00');

-- 10月31日 - 周四
INSERT INTO `schedules` (`parent_schedule_id`, `user_id`, `type`, `content`, `time`, `location`, `status`, `reminder_time`, `repeat_type`, `appointment_id`, `created_at`) VALUES
(NULL, 16, 'meal', '养生滋补汤（鸡汤炖山药）', '2025-10-31 12:00:00', '家中', 'pending', '11:45:00', 'none', NULL, '2025-10-31 00:00:00'),
(NULL, 16, 'other', '月度健康总结与计划', '2025-10-31 14:00:00', '家中', 'pending', '13:45:00', 'none', NULL, '2025-10-31 00:00:00');

-- ----------------------------
-- 数据统计说明
-- ----------------------------
-- 总计: 30条日程记录
-- 时间范围: 2025-10-08 至 2025-10-31 (24天)
-- 
-- 日程类型分布:
-- - medicine (服药): 5条
-- - doctor (就医): 5条
-- - exercise (锻炼): 6条
-- - meal (饮食): 4条
-- - sleep (睡眠): 2条
-- - other (其他活动): 8条
-- 
-- 设计特点:
-- 1. 涵盖老年人日常生活的各个方面
-- 2. 包含规律的服药、锻炼等健康管理活动
-- 3. 安排丰富的社交和文娱活动
-- 4. 定期的医疗检查和康复理疗
-- 5. 重视睡眠和饮食的规律性
-- 6. 体现家庭关怀（家庭聚餐等）
-- 7. 融入社区活动（老年大学、兴趣小组等）
-- ----------------------------

