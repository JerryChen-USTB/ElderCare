package edu.ustb.eldercarebackend.service.knowledge;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 混合内容检索器
 * 同时从公共知识库和用户私有知识库检索内容，并进行智能合并
 */
public class HybridContentRetriever implements ContentRetriever {
    
    private static final Logger logger = LoggerFactory.getLogger(HybridContentRetriever.class);
    
    private final ContentRetriever globalRetriever;
    private final ContentRetriever userRetriever;
    
    // 权重配置
    private final double globalWeight;
    private final double privateWeight;
    private final int totalMaxResults;
    
    /**
     * 构造函数
     * @param globalRetriever 全局检索器
     * @param userRetriever 用户私有检索器
     */
    public HybridContentRetriever(ContentRetriever globalRetriever, 
                                  ContentRetriever userRetriever) {
        this(globalRetriever, userRetriever, 0.7, 1.3, 10);
    }
    
    /**
     * 完整构造函数
     * @param globalRetriever 全局检索器
     * @param userRetriever 用户私有检索器
     * @param globalWeight 公共知识权重
     * @param privateWeight 私有知识权重
     * @param totalMaxResults 总结果数量
     */
    public HybridContentRetriever(ContentRetriever globalRetriever, 
                                  ContentRetriever userRetriever,
                                  double globalWeight,
                                  double privateWeight,
                                  int totalMaxResults) {
        this.globalRetriever = globalRetriever;
        this.userRetriever = userRetriever;
        this.globalWeight = globalWeight;
        this.privateWeight = privateWeight;
        this.totalMaxResults = totalMaxResults;
    }
    
    @Override
    public List<Content> retrieve(Query query) {
        logger.debug("🔍 混合检索查询: {}", query.text());
        
        List<ScoredContent> allResults = new ArrayList<>();
        
        try {
            // 1. 从公共知识库检索
            if (globalRetriever != null) {
                List<Content> globalResults = globalRetriever.retrieve(query);
                logger.debug("📚 公共知识库检索到 {} 条结果", globalResults.size());
                
                for (Content content : globalResults) {
                    double score = extractScore(content);
                    double weightedScore = score * globalWeight;
                    allResults.add(new ScoredContent(content, weightedScore, "公共知识库"));
                }
            }
            
            // 2. 从用户私有知识库检索
            if (userRetriever != null) {
                List<Content> userResults = userRetriever.retrieve(query);
                logger.debug("👤 私有知识库检索到 {} 条结果", userResults.size());
                
                for (Content content : userResults) {
                    double score = extractScore(content);
                    double weightedScore = score * privateWeight;
                    allResults.add(new ScoredContent(content, weightedScore, "您的知识库"));
                }
            }
            
            // 3. 按加权分数排序
            allResults.sort(Comparator.comparingDouble(ScoredContent::getScore).reversed());
            
            // 4. 取前N个结果
            List<Content> finalResults = allResults.stream()
                .limit(totalMaxResults)
                .map(sc -> {
                    // 在文本前添加来源标记
                    Content original = sc.getContent();
                    String markedText = String.format("【%s】%s", 
                        sc.getSource(), original.textSegment().text());
                    
                    return Content.from(markedText);
                })
                .collect(Collectors.toList());
            
            logger.info("✅ 混合检索完成: 公共 + 私有 → 总计 {} 条结果", finalResults.size());
            
            return finalResults;
            
        } catch (Exception e) {
            logger.error("❌ 混合检索失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 提取内容的相似度分数
     * 
     * 说明：
     * - EmbeddingStoreContentRetriever 在检索时已经过滤了低分文档（minScore=0.8）
     * - 能返回到这里的文档都是相似度 ≥ 0.8 的高质量结果
     * - 由于无法直接获取原始分数，这里使用固定高分
     * - 主要用于加权计算：公共知识×0.7，私有知识×1.3
     * 
     * @param content 检索返回的内容
     * @return 估算的相似度分数（范围0.8-1.0）
     */
    private double extractScore(Content content) {
        // 由于能通过 minScore=0.8 筛选的文档都是高质量的
        // 这里假设所有返回的文档分数在 0.8-1.0 之间
        // 使用统一的高分 0.9，让权重系数起主导作用
        return 0.9;
    }
    
    /**
     * 内部类：带分数的内容
     */
    private static class ScoredContent {
        private final Content content;
        private final double score;
        private final String source;
        
        public ScoredContent(Content content, double score, String source) {
            this.content = content;
            this.score = score;
            this.source = source;
        }
        
        public Content getContent() {
            return content;
        }
        
        public double getScore() {
            return score;
        }
        
        public String getSource() {
            return source;
        }
    }
}

