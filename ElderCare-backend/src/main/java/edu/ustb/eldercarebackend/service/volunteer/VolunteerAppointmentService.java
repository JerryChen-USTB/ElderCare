package edu.ustb.eldercarebackend.service.volunteer;


import edu.ustb.eldercarebackend.entity.Appointment;

import java.util.Date;
import java.util.List;

public interface VolunteerAppointmentService {
        List<Appointment> getByUserIdAndDate(Integer userId, Date targetDate);
    boolean cancelAppointment(Integer appointmentId, Integer userId);

    List<Appointment> getPendingAppointments();

    void acceptAppointment(Integer appointmentId, Integer volunteerUserId);
    // 新增：按 userId 查询该用户所有普通预约记录（服务记录用）
    List<Appointment> getByUserId(Integer userId);
    }
