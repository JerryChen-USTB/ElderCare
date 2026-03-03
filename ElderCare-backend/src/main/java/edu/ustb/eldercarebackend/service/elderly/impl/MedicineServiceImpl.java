package edu.ustb.eldercarebackend.service.elderly.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.ustb.eldercarebackend.entity.Medication;
import edu.ustb.eldercarebackend.entity.guardian.MedicationDTO;
import edu.ustb.eldercarebackend.mapper.MedicationMapper;
import edu.ustb.eldercarebackend.service.elderly.MedicineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MedicineServiceImpl implements MedicineService {
    @Autowired
    private MedicationMapper medicationMapper;


    @Override
    public List<MedicationDTO> findMedicationByElderUserId(Integer userId) {
        // 1. 构建查询条件：按老人关联的 user_id 筛选，按创建时间倒序（最新记录在前）
        QueryWrapper<Medication> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId) // 匹配 medications 表的 user_id 字段
                .orderByDesc("created_at"); // 按创建时间倒序

        // 2. 执行数据库查询，获取原始 Medication 列表
        List<Medication> medicationList = medicationMapper.selectList(queryWrapper);

        // 3. 将 Medication 实体列表转换为 MedicationDTO 列表（调用 DTO 自带的转换方法）
        return medicationList.stream()
                .map(MedicationDTO::fromEntity) // 每个实体转 DTO
                .collect(Collectors.toList()); // 收集为 DTO 列表
    }
}
