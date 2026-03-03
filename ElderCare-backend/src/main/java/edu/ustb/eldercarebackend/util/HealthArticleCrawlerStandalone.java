package edu.ustb.eldercarebackend.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 人民网健康生活栏目文章爬虫工具类（独立版本）
 * 不依赖Spring容器，可以独立运行
 */
public class HealthArticleCrawlerStandalone {
    
    // 全局配置
    private static final int ARTICLES_PER_CATEGORY = 10; // 每个板块爬取的文章数量
    private static final String DOMAIN = "http://health.people.com.cn";
    private static final String BASE_SAVE_DIR = "src/main/resources/rag_sources/";
    
    // 用户代理，模拟浏览器访问
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
    
    // 健康板块配置
    private static final HealthCategory[] HEALTH_CATEGORIES = {
        new HealthCategory("保健养生", "http://health.people.com.cn/GB/408572/index.html", "HealthArticles/"),
        new HealthCategory("健身", "http://health.people.com.cn/GB/408571/index.html", "FitnessArticles/"),
        new HealthCategory("心理健康", "http://health.people.com.cn/GB/408576/index.html", "MentalHealthArticles/"),
        new HealthCategory("饮食", "http://health.people.com.cn/GB/408569/408719/", "FoodArticles/")
    };
    
    // 内部类：健康板块配置
    private static class HealthCategory {
        final String name;
        final String url;
        final String saveDir;
        
        HealthCategory(String name, String url, String saveDir) {
            this.name = name;
            this.url = url;
            this.saveDir = BASE_SAVE_DIR + saveDir;
        }
    }
    
    /**
     * 爬取所有健康板块的文章
     */
    public void crawlHealthArticles() {
        System.out.println("=".repeat(80));
        System.out.println("开始爬取人民网健康生活栏目文章...");
        System.out.println("共 " + HEALTH_CATEGORIES.length + " 个板块，每个板块爬取前 " + ARTICLES_PER_CATEGORY + " 篇文章");
        System.out.println("=".repeat(80));
        
        int totalCrawledCount = 0;
        
        for (int i = 0; i < HEALTH_CATEGORIES.length; i++) {
            HealthCategory category = HEALTH_CATEGORIES[i];
            
            System.out.println("\n" + "█".repeat(60));
            System.out.println("正在爬取板块 [" + (i + 1) + "/" + HEALTH_CATEGORIES.length + "]: " + category.name);
            System.out.println("目标URL: " + category.url);
            System.out.println("保存目录: " + category.saveDir);
            System.out.println("█".repeat(60));
            
            try {
                int categoryCount = crawlCategoryArticles(category);
                totalCrawledCount += categoryCount;
                
                System.out.println("✅ 板块 \"" + category.name + "\" 爬取完成！共成功爬取 " + categoryCount + " 篇文章");
                
                // // 板块间添加延迟
                // if (i < HEALTH_CATEGORIES.length - 1) {
                //     System.out.println("等待 3 秒后继续下一个板块...");
                //     Thread.sleep(3000);
                // }
                
            } catch (Exception e) {
                System.err.println("❌ 爬取板块 \"" + category.name + "\" 时发生错误: " + e.getMessage());
                continue;
            }
        }
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🎉 全部爬取任务完成！");
        System.out.println("📊 总计成功爬取 " + totalCrawledCount + " 篇健康文章");
        System.out.println("📁 文章已分类保存到各自的目录中");
        System.out.println("=".repeat(80));
    }
    
    /**
     * 爬取单个板块的文章
     */
    private int crawlCategoryArticles(HealthCategory category) throws Exception {
        // 创建保存目录
        createSaveDirectory(category.saveDir);
        
        // 获取板块主页面
        Document mainPage = Jsoup.connect(category.url)
                .userAgent(USER_AGENT)
                .timeout(10000)
                .get();
        
        System.out.println("页面获取成功，正在解析文章链接...");
        
        // 查找文章链接 - 根据人民网页面结构进行选择
        Elements articleElements = mainPage.select("a[href*='/n1/']");
        
        if (articleElements.isEmpty()) {
            // 备用选择器 - 查找带有标题的链接
            articleElements = mainPage.select("a[title]");
            System.out.println("使用备用选择器查找文章链接");
        }
        
        if (articleElements.isEmpty()) {
            // 再备用选择器 - 查找所有包含文字的链接
            articleElements = mainPage.select("a:not([href^='javascript']):not([href^='#'])");
            System.out.println("使用通用选择器查找文章链接");
        }
        
        System.out.println("找到 " + articleElements.size() + " 个可能的文章链接");
        
        int crawledCount = 0;
        
        for (Element element : articleElements) {
            if (crawledCount >= ARTICLES_PER_CATEGORY) {
                break;
            }
            
            String href = element.attr("href");
            String title = element.text().trim();
            
            // 跳过空标题或无效链接
            if (title.isEmpty() || href.isEmpty() || title.length() < 5) {
                continue;
            }
            
            // 跳过明显不是文章的链接
            if (href.contains("javascript") || href.startsWith("#") || 
                title.contains("更多") || title.contains("下一页") || 
                title.length() < 8) {
                continue;
            }
            
            // 构建完整URL
            String fullUrl = href.startsWith("http") ? href : DOMAIN + href;
            
            try {
                System.out.println("\n" + "-".repeat(50));
                System.out.println("正在爬取第 " + (crawledCount + 1) + " 篇文章");
                System.out.println("标题: " + title);
                System.out.println("URL: " + fullUrl);
                System.out.println("-".repeat(50));
                
                // 爬取单篇文章
                crawlSingleArticle(fullUrl, title, crawledCount + 1, category);
                crawledCount++;
                System.out.println("✓ 成功爬取并保存第 " + crawledCount + " 篇文章");
                
                // // 文章间添加延迟，避免请求过于频繁
                // if (crawledCount < ARTICLES_PER_CATEGORY) {
                //     Thread.sleep(1500);
                // }
                
            } catch (Exception e) {
                System.out.println("✗ 爬取文章失败 [" + fullUrl + "]: " + e.getMessage());
                continue;
            }
        }
        
        return crawledCount;
    }
    
    /**
     * 爬取单篇文章内容
     */
    private void crawlSingleArticle(String url, String title, int index, HealthCategory category) throws IOException, InterruptedException {
        Document articlePage = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(15000)
                .get();
        
        // 提取文章内容 - 基于HTML结构精准提取
        String content = extractArticleContent(articlePage, title);
        
        if (content.isEmpty()) {
            throw new IOException("无法获取页面内容");
        }
        
        // 清理文件名中的非法字符
        String safeTitle = title.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (safeTitle.length() > 50) {
            safeTitle = safeTitle.substring(0, 50);
        }
        
        // 保存文章到对应分类的目录
        String fileName = String.format("article_%02d_%s.txt", index, safeTitle);
        saveArticleToFile(fileName, title, content, url, category);
    }
    
    /**
     * 从文章页面精准提取正文内容（基于真实HTML结构）
     */
    private String extractArticleContent(Document doc, String title) {
        StringBuilder content = new StringBuilder();
        
        try {
            // 1. 提取文章标题
            Elements titleElements = doc.select(".articleCont .title h2");
            String articleTitle = "";
            if (!titleElements.isEmpty()) {
                articleTitle = titleElements.first().text().trim();
                content.append("文章标题: ").append(articleTitle).append("\n");
            }
            
            // 2. 提取发布时间和来源
            Elements timeElements = doc.select(".artOri");
            if (!timeElements.isEmpty()) {
                String timeAndSource = timeElements.first().text().trim();
                // 提取日期时间部分 (格式: 2025年09月09日08:52)
                if (timeAndSource.contains("年") && timeAndSource.contains("月") && timeAndSource.contains("日")) {
                    int sourceIndex = timeAndSource.indexOf("来源：");
                    String timeOnly = sourceIndex > 0 ? timeAndSource.substring(0, sourceIndex).trim() : timeAndSource;
                    content.append("发布时间: ").append(timeOnly).append("\n");
                    
                    // 提取来源信息
                    if (sourceIndex > 0) {
                        String source = timeAndSource.substring(sourceIndex).replaceAll("\\d+", "").trim();
                        content.append("文章来源: ").append(source).append("\n");
                    }
                }
                content.append("\n");
            }
            
            // 3. 提取正文内容
            Elements contentElements = doc.select(".artDet p");
            if (!contentElements.isEmpty()) {
                content.append("正文内容:\n");                
                for (Element paragraph : contentElements) {
                    String paragraphText = paragraph.text().trim();
                    if (!paragraphText.isEmpty() && paragraphText.length() > 10) {
                        content.append(paragraphText);
                        // 确保段落以句号结尾
                        if (!paragraphText.endsWith("。") && !paragraphText.endsWith("！") && !paragraphText.endsWith("？")) {
                            content.append("。");
                        }
                        content.append("\n");
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("提取文章内容时出错: " + e.getMessage());
            // 如果精准提取失败，使用备用方法
            return extractContentFallback(doc);
        }
        
        String result = content.toString().trim();
        
        // 检查是否成功提取到内容
        if (result.length() < 100) {
            System.out.println("警告: 使用精准提取方法获取的内容较少，尝试备用方法...");
            return extractContentFallback(doc);
        }
        
        return result;
    }
    
    /**
     * 备用内容提取方法
     */
    private String extractContentFallback(Document doc) {
        StringBuilder content = new StringBuilder();
        content.append("=== 使用备用提取方法 ===\n\n");
        
        // 尝试提取页面中的段落内容
        Elements paragraphs = doc.select("p");
        for (Element p : paragraphs) {
            String text = p.text().trim();
            if (text.length() > 20 && !text.contains("登录") && !text.contains("注册") 
                && !text.contains("版权所有") && !text.contains("分享到")) {
                content.append(text);
                if (!text.endsWith("。") && !text.endsWith("！") && !text.endsWith("？")) {
                    content.append("。");
                }
                content.append("\n\n");
            }
        }
        
        return content.toString().trim();
    }
    
    /**
     * 保存文章到文件
     */
    private void saveArticleToFile(String fileName, String title, String content, String url, HealthCategory category) throws IOException {
        File file = new File(category.saveDir + fileName);
        
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            // 写入正文内容
            writer.write(content);
        }
        
        System.out.println("✓ 文章已保存到: " + file.getAbsolutePath());
        System.out.println("📁 板块: " + category.name);
        System.out.println("📄 文件大小: " + (content.length() / 1024.0) + " KB (" + content.length() + " 字符)");
        System.out.println("📝 内容预览: " + (content.length() > 100 ? content.substring(0, 100) + "..." : content));
    }
    
    /**
     * 创建保存目录
     */
    private void createSaveDirectory(String saveDir) {
        File dir = new File(saveDir);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                System.out.println("📁 创建保存目录: " + dir.getAbsolutePath());
            } else {
                System.out.println("⚠️ 警告: 无法创建保存目录: " + dir.getAbsolutePath());
            }
        }
    }
    
    /**
     * 清理所有分类目录中的旧文章文件
     */
    public void cleanOldArticles() {
        System.out.println("🧹 开始清理所有分类目录中的旧文章文件...");
        
        int totalDeleted = 0;
        
        for (HealthCategory category : HEALTH_CATEGORIES) {
            File dir = new File(category.saveDir);
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles((d, name) -> name.startsWith("article_") && name.endsWith(".txt"));
                if (files != null && files.length > 0) {
                    System.out.println("🗂️ 正在清理 \"" + category.name + "\" 目录中的 " + files.length + " 个旧文件...");
                    int deletedCount = 0;
                    for (File file : files) {
                        if (file.delete()) {
                            deletedCount++;
                            totalDeleted++;
                            System.out.println("  ✓ 删除: " + file.getName());
                        }
                    }
                    System.out.println("  📊 \"" + category.name + "\" 目录清理完成，删除了 " + deletedCount + " 个文件");
                } else {
                    System.out.println("  ℹ️ \"" + category.name + "\" 目录中没有发现旧文章文件");
                }
            } else {
                System.out.println("  📂 \"" + category.name + "\" 目录不存在或为空");
            }
        }
        
        System.out.println("✅ 清理完成！总共删除了 " + totalDeleted + " 个旧文章文件");
    }
}
