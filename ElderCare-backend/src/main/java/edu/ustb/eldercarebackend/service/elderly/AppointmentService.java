package edu.ustb.eldercarebackend.service.elderly;

import edu.ustb.eldercarebackend.entity.Appointment;

import java.util.List;

/**
 * 预约服务接口
 */
public interface AppointmentService {

    /**
     * 创建预约
     * @param appointment 预约信息
     * @return 创建的预约对象
     */
    Appointment createAppointment(Appointment appointment);

    /**
     * 根据老人ID获取预约列表
     * @param elderId 老人ID
     * @return 预约列表
     */
    List<Appointment> getAppointmentsByElderId(Integer elderId);

    /**
     * 根据预约ID获取预约详情
     * @param appointmentId 预约ID
     * @return 预约详情
     */
    Appointment getAppointmentById(Integer appointmentId);

    /**
     * 取消预约
     * @param appointmentId 预约ID
     * @return 是否取消成功
     */
    boolean cancelAppointment(Integer appointmentId);

    /**
     * 获取所有待接单的预约
     * @return 待接单预约列表
     */
    List<Appointment> getPendingAppointments();

    /**
     * 验证预约数据
     * @param appointment 预约数据
     * @return 验证结果，null表示验证通过，否则返回错误信息
     */
    String validateAppointment(Appointment appointment);

    /**
     * 志愿者接单
     * @param appointmentId 预约ID
     * @param volunteerId 志愿者ID
     * @return 是否接单成功
     */
    boolean acceptAppointment(Integer appointmentId, Integer volunteerId);

    /**
     * 更新预约状态
     * @param appointmentId 预约ID
     * @param status 新状态
     * @return 是否更新成功
     */
    boolean updateAppointmentStatus(Integer appointmentId, String status);

    /**
     * 同步预约到日程管理
     * @param appointment 预约信息
     * @return 是否同步成功
     */
    boolean syncAppointmentToSchedule(Appointment appointment);

    /**
     * 根据预约ID同步更新日程状态
     * @param appointmentId 预约ID
     * @param newStatus 新状态
     * @return 是否更新成功
     */
    boolean updateScheduleByAppointmentId(Integer appointmentId, String newStatus);

    /**
     * 查找需要标记为超时的预约
     * 查找所有在开始时间前一天晚上10点之前且状态仍为pending的预约
     * @return 需要更新为超时状态的预约列表
     */
    List<Appointment> findTimeoutAppointments();

    /**
     * 批量更新预约状态为超时
     * @param appointments 需要更新的预约列表
     * @return 更新成功的记录数
     */
    int updateAppointmentsToTimeout(List<Appointment> appointments);
}
