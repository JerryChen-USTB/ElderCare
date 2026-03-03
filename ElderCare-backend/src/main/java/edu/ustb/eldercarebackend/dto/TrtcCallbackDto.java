package edu.ustb.eldercarebackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 腾讯云TRTC回调数据传输对象
 * 根据官方文档: https://cloud.tencent.com/document/product/647/109686#event_infor
 */
public class TrtcCallbackDto {
    
    /**
     * 事件组ID
     * AI服务事件组固定为9
     */
    @JsonProperty("EventGroupId")
    private Integer eventGroupId;
    
    /**
     * 回调通知的事件类型
     * 901: AI任务开始状态回调
     * 902: AI任务结束状态回调  
     * 903: 回调完整的一句话
     */
    @JsonProperty("EventType")
    private Integer eventType;
    
    /**
     * 事件回调服务器向您的服务器发出回调请求的Unix时间戳，单位为毫秒
     * 注意：腾讯云实际发送的字段名是CallbackTs，不是文档中的CallbackMsTs
     */
    @JsonProperty("CallbackTs")
    private Long callbackTs;
    
    /**
     * 事件信息
     */
    @JsonProperty("EventInfo")
    private EventInfo eventInfo;
    
    // Getters and Setters
    public Integer getEventGroupId() {
        return eventGroupId;
    }
    
    public void setEventGroupId(Integer eventGroupId) {
        this.eventGroupId = eventGroupId;
    }
    
    public Integer getEventType() {
        return eventType;
    }
    
    public void setEventType(Integer eventType) {
        this.eventType = eventType;
    }
    
    public Long getCallbackTs() {
        return callbackTs;
    }
    
    public void setCallbackTs(Long callbackTs) {
        this.callbackTs = callbackTs;
    }
    
    public EventInfo getEventInfo() {
        return eventInfo;
    }
    
    public void setEventInfo(EventInfo eventInfo) {
        this.eventInfo = eventInfo;
    }
    
    /**
     * 获取事件类型描述
     */
    public String getEventTypeDescription() {
        if (eventType == null) return "未知事件";
        
        switch (eventType) {
            case 901:
                return "AI任务开始状态回调";
            case 902:
                return "AI任务结束状态回调";
            case 903:
                return "完整语句回调";
            default:
                return "未知事件类型: " + eventType;
        }
    }
    
    /**
     * 事件信息内部类
     */
    public static class EventInfo {
        /**
         * 事件发生的Unix时间戳，单位为毫秒
         */
        @JsonProperty("EventMsTs")
        private Long eventMsTs;
        
        /**
         * AI任务ID
         */
        @JsonProperty("TaskId")
        private String taskId;
        
        /**
         * TRTC的房间ID
         */
        @JsonProperty("RoomId")
        private String roomId;
        
        /**
         * 房间ID类型
         * 0：表示数字房间号
         * 1：表示字符串房间号
         */
        @JsonProperty("RoomIdType")
        private Integer roomIdType;
        
        /**
         * 负载数据，根据事件类型不同内容不同
         */
        @JsonProperty("Payload")
        private Object payload;
        
        // Getters and Setters
        public Long getEventMsTs() {
            return eventMsTs;
        }
        
        public void setEventMsTs(Long eventMsTs) {
            this.eventMsTs = eventMsTs;
        }
        
        public String getTaskId() {
            return taskId;
        }
        
        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }
        
        public String getRoomId() {
            return roomId;
        }
        
        public void setRoomId(String roomId) {
            this.roomId = roomId;
        }
        
        public Integer getRoomIdType() {
            return roomIdType;
        }
        
        public void setRoomIdType(Integer roomIdType) {
            this.roomIdType = roomIdType;
        }
        
        public Object getPayload() {
            return payload;
        }
        
        public void setPayload(Object payload) {
            this.payload = payload;
        }
        
        /**
         * 获取房间ID类型描述
         */
        public String getRoomIdTypeDescription() {
            if (roomIdType == null) return "未知";
            return roomIdType == 0 ? "数字房间号" : "字符串房间号";
        }
    }
}
