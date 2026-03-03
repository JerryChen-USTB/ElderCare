package edu.ustb.eldercarebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.ustb.eldercarebackend.entity.Message;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息管理数据访问层
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}