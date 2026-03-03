package edu.ustb.eldercarebackend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private Integer id;
    private Integer userId; // 接收用户ID
    private Integer senderId; // 发送者ID
    private String notificationType; // 对应enum('reminder','alert','message','update','other')
    private String title;
    private String content;
    private Boolean isRead; // 对应tinyint(1)
    private Integer relatedId; // 关联ID
    private Date createdAt; // 对应timestamp类型
    private Date updatedAt; // 对应timestamp类型
}