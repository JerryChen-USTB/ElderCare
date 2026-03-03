package edu.ustb.eldercarebackend.service.volunteer;


import com.baomidou.mybatisplus.extension.service.IService;
import edu.ustb.eldercarebackend.entity.Assistance;

import java.util.List;

public interface VolunteerAssistanceService extends IService<Assistance> {
    // 新增：按 userId 查询该用户所有远程协助记录（服务记录用）
    List<Assistance> getByUserId(Integer userId);
}