package edu.ustb.eldercarebackend.service.volunteer;

import edu.ustb.eldercarebackend.entity.Volunteer;

public interface VolunteerService {
    // 已有的方法
    Volunteer getVolunteerByUserId(Integer userId);
    String updateVolunteerInfo(Volunteer volunteer);

    // 新增：根据ID查询志愿者（供修改时获取原记录）
    Volunteer getVolunteerById(Integer id);

    // 新增：根据ID更新志愿者信息（实际执行修改操作）
    int updateVolunteerById(Volunteer volunteer);

    String updateVolunteerAvatar(Integer userId, String avatarUrl);
}