package edu.ustb.eldercarebackend.util;

/**
 * 爬虫独立运行工具类
 * 可以独立运行，不依赖Spring容器
 */
public class CrawlerRunner {
    
    public static void main(String[] args) {
        System.out.println("=== 人民网健康文章爬虫启动 ===");
        
        try {
            // 创建爬虫实例
            HealthArticleCrawlerStandalone crawler = new HealthArticleCrawlerStandalone();
            
            // 清理旧文章（可选）
            System.out.println("正在清理旧文章文件...");
            crawler.cleanOldArticles();
            
            // 开始爬取文章
            crawler.crawlHealthArticles();
            
            System.out.println("=== 爬虫任务完成 ===");
            
        } catch (Exception e) {
            System.err.println("爬虫运行出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
