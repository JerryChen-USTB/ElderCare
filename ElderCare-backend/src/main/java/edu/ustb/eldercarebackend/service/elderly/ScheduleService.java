package edu.ustb.eldercarebackend.service.elderly;

import edu.ustb.eldercarebackend.entity.Schedule;
import java.util.List;

/**
 * 日程管理服务接口
 */
public interface ScheduleService {
    
    /**
     * 根据用户ID获取日程列表
     * @param userId 用户ID
     * @return 日程列表
     */
    List<Schedule> getSchedulesByUserId(Integer userId);
    
    /**
     * 根据用户ID和类型获取日程列表
     * @param userId 用户ID
     * @param type 日程类型
     * @return 日程列表
     */
    List<Schedule> getSchedulesByUserIdAndType(Integer userId, String type);
    
    /**
     * 创建新日程
     * @param schedule 日程对象
     * @return 是否创建成功
     */
    boolean createSchedule(Schedule schedule);
    
    /**
     * 更新日程状态
     * @param id 日程ID
     * @param status 新状态
     * @return 是否更新成功
     */
    boolean updateScheduleStatus(Integer id, String status);
    
    /**
     * 更新日程信息
     * @param schedule 日程对象
     * @return 是否更新成功
     */
    boolean updateSchedule(Schedule schedule);
    
    /**
     * 删除日程
     * @param id 日程ID
     * @return 是否删除成功
     */
    boolean deleteSchedule(Integer id);
    
    /**
     * 仅删除当前日程（重复日程场景）
     * @param id 日程ID
     * @return 是否删除成功
     */
    boolean deleteCurrentScheduleOnly(Integer id);
    
    /**
     * 删除所有相关重复日程
     * @param id 日程ID
     * @return 是否删除成功
     */
    boolean deleteAllRelatedSchedules(Integer id);
    
    /**
     * 根据ID获取日程详情
     * @param id 日程ID
     * @return 日程对象
     */
    Schedule getScheduleById(Integer id);
    
    /**
     * 获取用户未来的日程列表（从当前时间开始，按时间排序）
     * @param userId 用户ID
     * @param limit 限制返回的数量
     * @return 未来日程列表
     */
    List<Schedule> getUpcomingSchedulesByUserId(Integer userId, int limit);
}
