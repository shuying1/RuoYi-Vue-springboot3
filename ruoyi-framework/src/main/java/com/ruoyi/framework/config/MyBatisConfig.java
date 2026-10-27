package com.ruoyi.framework.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import javax.sql.DataSource;
import org.apache.ibatis.io.VFS;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.boot.autoconfigure.SpringBootVFS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;
import com.ruoyi.common.utils.StringUtils;

/**
 * MyBatis 配置 —— 通配符包扫描 + SqlSessionFactory 构建
 *
 * <p>设计意图：解决 Spring Boot 默认 {@code mybatis.typeAliasesPackage}
 * 不支持通配符（如 {@code com.ruoyi.**.domain}）的问题。</p>
 *
 * <p>核心功能：</p>
 * <ol>
 *   <li>{@link #setTypeAliasesPackage} —— 使用 Spring 的 {@link PathMatchingResourcePatternResolver}
 *       对配置的 typeAliasesPackage 路径进行通配符解析，展开成具体的包名列表，
 *       同时支持逗号分隔的多个通配符路径</li>
 *   <li>{@link #resolveMapperLocations} —— 对 mapperLocations 配置进行通配符解析，
 *       支持多个 XML 路径（逗号分隔）</li>
 *   <li>{@link #sqlSessionFactory} —— 组装完整的 SqlSessionFactoryBean
 *       （数据源 + 扫描后的类型别名包 + Mapper XML 路径 + MyBatis 全局配置文件）</li>
 *   <li>{@code VFS.addImplClass(SpringBootVFS.class)} —— 注册 Spring Boot 兼容的 VFS 实现，
 *       使 MyBatis 在 jar 包部署时能正确扫描类</li>
 * </ol>
 *
 * @author ruoyi
 * @see PathMatchingResourcePatternResolver Spring 的资源模式匹配器
 * @see SpringBootVFS MyBatis 兼容 Spring Boot 的虚拟文件系统实现
 */
@Configuration
public class MyBatisConfig
{
    /** Spring 环境配置，读取 mybatis.typeAliasesPackage / mapperLocations / configLocation */
    @Autowired
    private Environment env;

    /** 类文件匹配模式，用于扫描包路径下所有 .class 文件 */
    static final String DEFAULT_RESOURCE_PATTERN = "**/*.class";

    /**
     * 将通配符包路径展开为具体的包名列表。
     *
     * <p>Spring Boot 默认的 {@code mybatis.typeAliasesPackage} 不支持 {@code **} 通配符，
     * 本方法通过 Spring 的 {@link PathMatchingResourcePatternResolver} +
     * {@link CachingMetadataReaderFactory} 手动展开。</p>
     *
     * <p>处理流程：</p>
     * <ol>
     *   <li>按逗号拆分 typeAliasesPackage 为多个路径</li>
     *   <li>将每个路径转换为 classpath*: 资源匹配模式</li>
     *   <li>扫描所有 .class 文件 → 用 MetadataReader 读取类元数据 → 提取 package 名</li>
     *   <li>去重后重新用逗号拼接，覆盖原配置</li>
     * </ol>
     *
     * @param typeAliasesPackage 配置文件中的 typeAliasesPackage（支持通配符，逗号分隔）
     * @return 去重后的具体包名列表（逗号分隔）
     */
    public static String setTypeAliasesPackage(String typeAliasesPackage)
    {
        ResourcePatternResolver resolver = (ResourcePatternResolver) new PathMatchingResourcePatternResolver();
        MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resolver);
        List<String> allResult = new ArrayList<String>();
        try
        {
            for (String aliasesPackage : typeAliasesPackage.split(","))
            {
                List<String> result = new ArrayList<String>();
                aliasesPackage = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
                        + ClassUtils.convertClassNameToResourcePath(aliasesPackage.trim()) + "/" + DEFAULT_RESOURCE_PATTERN;
                Resource[] resources = resolver.getResources(aliasesPackage);
                if (resources != null && resources.length > 0)
                {
                    MetadataReader metadataReader = null;
                    for (Resource resource : resources)
                    {
                        if (resource.isReadable())
                        {
                            metadataReader = metadataReaderFactory.getMetadataReader(resource);
                            try
                            {
                                result.add(Class.forName(metadataReader.getClassMetadata().getClassName()).getPackage().getName());
                            }
                            catch (ClassNotFoundException e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                if (result.size() > 0)
                {
                    HashSet<String> hashResult = new HashSet<String>(result);
                    allResult.addAll(hashResult);
                }
            }
            if (allResult.size() > 0)
            {
                typeAliasesPackage = String.join(",", (String[]) allResult.toArray(new String[0]));
            }
            else
            {
                throw new RuntimeException("mybatis typeAliasesPackage 路径扫描错误,参数typeAliasesPackage:" + typeAliasesPackage + "未找到任何包");
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return typeAliasesPackage;
    }

    /**
     * 解析 Mapper XML 文件的通配符路径。
     *
     * <p>对配置文件中的 mybatis.mapperLocations（逗号分隔的多个路径）进行通配符展开，
     * 收集所有匹配的 XML Resource 数组。</p>
     *
     * @param mapperLocations 配置文件中的 mapperLocations（可能包含通配符，如 classpath*:mapper/**/*Mapper.xml）
     * @return 解析后的 Resource 数组
     */
    public Resource[] resolveMapperLocations(String[] mapperLocations)
    {
        ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
        List<Resource> resources = new ArrayList<Resource>();
        if (mapperLocations != null)
        {
            for (String mapperLocation : mapperLocations)
            {
                try
                {
                    Resource[] mappers = resourceResolver.getResources(mapperLocation);
                    resources.addAll(Arrays.asList(mappers));
                }
                catch (IOException e)
                {
                    // ignore
                }
            }
        }
        return resources.toArray(new Resource[resources.size()]);
    }

    /**
     * 构建 MyBatis SqlSessionFactory Bean。
     *
     * <p>从 Environment 读取配置：</p>
     * <ul>
     *   <li>{@code mybatis.typeAliasesPackage} —— 类型别名包路径（先经 setTypeAliasesPackage 展开）</li>
     *   <li>{@code mybatis.mapperLocations} —— Mapper XML 位置</li>
     *   <li>{@code mybatis.configLocation} —— MyBatis 全局配置文件位置</li>
     * </ul>
     *
     * @param dataSource Spring 自动注入的数据源（实际是 {@link com.ruoyi.framework.datasource.DynamicDataSource}）
     * @return 构建好的 SqlSessionFactory
     * @throws Exception 构建过程中可能的异常
     */
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception
    {
        String typeAliasesPackage = env.getProperty("mybatis.typeAliasesPackage");
        String mapperLocations = env.getProperty("mybatis.mapperLocations");
        String configLocation = env.getProperty("mybatis.configLocation");
        typeAliasesPackage = setTypeAliasesPackage(typeAliasesPackage);
        VFS.addImplClass(SpringBootVFS.class);

        final SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setTypeAliasesPackage(typeAliasesPackage);
        sessionFactory.setMapperLocations(resolveMapperLocations(StringUtils.split(mapperLocations, ",")));
        sessionFactory.setConfigLocation(new DefaultResourceLoader().getResource(configLocation));
        return sessionFactory.getObject();
    }
}
