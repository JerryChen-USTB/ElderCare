package edu.ustb.eldercarebackend.service.elderly.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.ustb.eldercarebackend.entity.Schedule;
import edu.ustb.eldercarebackend.mapper.ScheduleMapper;
import edu.ustb.eldercarebackend.service.elderly.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 日程管理服务实现类
 */
@Service
public class ScheduleServiceImpl implements ScheduleService {
    
    @Autowired
    private ScheduleMapper scheduleMapper;
    
    @Override
    public List<Schedule> getSchedulesByUserId(Integer userId) {
        QueryWrapper<Schedule> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                   .orderByAsc("time"); // 按时间排序
        return scheduleMapper.selectList(queryWrapper);
    }
    
    @Override
    public List<Schedule> getSchedulesByUserIdAndType(Integer userId, String type) {
        QueryWrapper<Schedule> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                   .eq("type", type)
                   .orderByAsc("time"); // 按时间排序
        return scheduleMapper.selectList(queryWrapper);
    }
    
    @Override
    public boolean createSchedule(Schedule schedule) {
        try {
            System.out.println("调用createSchedule");
            // 设置创建时间和更新时间
            Date now = new Date();
            schedule.setCreatedAt(now);
            schedule.setUpdatedAt(now);
            
            // 如果状态为空，设置为待处理
            if (schedule.getStatus() == null || schedule.getStatus().isEmpty()) {
                schedule.setStatus("pending");
            }
            
            // 处理重复类型
            String repeatType = schedule.getRepeatType();
            if (repeatType == null || "none".equals(repeatType)) {
                // 一次性日程，parent_schedule_id保持为NULL
                schedule.setParentScheduleId(null);
                int result = scheduleMapper.insert(schedule);
                return result > 0;
            } else {
                // 重复日程，先创建父日程
                schedule.setParentScheduleId(-1); // 标记为父日程
                int result = scheduleMapper.insert(schedule);
                
                if (result > 0) {
                    // 获取插入后的父日程ID
                    Integer parentId = schedule.getId();
                    
                    // 创建一年内的重复日程
                    createRepeatSchedules(schedule, parentId, repeatType);
                    
                    return true;
                }
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public boolean updateScheduleStatus(Integer id, String status) {
        try {
            Schedule schedule = new Schedule();
            schedule.setId(id);
            schedule.setStatus(status);
            schedule.setUpdatedAt(new Date());
            
            int result = scheduleMapper.updateById(schedule);
            return result > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public boolean updateSchedule(Schedule schedule) {
        try {
            // 设置更新时间
            schedule.setUpdatedAt(new Date());
            
            int result = scheduleMapper.updateById(schedule);
            return result > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public boolean deleteSchedule(Integer id) {
        try {
            int result = scheduleMapper.deleteById(id);
            return result > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public boolean deleteCurrentScheduleOnly(Integer id) {
        try {
            Schedule currentSchedule = scheduleMapper.selectById(id);
            if (currentSchedule == null) {
                return false;
            }
            
            // 如果是父日程（parent_schedule_id = -1），需要特殊处理
            if (currentSchedule.getParentScheduleId() != null && currentSchedule.getParentScheduleId() == -1) {
                // 查找第一个子日程，将其提升为父日程
                QueryWrapper<Schedule> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("parent_schedule_id", id)
                           .orderByAsc("time")
                           .last("LIMIT 1");
                
                Schedule firstChild = scheduleMapper.selectOne(queryWrapper);
                if (firstChild != null) {
                    // 将第一个子日程设为父日程
                    firstChild.setParentScheduleId(-1);
                    scheduleMapper.updateById(firstChild);
                    
                    // 更新其他子日程的parent_schedule_id
                    QueryWrapper<Schedule> updateWrapper = new QueryWrapper<>();
                    updateWrapper.eq("parent_schedule_id", id);
                    List<Schedule> otherChildren = scheduleMapper.selectList(updateWrapper);
                    
                    for (Schedule child : otherChildren) {
                        if (!child.getId().equals(firstChild.getId())) {
                            child.setParentScheduleId(firstChild.getId());
                            scheduleMapper.updateById(child);
                        }
                    }
                }
            }
            
            // 删除当前日程
            int result = scheduleMapper.deleteById(id);
            return result > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public boolean deleteAllRelatedSchedules(Integer id) {
        try {
            Schedule currentSchedule = scheduleMapper.selectById(id);
            if (currentSchedule == null) {
                return false;
            }
            
            Integer parentId;
            if (currentSchedule.getParentScheduleId() != null && currentSchedule.getParentScheduleId() == -1) {
                // 当前是父日程
                parentId = id;
            } else {
                // 当前是子日程，找到父日程
                parentId = currentSchedule.getParentScheduleId();
            }
            
            // 删除所有相关的子日程
            QueryWrapper<Schedule> deleteChildrenWrapper = new QueryWrapper<>();
            deleteChildrenWrapper.eq("parent_schedule_id", parentId);
            scheduleMapper.delete(deleteChildrenWrapper);
            
            // 删除父日程
            scheduleMapper.deleteById(parentId);
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public Schedule getScheduleById(Integer id) {
        try {
            return scheduleMapper.selectById(id);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    @Override
    public List<Schedule> getUpcomingSchedulesByUserId(Integer userId, int limit) {
        try {
            QueryWrapper<Schedule> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", userId)
                       .ge("time", new Date()) // 大于等于当前时间
                       .orderByAsc("time") // 按时间升序排列
                       .last("LIMIT " + limit); // 限制返回数量
            return scheduleMapper.selectList(queryWrapper);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * 创建重复日程
     * @param parentSchedule 父日程
     * @param parentId 父日程ID
     * @param repeatType 重复类型
     */
    private void createRepeatSchedules(Schedule parentSchedule, Integer parentId, String repeatType) {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parentSchedule.getTime());
            
            Calendar endDate = Calendar.getInstance();
            endDate.add(Calendar.YEAR, 1); // 一年范围
            
            int count = 0;
            int maxCount = 365; // 防止无限循环
            
            while (calendar.before(endDate) && count < maxCount) {
                // 根据重复类型添加时间间隔
                switch (repeatType) {
                    case "daily":
                        calendar.add(Calendar.DAY_OF_MONTH, 1);
                        break;
                    case "weekly":
                        calendar.add(Calendar.WEEK_OF_YEAR, 1);
                        break;
                    case "monthly":
                        calendar.add(Calendar.MONTH, 1);
                        break;
                    default:
                        return; // 不支持的重复类型
                }
                
                if (calendar.before(endDate)) {
                    // 创建子日程
                    Schedule childSchedule = new Schedule();
                    childSchedule.setUserId(parentSchedule.getUserId());
                    childSchedule.setType(parentSchedule.getType());
                    childSchedule.setTime(calendar.getTime());
                    childSchedule.setContent(parentSchedule.getContent());
                    childSchedule.setLocation(parentSchedule.getLocation());
                    childSchedule.setStatus("pending"); // 子日程默认为待处理
                    childSchedule.setReminderTime(parentSchedule.getReminderTime());
                    childSchedule.setRepeatType(repeatType);
                    childSchedule.setParentScheduleId(parentId); // 设置父日程ID
                    childSchedule.setCreatedAt(new Date());
                    childSchedule.setUpdatedAt(new Date());
                    
                    scheduleMapper.insert(childSchedule);
                }
                
                count++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
