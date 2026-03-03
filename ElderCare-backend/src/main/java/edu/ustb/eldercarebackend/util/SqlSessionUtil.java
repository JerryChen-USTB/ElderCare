package edu.ustb.eldercarebackend.util;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;

/**
 * @program: mybatis01_ustb
 * @ClassName SqlSessionUtil
 * @description:
 * @author: peterSUN
 * @create: 2025-07-01 15:37
 * @Version 1.0
 *
 * 这是一个SqlSession的工具类。获得和关闭都封装在这个类中。<br>
 * 在工具类需要SqlSessionFactory只创建一次。<br>
 * 并提供二个方法：获得sqlSession方法 和 关闭sqlSession方法
 */
public class SqlSessionUtil {
    private static final SqlSessionFactory sqlSessionFactory ;
    private static SqlSession sqlSession;
    /**
     * 在静态代码块中编写创建SqlSessionFactory的代码。
     */
    static{
        try {
            String resource = "mybatis.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static SqlSession openSession(){
        if(sqlSession == null){
            sqlSession = sqlSessionFactory.openSession();
        }
        return sqlSession;
    }

    public static void closeSession(){
        if(sqlSession != null){
            sqlSession.close();
        }
        sqlSession = null;
    }

}
