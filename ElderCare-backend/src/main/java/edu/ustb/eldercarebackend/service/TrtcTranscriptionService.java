package edu.ustb.eldercarebackend.service;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.trtc.v20190722.TrtcClient;
import com.tencentcloudapi.trtc.v20190722.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 腾讯云TRTC语音转文字服务
 * 处理转录任务的启动和停止
 */
@Service
public class TrtcTranscriptionService {
    
    private static final Logger logger = LoggerFactory.getLogger(TrtcTranscriptionService.class);
    
    @Value("${tencent.cloud.secret-id}")
    private String secretId;
    
    @Value("${tencent.cloud.secret-key}")
    private String secretKey;
    
    @Value("${tencent.cloud.trtc.sdk-app-id}")
    private Long sdkAppId;
    
    // 存储房间ID对应的转录任务ID
    private final ConcurrentHashMap<String, String> roomTaskMap = new ConcurrentHashMap<>();
    
    // 存储房间ID对应的转录文件信息
    private final ConcurrentMap<String, TranscriptFileInfo> roomFileMap = new ConcurrentHashMap<>();
    
    /**
     * 启动AI转录任务
     * @param roomId 房间ID
     * @param userSig 转录机器人的UserSig
     * @param transcriptFileName 转录文件名
     * @param elderId 用户ID
     * @return 任务ID，失败返回null
     */
    public String startTranscription(String roomId, String userSig, String transcriptFileName, String elderId) {
        logger.info("开始启动转录任务 - RoomId: {}, SdkAppId: {}, 文件名: {}, 用户ID: {}", 
                   roomId, sdkAppId, transcriptFileName, elderId);
        
        try {
            // 实例化认证对象
            Credential cred = new Credential(secretId, secretKey);
            
            // 实例化http选项
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("trtc.tencentcloudapi.com");
            
            // 实例化client选项
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            
            // 实例化TRTC客户端
            TrtcClient client = new TrtcClient(cred, "", clientProfile);
            
            // 实例化请求对象
            StartAITranscriptionRequest req = new StartAITranscriptionRequest();
            req.setSdkAppId(sdkAppId);
            req.setRoomId(roomId);
            
            // 设置转录参数
            TranscriptionParams transcriptionParams = new TranscriptionParams();
            transcriptionParams.setUserId("robot");  // 转录机器人ID
            transcriptionParams.setUserSig(userSig);  // 机器人UserSig
            transcriptionParams.setMaxIdleTime(300L); // 最大空闲时间5分钟
            transcriptionParams.setTranscriptionMode(0L); // 拉取全房间的流
            
            req.setTranscriptionParams(transcriptionParams);
            
            // 调用API
            StartAITranscriptionResponse resp = client.StartAITranscription(req);
            
            String taskId = resp.getTaskId();
            logger.info("转录任务启动成功 - TaskId: {}, RoomId: {}", taskId, roomId);
            
            // 保存房间ID和任务ID的映射关系
            roomTaskMap.put(roomId, taskId);
            
            // 保存转录文件信息
            if (transcriptFileName != null && elderId != null) {
                TranscriptFileInfo fileInfo = new TranscriptFileInfo(transcriptFileName, elderId);
                roomFileMap.put(roomId, fileInfo);
                logger.info("保存转录文件信息 - RoomId: {}, 文件名: {}, 用户ID: {}", 
                           roomId, transcriptFileName, elderId);
            }
            
            return taskId;
            
        } catch (TencentCloudSDKException e) {
            logger.error("启动转录任务失败 - RoomId: {}, Error: {}", roomId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 停止AI转录任务
     * @param roomId 房间ID
     * @return 是否成功
     */
    public boolean stopTranscription(String roomId) {
        String taskId = roomTaskMap.get(roomId);
        if (taskId == null) {
            logger.warn("未找到房间对应的转录任务 - RoomId: {}", roomId);
            return false;
        }
        
        return stopTranscriptionByTaskId(taskId, roomId);
    }
    
    /**
     * 通过TaskId停止转录任务
     * @param taskId 任务ID
     * @param roomId 房间ID（用于日志）
     * @return 是否成功
     */
    public boolean stopTranscriptionByTaskId(String taskId, String roomId) {
        logger.info("开始停止转录任务 - TaskId: {}, RoomId: {}", taskId, roomId);
        
        try {
            // 实例化认证对象
            Credential cred = new Credential(secretId, secretKey);
            
            // 实例化http选项
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("trtc.tencentcloudapi.com");
            
            // 实例化client选项
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            
            // 实例化TRTC客户端
            TrtcClient client = new TrtcClient(cred, "", clientProfile);
            
            // 实例化请求对象
            StopAITranscriptionRequest req = new StopAITranscriptionRequest();
            req.setTaskId(taskId);
            
            // 调用API
            client.StopAITranscription(req);
            
            logger.info("转录任务停止成功 - TaskId: {}, RoomId: {}", taskId, roomId);
            
            // 移除映射关系
            roomTaskMap.remove(roomId);
            roomFileMap.remove(roomId);
            
            return true;
            
        } catch (TencentCloudSDKException e) {
            logger.error("停止转录任务失败 - TaskId: {}, RoomId: {}, Error: {}", taskId, roomId, e.getMessage(), e);
            return false;
        }
    }
    
    
    /**
     * 获取当前活跃的转录任务数量
     */
    public int getActiveTaskCount() {
        return roomTaskMap.size();
    }
    
    /**
     * 获取指定房间的任务ID
     */
    public String getTaskIdByRoomId(String roomId) {
        return roomTaskMap.get(roomId);
    }
    
    /**
     * 获取指定房间的转录文件信息
     */
    public TranscriptFileInfo getFileInfoByRoomId(String roomId) {
        return roomFileMap.get(roomId);
    }
    
    /**
     * 转录文件信息内部类
     */
    public static class TranscriptFileInfo {
        private final String fileName;
        private final String elderId;
        
        public TranscriptFileInfo(String fileName, String elderId) {
            this.fileName = fileName;
            this.elderId = elderId;
        }
        
        public String getFileName() {
            return fileName;
        }
        
        public String getElderId() {
            return elderId;
        }
    }
}
