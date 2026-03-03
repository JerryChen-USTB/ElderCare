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
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 全局知识库管理器
 * 负责在应用启动时加载和向量化公共知识库
 */
@Service
public class GlobalKnowledgeManager implements ApplicationRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalKnowledgeManager.class);
    
    @Autowired
    private EmbeddingModel embeddingModel;
    
    private EmbeddingStore<TextSegment> globalStore;
    private ContentRetriever globalRetriever;
    private volatile boolean initialized = false;
    
    /**
     * 应用启动时自动执行
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("🚀 开始初始化全局知识库...");
        initializeGlobalKnowledge();
        logger.info("✅ 全局知识库初始化完成");
    }
    
    /**
     * 初始化全局知识库
     */
    private void initializeGlobalKnowledge() {
        try {
            // 1. 加载公共文档
            List<Document> documents = loadGlobalDocuments();
            
            if (documents.isEmpty()) {
                logger.warn("⚠️ 未找到公共知识库文档");
                // 创建空的存储
                globalStore = new InMemoryEmbeddingStore<>();
            } else {
                logger.info("📚 加载了 {} 个公共文档", documents.size());
                
                // 2. 创建向量存储
                globalStore = new InMemoryEmbeddingStore<>();
                
                // 3. 文档向量化并存储
                EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .embeddingStore(globalStore)
                    .embeddingModel(embeddingModel)
                    .build();
                
                ingestor.ingest(documents);
                logger.info("✅ 公共文档向量化完成");
            }
            
            // 4. 创建全局检索器
            globalRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(globalStore)
                .embeddingModel(embeddingModel)
                .minScore(0.8)  // 相似度阈值：只返回相似度 ≥ 80% 的文档
                .maxResults(5)
                .build();
            
            initialized = true;
            
        } catch (Exception e) {
            logger.error("❌ 全局知识库初始化失败", e);
            // 创建空的检索器以避免NPE
            globalStore = new InMemoryEmbeddingStore<>();
            globalRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(globalStore)
                .embeddingModel(embeddingModel)
                .build();
        }
    }
    
    /**
     * 递归加载公共文档
     */
    private List<Document> loadGlobalDocuments() {
        List<Document> allDocuments = new ArrayList<>();
        
        try {
            // 获取资源目录的URL
            URL resourceUrl = getClass().getClassLoader().getResource("rag_sources");
            if (resourceUrl == null) {
                logger.warn("❌ 无法找到公共知识库目录: rag_sources");
                return allDocuments;
            }
            
            Path rootPath = Paths.get(resourceUrl.toURI());
            logger.info("📂 扫描公共知识库目录: {}", rootPath);
            
            // 递归遍历所有文件
            try (Stream<Path> pathStream = Files.walk(rootPath)) {
                pathStream.filter(Files::isRegularFile)
                    .forEach(filePath -> {
                        try {
                            Document document = loadDocument(filePath);
                            if (document != null) {
                                allDocuments.add(document);
                                logger.info("✅ 加载文档: {}", filePath.getFileName());
                            }
                        } catch (Exception e) {
                            logger.error("❌ 加载文档失败: {} - {}", filePath, e.getMessage());
                        }
                    });
            }
            
        } catch (Exception e) {
            logger.error("❌ 扫描公共知识库失败", e);
        }
        
        return allDocuments;
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
     * 获取全局检索器
     */
    public ContentRetriever getGlobalRetriever() {
        if (!initialized) {
            logger.warn("⚠️ 全局知识库尚未初始化完成");
        }
        return globalRetriever;
    }
    
    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }
}

