/*
 Navicat Premium Dump SQL

 Source Server         : Jerry-USTB
 Source Server Type    : MySQL
 Source Server Version : 80042 (8.0.42)
 Source Host           : localhost:3306
 Source Schema         : eldercare_project

 Target Server Type    : MySQL
 Target Server Version : 80042 (8.0.42)
 File Encoding         : 65001

 Date: 09/10/2025 23:32:41
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for agreements
-- ----------------------------
DROP TABLE IF EXISTS `agreements`;
CREATE TABLE `agreements`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `type` enum('agreement','privacy','terms','policy') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '协议类型',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '协议标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '协议内容',
  `version` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '协议版本',
  `effective_date` date NOT NULL COMMENT '生效日期',
  `is_current` tinyint(1) NULL DEFAULT 0 COMMENT '是否当前版本',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of agreements
-- ----------------------------

-- ----------------------------
-- Table structure for app_info
-- ----------------------------
DROP TABLE IF EXISTS `app_info`;
CREATE TABLE `app_info`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '应用名称',
  `version` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '应用版本',
  `build_number` int NOT NULL COMMENT '内部版本号',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '应用描述',
  `download_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '下载链接',
  `is_mandatory` tinyint(1) NULL DEFAULT 0 COMMENT '是否强制更新',
  `release_notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '更新说明',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of app_info
-- ----------------------------

-- ----------------------------
-- Table structure for appointments
-- ----------------------------
DROP TABLE IF EXISTS `appointments`;
CREATE TABLE `appointments`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '志愿者服务预约ID',
  `elder_id` int NOT NULL COMMENT '关联老人ID',
  `volunteer_id` int NULL DEFAULT NULL COMMENT '关联志愿者ID',
  `appointment_type` enum('doctor','nurse','rehab','therapy','other') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'other' COMMENT '预约类型',
  `appointment_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '预约内容',
  `start_time` timestamp NOT NULL COMMENT '服务开始时间',
  `end_time` timestamp NULL DEFAULT NULL COMMENT '服务结束时间',
  `location` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '预约地点',
  `status` enum('pending','confirmed','completed','canceled','time_out','no_show') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'pending' COMMENT '预约状态',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `user_id`(`elder_id` ASC) USING BTREE,
  INDEX `volunteer_id`(`volunteer_id` ASC) USING BTREE,
  CONSTRAINT `appointments_ibfk_1` FOREIGN KEY (`elder_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `appointments_ibfk_2` FOREIGN KEY (`volunteer_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 29 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of appointments
-- ----------------------------
INSERT INTO `appointments` VALUES (13, 16, NULL, 'doctor', '老年慢性病复诊：血压、用药调整与随访建议', '2025-09-17 18:00:00', '2025-09-17 18:30:00', NULL, 'completed', '2025-09-17 17:50:10', '2025-09-17 21:52:06');
INSERT INTO `appointments` VALUES (16, 16, NULL, 'doctor', '患有高血压和糖尿病，需要志愿者陪同前往社区医院进行常规复查，并协助与医生沟通病情、记录医嘱\n\n备注：老人姓李，行动稍有不便，需使用拐杖。希望志愿者能耐心一些，帮助记录血压、血糖数值', '2025-09-18 09:00:00', '2025-09-18 11:00:00', '向阳社区卫生服务中心三楼内科诊室', 'time_out', '2025-09-17 18:15:41', '2025-09-17 22:05:00');
INSERT INTO `appointments` VALUES (17, 16, NULL, 'nurse', '需要志愿者上门协助进行每周一次的身体擦浴和更换床单服务。老人因手术后恢复期，自行完成这些动作有困难\n\n备注：对花粉过敏，请志愿者避免使用含有花香的洗护产品。家中备有所有需要的护理用品', '2025-09-22 14:00:00', '2025-09-22 15:30:00', '幸福路小区5号楼201室', 'time_out', '2025-09-17 18:17:06', '2025-09-21 22:25:30');
INSERT INTO `appointments` VALUES (18, 16, NULL, 'rehab', '几个月前因中风导致左侧肢体活动受限，需要志愿者上门，按照康复师制定的方案，指导和辅助进行每日的居家康复训练，如抬腿、抓握等\n\n备注：性格有些内向，请志愿者多给予鼓励。康复训练计划贴在客厅墙上', '2025-09-25 10:00:00', '2025-09-25 11:00:00', '康乐家园12栋1单元803室', 'time_out', '2025-09-17 18:18:33', '2025-09-26 22:05:00');
INSERT INTO `appointments` VALUES (19, 16, NULL, 'therapy', '子女长期在国外，老人近期情绪低落，时常感到孤独。希望能有志愿者上门陪同聊天、读报，进行情感上的沟通和慰藉\n\n备注：喜欢听旧上海的歌曲，如果志愿者方便，可以准备一些相关话题或音乐。老人听力略有下降，沟通时请适当提高音量', '2025-09-28 15:00:00', '2025-09-28 16:30:00', '书香门第小区A座1502室', 'time_out', '2025-09-17 18:19:20', '2025-09-27 22:05:00');
INSERT INTO `appointments` VALUES (20, 16, NULL, 'other', '家中的智能电视和手机操作不熟练，经常无法观看想看的节目或与家人视频通话。需要一名熟悉电子产品的志愿者上门教学\n\n备注：希望能将操作步骤用大字写在纸上，方便日后自己查阅。请志愿者尽量用简单易懂的语言进行讲解', '2025-09-30 10:30:00', '2025-09-30 11:30:00', '绿地花园3期7号楼401室', 'time_out', '2025-09-17 18:20:15', '2025-09-29 22:05:00');
INSERT INTO `appointments` VALUES (23, 16, NULL, 'doctor', '需要志愿者搀扶，陪同去社区医院看病', '2025-09-18 09:00:00', NULL, NULL, 'time_out', '2025-09-17 22:22:01', '2025-09-18 22:05:00');
INSERT INTO `appointments` VALUES (26, 16, NULL, 'doctor', '检查听力', '2025-09-25 11:00:00', NULL, '北医三院', 'time_out', '2025-09-18 22:01:50', '2025-09-26 22:05:00');
INSERT INTO `appointments` VALUES (27, 16, NULL, 'doctor', '陪同前往积水潭医院骨科复查并协助挂号、就诊', '2025-09-29 11:00:00', NULL, '积水潭医院骨科', 'time_out', '2025-09-18 22:16:36', '2025-09-29 22:05:00');
INSERT INTO `appointments` VALUES (28, 30, NULL, 'therapy', '心理咨询', '2025-09-30 04:28:00', '2025-09-30 07:28:00', '辽宁省', 'canceled', '2025-09-27 01:28:19', '2025-09-27 01:28:56');

-- ----------------------------
-- Table structure for assistance
-- ----------------------------
DROP TABLE IF EXISTS `assistance`;
CREATE TABLE `assistance`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '远程志愿者协助服务ID',
  `elder_id` int NOT NULL COMMENT '关联老人ID',
  `volunteer_id` int NULL DEFAULT NULL COMMENT '关联志愿者ID',
  `appointment_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '协助内容',
  `status` enum('waiting_apply','waiting_response','waiting_call','calling','in_progress','waiting_call_again','completed','cancelled') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'waiting_apply' COMMENT '状态',
  `start_time` timestamp NULL DEFAULT NULL COMMENT '协助开始时间',
  `end_time` timestamp NULL DEFAULT NULL COMMENT '协助结束时间',
  `apply_time` timestamp NULL DEFAULT NULL COMMENT '申请提交时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `user_id`(`elder_id` ASC) USING BTREE,
  INDEX `volunteer_id`(`volunteer_id` ASC) USING BTREE,
  CONSTRAINT `assistance_ibfk_1` FOREIGN KEY (`elder_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `assistance_ibfk_2` FOREIGN KEY (`volunteer_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 71 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of assistance
-- ----------------------------
INSERT INTO `assistance` VALUES (62, 16, 23, '教我使用电脑上网', 'completed', '2025-09-19 15:26:01', '2025-09-19 15:26:45', '2025-09-19 15:25:55', '2025-09-19 15:25:55', '2025-09-19 15:26:45');
INSERT INTO `assistance` VALUES (63, 16, 23, '女儿给我买了一台新电脑，但是我不太会用，希望志愿者能教教我如何使用电脑上网', 'completed', '2025-09-20 14:09:05', '2025-10-08 18:07:40', '2025-09-20 14:08:29', '2025-09-20 14:08:29', '2025-10-08 18:07:40');

-- ----------------------------
-- Table structure for contact_info
-- ----------------------------
DROP TABLE IF EXISTS `contact_info`;
CREATE TABLE `contact_info`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '电子邮箱',
  `website` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '官方网站',
  `wechat` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '微信公众号',
  `address` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '公司地址',
  `working_hours` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工作时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of contact_info
-- ----------------------------
INSERT INTO `contact_info` VALUES (1, '18910488457', 'U202342508@xs.ustb.edu.cn', 'www.eldercare.com', '', NULL, NULL, '2025-09-18 11:07:27');

-- ----------------------------
-- Table structure for devices
-- ----------------------------
DROP TABLE IF EXISTS `devices`;
CREATE TABLE `devices`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL COMMENT '关联老人ID',
  `device_type` enum('watch','bracelet','scale','monitor','other') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'other' COMMENT '设备类型',
  `device_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备名称',
  `device_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备型号',
  `device_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备唯一标识',
  `last_active` timestamp NULL DEFAULT NULL COMMENT '最后活跃时间',
  `status` enum('active','inactive','lost','broken') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'active' COMMENT '设备状态',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `device_id`(`device_id` ASC) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `devices_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of devices
-- ----------------------------

-- ----------------------------
-- Table structure for elder_locations
-- ----------------------------
DROP TABLE IF EXISTS `elder_locations`;
CREATE TABLE `elder_locations`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` int NOT NULL COMMENT '关联用户表ID',
  `latitude` decimal(10, 6) NOT NULL COMMENT '纬度（保留6位小数，范围-90~90）',
  `longitude` decimal(10, 6) NOT NULL COMMENT '经度（保留6位小数，范围-180~180）',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '位置更新时间',
  `is_valid` tinyint(1) NOT NULL DEFAULT 1 COMMENT '位置是否有效（1-有效，0-无效）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE COMMENT '按用户ID查询索引',
  INDEX `idx_update_time`(`update_time` ASC) USING BTREE COMMENT '按更新时间查询索引',
  CONSTRAINT `elder_locations_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 106 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '老年人位置信息表（存储老年人的经纬度位置及更新时间）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of elder_locations
-- ----------------------------
INSERT INTO `elder_locations` VALUES (31, 40, 42.423513, 128.110856, '2025-10-06 19:23:40', 1);
INSERT INTO `elder_locations` VALUES (32, 42, 42.423513, 128.110856, '2025-10-06 19:25:27', 1);
INSERT INTO `elder_locations` VALUES (62, 45, 41.111181, 122.942981, '2025-10-08 17:38:37', 1);
INSERT INTO `elder_locations` VALUES (98, 46, 39.987054, 116.352745, '2025-10-09 16:03:37', 1);
INSERT INTO `elder_locations` VALUES (99, 48, 39.987068, 116.352625, '2025-10-09 17:08:04', 1);
INSERT INTO `elder_locations` VALUES (105, 16, 40.064674, 116.346458, '2025-10-09 22:48:00', 1);

-- ----------------------------
-- Table structure for elders
-- ----------------------------
DROP TABLE IF EXISTS `elders`;
CREATE TABLE `elders`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL COMMENT '关联用户ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '老人姓名',
  `gender` enum('male','female','unknown') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'unknown' COMMENT '性别',
  `birthday` date NULL DEFAULT NULL COMMENT '出生日期',
  `address` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '居住地址',
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像URL',
  `health_condition` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '健康状况描述',
  `medical_history` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '病史记录',
  `daily_care_needs` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '日常护理需求',
  `preferences` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '个人偏好',
  `emergency_contact_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '紧急联系人姓名',
  `emergency_contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '紧急联系人电话',
  `emergency_contact_relation` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '紧急联系人关系',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `elders_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of elders
-- ----------------------------
INSERT INTO `elders` VALUES (5, 16, '张爷爷', 'male', '2005-06-21', '北京市海淀区学院路30号院北京科技大学九斋', '/uploads/avatars/avatar_16_5f3a3894-a760-4e8c-b604-5ddef2c0c010.jpg', NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2025-08-09 21:29:17', '2025-09-18 21:46:16');
INSERT INTO `elders` VALUES (9, 30, '15941209366', 'male', NULL, '', '/uploads/avatars/default-avatar.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2025-09-27 01:26:44', '2025-09-27 01:29:17');
INSERT INTO `elders` VALUES (10, 40, '张三', 'unknown', NULL, NULL, '/uploads/avatars/default-avatar.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2025-10-06 19:23:38', '2025-10-06 19:23:53');
INSERT INTO `elders` VALUES (11, 42, '张三三', 'unknown', NULL, NULL, '/uploads/avatars/default-avatar.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2025-10-06 19:25:25', '2025-10-06 19:25:41');
INSERT INTO `elders` VALUES (12, 45, '李四', 'unknown', NULL, NULL, '/uploads/avatars/default-avatar.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2025-10-08 17:38:18', '2025-10-08 17:38:56');
INSERT INTO `elders` VALUES (13, 46, '13651111201', 'unknown', NULL, NULL, '/uploads/avatars/default-avatar.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2025-10-09 16:02:37', '2025-10-09 16:02:37');
INSERT INTO `elders` VALUES (14, 48, '15842278888', 'unknown', NULL, NULL, '/uploads/avatars/default-avatar.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2025-10-09 17:08:02', '2025-10-09 17:08:02');

-- ----------------------------
-- Table structure for emergencies
-- ----------------------------
DROP TABLE IF EXISTS `emergencies`;
CREATE TABLE `emergencies`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL COMMENT '关联老人ID',
  `guardian_id` int NULL DEFAULT NULL COMMENT '关联监护者ID',
  `situation_type` enum('fall','heart_attack','stroke','accident','other') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'other' COMMENT '紧急情况类型',
  `situation_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '情况描述',
  `location` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '发生位置',
  `is_resolved` tinyint(1) NULL DEFAULT 0 COMMENT '是否已解决',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `resolved_at` timestamp NULL DEFAULT NULL COMMENT '解决时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  INDEX `guardian_id`(`guardian_id` ASC) USING BTREE,
  CONSTRAINT `emergencies_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `emergencies_ibfk_2` FOREIGN KEY (`guardian_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of emergencies
-- ----------------------------

-- ----------------------------
-- Table structure for guardians
-- ----------------------------
DROP TABLE IF EXISTS `guardians`;
CREATE TABLE `guardians`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL COMMENT '关联用户ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '监护人姓名',
  `gender` enum('male','female','unknown') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'unknown' COMMENT '性别',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地址',
  `birthday` date NULL DEFAULT NULL COMMENT '出生日期',
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像URL',
  `profession` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '职业',
  `workplace` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工作单位',
  `care_experience` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '照顾经验',
  `availability` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '可用时间',
  `relationship_with_elderly` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '与老人关系',
  `verification_status` enum('pending','verified','rejected') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'pending' COMMENT '认证状态',
  `verification_documents` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '认证材料',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `guardians_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of guardians
-- ----------------------------
INSERT INTO `guardians` VALUES (1, 24, '游客24', 'unknown', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, '2025-09-17 01:00:37', '2025-09-17 01:00:37');
INSERT INTO `guardians` VALUES (2, 31, '游客31', 'unknown', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, '2025-09-27 01:30:58', '2025-09-27 01:30:58');
INSERT INTO `guardians` VALUES (3, 32, '游客32', 'unknown', NULL, NULL, '/upload/guardian_32_73ec1be1-519e-486f-b366-47015d318b47.jpg', NULL, NULL, NULL, NULL, NULL, 'pending', NULL, '2025-10-02 23:51:39', '2025-10-06 10:47:00');
INSERT INTO `guardians` VALUES (4, 34, '张爷爷的女儿', 'unknown', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, '2025-10-04 17:19:59', '2025-10-04 17:19:59');
INSERT INTO `guardians` VALUES (5, 41, '李四', 'male', NULL, '1999-10-15', '/upload/guardian_41_7e649638-2ad2-47f4-8e6d-8a71de19c69e.jpg', NULL, NULL, NULL, NULL, NULL, 'pending', NULL, '2025-10-06 19:24:20', '2025-10-06 19:27:42');
INSERT INTO `guardians` VALUES (6, 44, '游客44', 'unknown', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, '2025-10-08 15:41:37', '2025-10-08 15:41:37');

-- ----------------------------
-- Table structure for health_info
-- ----------------------------
DROP TABLE IF EXISTS `health_info`;
CREATE TABLE `health_info`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL COMMENT '关联老人ID',
  `health_type` enum('heart_rate','blood_pressure','blood_sugar','weight','temperature','steps','other') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'other' COMMENT '健康数据类型',
  `value` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '健康数据值',
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '数据单位',
  `record_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  `notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '备注信息',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `health_info_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of health_info
-- ----------------------------

-- ----------------------------
-- Table structure for medications
-- ----------------------------
DROP TABLE IF EXISTS `medications`;
CREATE TABLE `medications`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL COMMENT '关联老人ID',
  `medicine_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '药品名称',
  `dosage` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '剂量',
  `frequency` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '服用频率',
  `start_date` date NULL DEFAULT NULL COMMENT '开始日期',
  `end_date` date NULL DEFAULT NULL COMMENT '结束日期',
  `expire_date` date NULL DEFAULT NULL COMMENT '药物过期时间',
  `notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '备注信息',
  `is_active` tinyint(1) NULL DEFAULT 1 COMMENT '是否正在服用',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `medications_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of medications
-- ----------------------------
INSERT INTO `medications` VALUES (1, 16, '维生素C', '1片/次', '每日3次', '2025-10-04', NULL, NULL, NULL, 1, '2025-10-03 00:43:28', '2025-10-03 00:43:28');
INSERT INTO `medications` VALUES (2, 16, '维生素D', '1片/次', '每日2次', '2025-10-06', NULL, NULL, NULL, 1, '2025-10-06 10:28:01', '2025-10-06 10:28:01');
INSERT INTO `medications` VALUES (3, 16, '维生素E', '1片1次', '每日1次', '2025-10-06', NULL, NULL, NULL, 1, '2025-10-06 10:31:40', '2025-10-06 10:31:40');

-- ----------------------------
-- Table structure for messages
-- ----------------------------
DROP TABLE IF EXISTS `messages`;
CREATE TABLE `messages`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL COMMENT '接收用户ID',
  `sender_id` int NULL DEFAULT NULL COMMENT '发送者ID',
  `notification_type` enum('reminder','alert','message','update','other') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'other' COMMENT '通知类型',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知内容',
  `is_read` tinyint(1) NULL DEFAULT 0 COMMENT '是否已读',
  `related_id` int NULL DEFAULT NULL COMMENT '关联ID(如日程ID)',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  INDEX `sender_id`(`sender_id` ASC) USING BTREE,
  CONSTRAINT `messages_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `messages_ibfk_2` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of messages
-- ----------------------------

-- ----------------------------
-- Table structure for relations
-- ----------------------------
DROP TABLE IF EXISTS `relations`;
CREATE TABLE `relations`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `guardian_id` int NOT NULL COMMENT '监护者ID',
  `elderly_id` int NOT NULL COMMENT '被监护老人ID',
  `relationship` enum('spouse','child','parent','sibling','friend','other') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'other' COMMENT '关系类型',
  `is_primary` tinyint(1) NULL DEFAULT 0 COMMENT '是否为主监护者',
  `remarks` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `guardian_id`(`guardian_id` ASC, `elderly_id` ASC) USING BTREE,
  INDEX `elderly_id`(`elderly_id` ASC) USING BTREE,
  CONSTRAINT `relations_ibfk_1` FOREIGN KEY (`guardian_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `relations_ibfk_2` FOREIGN KEY (`elderly_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of relations
-- ----------------------------
INSERT INTO `relations` VALUES (6, 34, 16, 'child', 0, NULL, '2025-10-08 11:43:16', '2025-10-08 11:43:16');

-- ----------------------------
-- Table structure for schedules
-- ----------------------------
DROP TABLE IF EXISTS `schedules`;
CREATE TABLE `schedules`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '日程ID',
  `parent_schedule_id` int NULL DEFAULT NULL COMMENT '父日程ID',
  `user_id` int NOT NULL COMMENT '关联老人ID',
  `type` enum('medicine','doctor','exercise','meal','sleep','appointment','other') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '日程类型',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '日程内容',
  `time` timestamp NOT NULL COMMENT '日程日期+时间',
  `location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '日程地点',
  `status` enum('pending','completed','canceled','overdue') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '日程状态',
  `reminder_time` time NULL DEFAULT NULL COMMENT '提醒时间',
  `repeat_type` enum('none','daily','weekly','monthly') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'none' COMMENT '重复类型',
  `appointment_id` int NULL DEFAULT NULL COMMENT '志愿者服务预约ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`, `type`) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `schedules_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 958 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of schedules
-- ----------------------------
INSERT INTO `schedules` VALUES (912, NULL, 16, 'other', '办理身份证', '2025-09-17 20:00:00', '派出所', 'pending', NULL, 'none', NULL, '2025-09-15 22:21:26', '2025-09-15 22:21:26');
INSERT INTO `schedules` VALUES (915, NULL, 16, 'medicine', '早晨服用降压药（缬沙坦片）', '2025-09-15 08:00:00', '家中', 'completed', '07:45:00', 'none', NULL, '2025-09-17 17:43:12', '2025-09-17 17:43:56');
INSERT INTO `schedules` VALUES (916, NULL, 16, 'medicine', '餐后服用钙片', '2025-09-17 12:30:00', '家中', 'completed', '12:15:00', 'none', NULL, '2025-09-17 17:43:12', '2025-09-17 17:44:03');
INSERT INTO `schedules` VALUES (917, NULL, 16, 'medicine', '晚间服用维生素D', '2025-09-19 20:00:00', '家中', 'pending', '19:45:00', 'none', NULL, '2025-09-17 17:43:12', '2025-09-17 17:43:12');
INSERT INTO `schedules` VALUES (918, NULL, 16, 'doctor', '心血管科复诊检查', '2025-09-16 09:30:00', '北京安贞医院', 'completed', '08:30:00', 'none', NULL, '2025-09-17 17:43:12', '2025-09-17 17:44:04');
INSERT INTO `schedules` VALUES (919, NULL, 16, 'doctor', '眼科定期检查（白内障复查）', '2025-09-20 14:00:00', '同仁医院眼科', 'pending', '13:00:00', 'none', NULL, '2025-09-17 17:43:12', '2025-09-17 17:43:12');
INSERT INTO `schedules` VALUES (920, NULL, 16, 'doctor', '骨科关节检查', '2025-09-25 10:30:00', '积水潭医院', 'pending', '09:30:00', 'none', NULL, '2025-09-17 17:43:12', '2025-09-17 17:43:12');
INSERT INTO `schedules` VALUES (921, NULL, 16, 'exercise', '晨练太极拳', '2025-09-15 06:30:00', '社区公园', 'pending', '06:15:00', 'none', NULL, '2025-09-17 17:43:12', '2025-09-17 17:43:12');
INSERT INTO `schedules` VALUES (922, NULL, 16, 'exercise', '健步走锻炼', '2025-09-18 17:00:00', '奥林匹克森林公园', 'pending', '16:45:00', 'none', NULL, '2025-09-17 17:43:12', '2025-09-17 17:43:12');
INSERT INTO `schedules` VALUES (923, NULL, 16, 'exercise', '老年体操课', '2025-09-21 15:30:00', '社区活动中心', 'pending', '15:15:00', 'none', NULL, '2025-09-17 17:43:12', '2025-09-17 17:43:12');
INSERT INTO `schedules` VALUES (924, NULL, 16, 'exercise', '游泳锻炼', '2025-09-24 09:00:00', '社区游泳馆', 'pending', '08:45:00', 'none', NULL, '2025-09-17 17:43:12', '2025-09-17 17:43:12');
INSERT INTO `schedules` VALUES (925, NULL, 16, 'meal', '营养早餐（燕麦粥配鸡蛋）', '2025-09-16 07:30:00', '家中', 'completed', '07:15:00', 'none', NULL, '2025-09-17 17:43:12', '2025-09-17 17:43:57');
INSERT INTO `schedules` VALUES (926, NULL, 16, 'meal', '补充维生素水果餐', '2025-09-19 15:00:00', '家中', 'pending', '14:45:00', 'none', NULL, '2025-09-17 17:43:12', '2025-09-17 17:43:12');
INSERT INTO `schedules` VALUES (927, NULL, 16, 'meal', '清淡养生汤', '2025-09-23 12:00:00', '家中', 'pending', '11:45:00', 'none', NULL, '2025-09-17 17:43:12', '2025-09-17 17:43:12');
INSERT INTO `schedules` VALUES (928, NULL, 16, 'sleep', '午休时间', '2025-09-17 13:30:00', '家中卧室', 'pending', '13:15:00', 'none', NULL, '2025-09-17 17:43:12', '2025-09-17 17:43:12');
INSERT INTO `schedules` VALUES (929, NULL, 16, 'sleep', '规律作息早睡', '2025-09-21 21:30:00', '家中卧室', 'pending', '21:15:00', 'none', NULL, '2025-09-17 17:43:12', '2025-09-17 17:43:12');
INSERT INTO `schedules` VALUES (930, NULL, 16, 'other', '参加社区老年合唱团', '2025-09-18 09:30:00', '社区文化中心', 'pending', '09:00:00', 'none', NULL, '2025-09-17 17:43:12', '2025-09-17 17:43:12');
INSERT INTO `schedules` VALUES (931, NULL, 16, 'other', '书法练习', '2025-09-20 10:00:00', '家中书房', 'pending', '09:45:00', 'none', NULL, '2025-09-17 17:43:12', '2025-09-17 17:43:12');
INSERT INTO `schedules` VALUES (932, NULL, 16, 'other', '与老友聚会品茶', '2025-09-22 14:30:00', '翠湖茶楼', 'pending', '14:00:00', 'none', NULL, '2025-09-17 17:43:12', '2025-09-17 17:43:12');
INSERT INTO `schedules` VALUES (933, NULL, 16, 'other', '观看京剧表演', '2025-09-26 19:30:00', '梅兰芳大剧院', 'pending', '18:30:00', 'none', NULL, '2025-09-17 17:43:12', '2025-09-17 17:43:12');
INSERT INTO `schedules` VALUES (934, NULL, 16, 'other', '阅读养生保健书籍', '2025-09-30 16:00:00', '家中客厅', 'pending', '15:45:00', 'none', NULL, '2025-09-17 17:43:12', '2025-09-17 17:43:12');
INSERT INTO `schedules` VALUES (935, NULL, 16, 'appointment', '老年慢性病复诊：血压、用药调整与随访建议', '2025-09-17 18:00:00', '待志愿者确认', 'pending', '17:30:00', 'none', 13, '2025-09-17 17:50:10', '2025-09-17 17:50:10');
INSERT INTO `schedules` VALUES (938, NULL, 16, 'appointment', '患有高血压和糖尿病，需要志愿者陪同前往社区医院进行常规复查，并协助与医生沟通病情、记录医嘱\n\n备注：老人姓李，行动稍有不便，需使用拐杖。希望志愿者能耐心一些，帮助记录血压、血糖数值', '2025-09-18 09:00:00', '向阳社区卫生服务中心三楼内科诊室', 'pending', '08:30:00', 'none', 16, '2025-09-17 18:15:41', '2025-09-17 18:15:41');
INSERT INTO `schedules` VALUES (939, NULL, 16, 'appointment', '需要志愿者上门协助进行每周一次的身体擦浴和更换床单服务。老人因手术后恢复期，自行完成这些动作有困难\n\n备注：对花粉过敏，请志愿者避免使用含有花香的洗护产品。家中备有所有需要的护理用品', '2025-09-22 14:00:00', '幸福路小区5号楼201室', 'pending', '13:30:00', 'none', 17, '2025-09-17 18:17:06', '2025-09-17 18:17:06');
INSERT INTO `schedules` VALUES (940, NULL, 16, 'appointment', '几个月前因中风导致左侧肢体活动受限，需要志愿者上门，按照康复师制定的方案，指导和辅助进行每日的居家康复训练，如抬腿、抓握等\n\n备注：性格有些内向，请志愿者多给予鼓励。康复训练计划贴在客厅墙上', '2025-09-25 10:00:00', '康乐家园12栋1单元803室', 'pending', '09:30:00', 'none', 18, '2025-09-17 18:18:33', '2025-09-17 18:18:33');
INSERT INTO `schedules` VALUES (941, NULL, 16, 'appointment', '子女长期在国外，老人近期情绪低落，时常感到孤独。希望能有志愿者上门陪同聊天、读报，进行情感上的沟通和慰藉\n\n备注：喜欢听旧上海的歌曲，如果志愿者方便，可以准备一些相关话题或音乐。老人听力略有下降，沟通时请适当提高音量', '2025-09-28 15:00:00', '书香门第小区A座1502室', 'pending', '14:30:00', 'none', 19, '2025-09-17 18:19:20', '2025-09-17 18:19:20');
INSERT INTO `schedules` VALUES (942, NULL, 16, 'appointment', '家中的智能电视和手机操作不熟练，经常无法观看想看的节目或与家人视频通话。需要一名熟悉电子产品的志愿者上门教学\n\n备注：希望能将操作步骤用大字写在纸上，方便日后自己查阅。请志愿者尽量用简单易懂的语言进行讲解', '2025-09-30 10:30:00', '绿地花园3期7号楼401室', 'pending', '10:00:00', 'none', 20, '2025-09-17 18:20:15', '2025-09-17 18:20:15');
INSERT INTO `schedules` VALUES (943, NULL, 16, 'other', '睡前敷面膜', '2025-09-17 22:00:00', '家中', 'pending', NULL, 'none', NULL, '2025-09-17 21:18:38', '2025-09-17 21:18:38');
INSERT INTO `schedules` VALUES (952, NULL, 16, 'appointment', '检查听力', '2025-09-25 11:00:00', '北医三院', 'pending', '10:30:00', 'none', 26, '2025-09-18 22:01:50', '2025-09-18 22:01:50');
INSERT INTO `schedules` VALUES (953, NULL, 16, 'other', '去茶馆喝茶', '2025-09-20 15:00:00', '茶馆', 'pending', NULL, 'none', NULL, '2025-09-18 22:14:26', '2025-09-18 22:15:25');
INSERT INTO `schedules` VALUES (954, NULL, 30, 'appointment', '心理咨询', '2025-09-30 04:28:00', '辽宁省', 'pending', '03:58:00', 'none', 28, '2025-09-27 01:28:19', '2025-09-27 01:28:19');
INSERT INTO `schedules` VALUES (956, NULL, 16, 'other', '跟朋友喝茶', '2025-09-30 16:00:00', '朋友家里', 'pending', NULL, 'none', NULL, '2025-09-29 18:16:07', '2025-09-29 18:16:07');
INSERT INTO `schedules` VALUES (957, NULL, 16, 'other', '和朋友喝咖啡', '2025-10-01 09:00:00', '朋友家里', 'pending', NULL, 'none', NULL, '2025-09-29 20:33:56', '2025-09-29 20:33:56');

-- ----------------------------
-- Table structure for services
-- ----------------------------
DROP TABLE IF EXISTS `services`;
CREATE TABLE `services`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `volunteer_id` int NOT NULL COMMENT '关联志愿者ID',
  `elderly_id` int NOT NULL COMMENT '关联老人ID',
  `service_type` enum('companion','shopping','medical','housework','other') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'other' COMMENT '服务类型',
  `service_time` timestamp NOT NULL COMMENT '服务时间',
  `service_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '服务内容',
  `duration_minutes` int NULL DEFAULT NULL COMMENT '服务时长(分钟)',
  `rating` tinyint NULL DEFAULT NULL COMMENT '服务评分(1-5)',
  `feedback` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '服务反馈',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `service_status` enum('pending','confirmed','completed','canceled') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'pending' COMMENT '服务状态',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `volunteer_id`(`volunteer_id` ASC) USING BTREE,
  INDEX `elderly_id`(`elderly_id` ASC) USING BTREE,
  CONSTRAINT `services_ibfk_1` FOREIGN KEY (`volunteer_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `services_ibfk_2` FOREIGN KEY (`elderly_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of services
-- ----------------------------

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户手机号',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '加密后的密码',
  `role` enum('elder','guardian','volunteer','admin') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户角色',
  `push_clientid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '推送消息时的设备ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `last_login_at` timestamp NULL DEFAULT NULL COMMENT '最后登录时间',
  `status` enum('active','inactive','blocked') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'active' COMMENT '用户状态',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `phone`(`phone` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 49 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (16, '18910488457', 'acd36f662b05e59de6d555011a520e31', 'elder', 'cfd6afeb4ac3c020ccd7b9c7cda69e3c', '2025-08-09 21:29:11', '2025-08-09 21:29:17', '2025-10-09 22:47:57', 'active');
INSERT INTO `users` VALUES (23, '18910963015', 'acd36f662b05e59de6d555011a520e31', 'volunteer', NULL, '2025-08-26 23:58:22', '2025-08-26 23:58:59', '2025-09-20 14:08:45', 'active');
INSERT INTO `users` VALUES (24, '18910351949', 'acd36f662b05e59de6d555011a520e31', 'guardian', NULL, '2025-09-17 01:00:21', '2025-09-17 01:00:37', '2025-09-17 01:00:33', 'active');
INSERT INTO `users` VALUES (29, '15941209363', 'dd9d21e22391090ddce7c6ed58c6412d', NULL, NULL, '2025-09-27 01:24:50', '2025-09-27 01:24:50', NULL, 'active');
INSERT INTO `users` VALUES (30, '15941209366', 'de88e3e4ab202d87754078cbb2df6063', 'elder', NULL, '2025-09-27 01:26:29', '2025-09-27 01:26:44', '2025-09-27 11:08:56', 'active');
INSERT INTO `users` VALUES (31, '15941209666', 'de88e3e4ab202d87754078cbb2df6063', 'guardian', '5742e4dbc1a8bf1b8a4ffda3d307c87c', '2025-09-27 01:30:33', '2025-09-27 01:30:58', '2025-10-09 17:03:45', 'active');
INSERT INTO `users` VALUES (32, '18910488458', 'acd36f662b05e59de6d555011a520e31', 'guardian', 'e0d66b05aaeec2b81212b3511a253e13', '2025-10-02 23:51:25', '2025-10-02 23:51:39', '2025-10-06 10:47:10', 'active');
INSERT INTO `users` VALUES (33, '18910488459', 'acd36f662b05e59de6d555011a520e31', 'volunteer', NULL, '2025-10-03 00:48:19', '2025-10-03 00:48:32', '2025-10-03 23:44:03', 'active');
INSERT INTO `users` VALUES (34, '18910488450', 'acd36f662b05e59de6d555011a520e31', 'guardian', 'cfd6afeb4ac3c020ccd7b9c7cda69e3c', '2025-10-04 17:19:44', '2025-10-04 17:19:59', '2025-10-09 17:43:07', 'active');
INSERT INTO `users` VALUES (35, '18910488451', 'acd36f662b05e59de6d555011a520e31', 'volunteer', '277dad7a924f2503fb02605b5bfab93b', '2025-10-04 22:22:40', '2025-10-04 22:23:00', '2025-10-09 22:39:44', 'active');
INSERT INTO `users` VALUES (39, '18910488453', 'acd36f662b05e59de6d555011a520e31', NULL, NULL, '2025-10-06 13:32:26', '2025-10-06 13:32:26', '2025-10-06 13:32:26', 'active');
INSERT INTO `users` VALUES (40, '15842279798', 'de88e3e4ab202d87754078cbb2df6063', 'elder', NULL, '2025-10-06 19:23:32', '2025-10-06 19:23:38', '2025-10-06 19:23:32', 'active');
INSERT INTO `users` VALUES (41, '15941203333', 'de88e3e4ab202d87754078cbb2df6063', 'guardian', 'b6a88a6a56d96f7215bd53b7c00c5b1a', '2025-10-06 19:24:17', '2025-10-06 19:28:25', '2025-10-06 19:25:59', 'active');
INSERT INTO `users` VALUES (42, '15842278978', 'de88e3e4ab202d87754078cbb2df6063', 'elder', NULL, '2025-10-06 19:25:21', '2025-10-06 19:25:25', '2025-10-06 19:25:22', 'active');
INSERT INTO `users` VALUES (43, '17326882506', 'acd36f662b05e59de6d555011a520e31', NULL, NULL, '2025-10-08 14:42:52', '2025-10-08 14:42:52', '2025-10-08 14:42:53', 'active');
INSERT INTO `users` VALUES (44, '15941206666', 'de88e3e4ab202d87754078cbb2df6063', 'guardian', '5742e4dbc1a8bf1b8a4ffda3d307c87c', '2025-10-08 15:41:33', '2025-10-08 15:41:37', '2025-10-09 17:07:35', 'active');
INSERT INTO `users` VALUES (45, '15941209999', 'de88e3e4ab202d87754078cbb2df6063', 'elder', 'b6a88a6a56d96f7215bd53b7c00c5b1a', '2025-10-08 17:38:13', '2025-10-08 17:38:18', '2025-10-08 17:38:35', 'active');
INSERT INTO `users` VALUES (46, '13651111201', 'de88e3e4ab202d87754078cbb2df6063', 'elder', NULL, '2025-10-09 16:02:33', '2025-10-09 16:02:37', '2025-10-09 16:02:34', 'active');
INSERT INTO `users` VALUES (47, '15941209663', 'de88e3e4ab202d87754078cbb2df6063', 'volunteer', NULL, '2025-10-09 16:04:29', '2025-10-09 16:04:33', '2025-10-09 16:04:29', 'active');
INSERT INTO `users` VALUES (48, '15842278888', 'de88e3e4ab202d87754078cbb2df6063', 'elder', NULL, '2025-10-09 17:07:58', '2025-10-09 17:08:02', '2025-10-09 17:07:59', 'active');

-- ----------------------------
-- Table structure for volunteers
-- ----------------------------
DROP TABLE IF EXISTS `volunteers`;
CREATE TABLE `volunteers`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL COMMENT '关联用户ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '志愿者姓名',
  `gender` enum('male','female','unknown') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'unknown' COMMENT '性别',
  `birthday` date NULL DEFAULT NULL COMMENT '出生日期',
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像URL',
  `skills` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '技能特长',
  `availability` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '可用时间',
  `experience` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '志愿服务经验',
  `training_certificates` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '培训证书',
  `service_area` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '服务区域',
  `verification_status` enum('pending','verified','rejected') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'pending' COMMENT '认证状态',
  `verification_documents` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '认证材料',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `volunteers_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of volunteers
-- ----------------------------
INSERT INTO `volunteers` VALUES (4, 23, '18910963015', 'unknown', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, '2025-08-26 23:58:59', '2025-08-26 23:58:59');
INSERT INTO `volunteers` VALUES (5, 33, '18910488459', 'unknown', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, '2025-10-03 00:48:32', '2025-10-03 00:48:32');
INSERT INTO `volunteers` VALUES (6, 35, '18910488451', 'unknown', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, '2025-10-04 22:23:00', '2025-10-04 22:23:00');
INSERT INTO `volunteers` VALUES (7, 47, '15941209663', 'unknown', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'pending', NULL, '2025-10-09 16:04:33', '2025-10-09 16:04:33');

SET FOREIGN_KEY_CHECKS = 1;
