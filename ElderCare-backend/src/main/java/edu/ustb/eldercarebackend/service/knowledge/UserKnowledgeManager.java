package edu.ustb.eldercarebackend.service.knowledge;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * 用户知识库管理器
 * 负责按需加载和管理用户私有知识库
 */
@Service
public class UserKnowledgeManager {
    
    private static final Logger logger = LoggerFactory.getLogger(UserKnowledgeManager.class);
    
    @Autowired
    private EmbeddingModel embeddingModel;
    
    @Value("${app.user-data.root-path}")
    private String userDataRootPath;
    
    @Value("${app.user-data.users-dir}")
    private String usersDir;
    
    // 用户向量存储缓存：userId -> EmbeddingStore
    private final Map<String, EmbeddingStore<TextSegment>> userStoreCache = new ConcurrentHashMap<>();
    
    // 用户检索器缓存：userId -> ContentRetriever
    private final Map<String, ContentRetriever> userRetrieverCache = new ConcurrentHashMap<>();
    
    /**
     * 异步加载用户知识库（用户登录时调用）
     */
    @Async
    public void loadUserKnowledgeAsync(Long userId) {
        String userKey = userId.toString();
        logger.info("🔄 异步加载用户知识库: user_{}", userKey);
        
        try {
            getUserRetriever(userKey);
            logger.info("✅ 用户知识库加载完成: user_{}", userKey);
        } catch (Exception e) {
            logger.error("❌ 用户知识库加载失败: user_{}", userKey, e);
        }
    }
    
    /**
     * 获取用户检索器（懒加载 + 缓存）
     */
    public ContentRetriever getUserRetriever(String userId) {
        // 检查缓存
        if (userRetrieverCache.containsKey(userId)) {
            logger.debug("📦 使用缓存的用户检索器: user_{}", userId);
            return userRetrieverCache.get(userId);
        }
        
        // 不存在，创建新的
        logger.info("🆕 为用户创建新的知识库检索器: user_{}", userId);
        
        // 获取或创建用户向量存储
        EmbeddingStore<TextSegment> userStore = getOrCreateUserStore(userId);
        
        // 创建检索器
        ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
            .embeddingStore(userStore)
            .embeddingModel(embeddingModel)
            .minScore(0.7)  // 相似度阈值：只返回相似度 ≥ 70% 的文档
            .maxResults(5)
            .build();
        
        // 缓存
        userRetrieverCache.put(userId, retriever);
        
        return retriever;
    }
    
    /**
     * 获取或创建用户向量存储
     */
    private EmbeddingStore<TextSegment> getOrCreateUserStore(String userId) {
        // 检查缓存
        if (userStoreCache.containsKey(userId)) {
            return userStoreCache.get(userId);
        }
        
        // 加载用户文档
        List<Document> userDocuments = loadUserDocuments(userId);
        
        // 创建向量存储
        InMemoryEmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();
        
        if (!userDocuments.isEmpty()) {
            logger.info("📚 用户 user_{} 有 {} 个私有文档", userId, userDocuments.size());
            
            // 文档向量化
            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingStore(store)
                .embeddingModel(embeddingModel)
                .build();
            
            ingestor.ingest(userDocuments);
            logger.info("✅ 用户文档向量化完成: user_{}", userId);
        } else {
            logger.info("ℹ️ 用户 user_{} 暂无私有文档", userId);
        }
        
        // 缓存
        userStoreCache.put(userId, store);
        
        return store;
    }
    
    /**
     * 加载用户私有文档
     */
    private List<Document> loadUserDocuments(String userId) {
        List<Document> documents = new ArrayList<>();
        
        try {
            // 构建用户知识库路径
            String userRagPath = userDataRootPath + File.separator + usersDir + 
                               File.separator + "user_" + userId + File.separator + "rag_sources";
            
            Path userPath = Paths.get(userRagPath);
            
            // 检查目录是否存在
            if (!Files.exists(userPath)) {
                logger.info("ℹ️ 用户知识库目录不存在: {}", userRagPath);
                return documents;
            }
            
            logger.info("📂 扫描用户知识库: {}", userRagPath);
            
            // 递归遍历用户文档
            try (Stream<Path> pathStream = Files.walk(userPath)) {
                pathStream.filter(Files::isRegularFile)
                    .forEach(filePath -> {
                        try {
                            Document document = loadDocument(filePath);
                            if (document != null) {
                                documents.add(document);
                                logger.debug("✅ 加载用户文档: {}", filePath.getFileName());
                            }
                        } catch (Exception e) {
                            logger.error("❌ 加载用户文档失败: {} - {}", filePath, e.getMessage());
                        }
                    });
            }
            
        } catch (Exception e) {
            logger.error("❌ 扫描用户知识库失败: user_{}", userId, e);
        }
        
        return documents;
    }
    
    /**
     * 根据文件类型加载文档
     */
    private Document loadDocument(Path filePath) throws IOException {
        String fileName = filePath.getFileName().toString().toLowerCase();
        
        if (fileName.endsWith(".txt") || fileName.endsWith(".md")) {
            return FileSystemDocumentLoader.loadDocument(filePath, new TextDocumentParser());
        } else if (fileName.endsWith(".pdf")) {
            return FileSystemDocumentLoader.loadDocument(filePath, new ApachePdfBoxDocumentParser());
        } else if (fileName.endsWith(".doc") || fileName.endsWith(".docx") || 
                   fileName.endsWith(".ppt") || fileName.endsWith(".pptx") ||
                   fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {
            return FileSystemDocumentLoader.loadDocument(filePath, new ApachePoiDocumentParser());
        }
        
        return null;
    }
    
    /**
     * 清除用户缓存（用户上传新文档后调用）
     */
    public void clearUserCache(String userId) {
        userStoreCache.remove(userId);
        userRetrieverCache.remove(userId);
        logger.info("🗑️ 清除用户缓存: user_{}", userId);
    }
    
    /**
     * 获取缓存统计信息
     */
    public String getCacheStats() {
        return String.format("用户知识库缓存: %d 个用户, 检索器缓存: %d 个", 
                           userStoreCache.size(), userRetrieverCache.size());
    }
}

