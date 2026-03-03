package edu.ustb.eldercarebackend;

import edu.ustb.eldercarebackend.util.ScheduleTool;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.lang.reflect.Method;

/**
 * 智能时间解析功能测试
 */
@SpringBootTest
public class DateTimeParseTest {

    @Test
    public void testSmartDateTimeParsing() {
        ScheduleTool scheduleTool = new ScheduleTool();
        
        // 获取当前时间信息
        Calendar currentCal = Calendar.getInstance();
        int currentYear = currentCal.get(Calendar.YEAR);
        int currentMonth = currentCal.get(Calendar.MONTH) + 1;
        int currentDay = currentCal.get(Calendar.DAY_OF_MONTH);
        
        System.out.println("=== 智能时间解析测试 ===");
        System.out.println("当前时间: " + currentYear + "-" + currentMonth + "-" + currentDay);
        
        // 测试用例
        String[] testCases = {
            "2025-01-15 14:30",    // 完整格式
            "2025/01/15 14:30",    // 斜杠格式
            "01-15 14:30",         // 缺少年份
            "01/15 14:30",         // 缺少年份斜杠
            "15 14:30",            // 只有日期（需要当前月份合适时测试）
            "2025-12-25",          // 只有日期
            "12-25",               // 月日格式
        };
        
        for (String testCase : testCases) {
            try {
                // 使用反射调用私有方法进行测试
                Method parseMethod = ScheduleTool.class.getDeclaredMethod("parseDateTime", String.class);
                parseMethod.setAccessible(true);
                
                Date parsedDate = (Date) parseMethod.invoke(scheduleTool, testCase);
                
                SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm E");
                System.out.println("输入: '" + testCase + "' -> 解析结果: " + displayFormat.format(parsedDate));
                
                // 验证解析结果不在过去
                if (parsedDate.before(new Date())) {
                    System.out.println("⚠️  警告: 解析结果在过去，可能需要调整逻辑");
                } else {
                    System.out.println("✅ 解析结果在未来，符合预期");
                }
                
            } catch (Exception e) {
                System.out.println("❌ 解析失败: '" + testCase + "' - " + e.getMessage());
            }
            System.out.println("----------------------------------------");
        }
    }
    
    @Test 
    public void testEdgeCases() {
        System.out.println("\n=== 边界情况测试 ===");
        
        ScheduleTool scheduleTool = new ScheduleTool();
        
        // 边界测试用例
        String[] edgeCases = {
            "",                    // 空字符串
            "   ",                 // 空白字符
            "invalid format",      // 无效格式
            "13-32 25:70",        // 无效日期时间
            "02-29 10:00",        // 闰年测试（需要看当前年份）
        };
        
        for (String testCase : edgeCases) {
            try {
                Method parseMethod = ScheduleTool.class.getDeclaredMethod("parseDateTime", String.class);
                parseMethod.setAccessible(true);
                
                Date parsedDate = (Date) parseMethod.invoke(scheduleTool, testCase);
                
                SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                System.out.println("边界测试: '" + testCase + "' -> " + displayFormat.format(parsedDate));
                
            } catch (Exception e) {
                System.out.println("预期的解析失败: '" + testCase + "' - " + e.getCause().getMessage());
            }
        }
    }
    
    @Test
    public void testRelativeDateScenarios() {
        System.out.println("\n=== 相对日期场景模拟 ===");
        
        // 模拟用户常见的输入场景
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat monthDay = new SimpleDateFormat("MM-dd");
        SimpleDateFormat dayOnly = new SimpleDateFormat("dd");
        
        // 明天
        cal.add(Calendar.DAY_OF_MONTH, 1);
        String tomorrow = monthDay.format(cal.getTime()) + " 14:00";
        System.out.println("明天场景测试: " + tomorrow);
        
        // 下个月同一天
        cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 1);
        String nextMonth = monthDay.format(cal.getTime()) + " 10:00";
        System.out.println("下个月场景测试: " + nextMonth);
        
        // 下周同一天
        cal = Calendar.getInstance();
        cal.add(Calendar.WEEK_OF_YEAR, 1);
        String nextWeek = dayOnly.format(cal.getTime()) + " 09:00";
        System.out.println("下周场景测试: " + nextWeek);
        
        System.out.println("注意: 这些是模拟场景，实际AI应该将相对时间转换为具体日期传递给工具");
    }
}
