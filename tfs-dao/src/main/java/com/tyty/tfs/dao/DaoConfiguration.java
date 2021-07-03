package com.tyty.tfs.dao;

import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

@Configuration
@MapperScan(basePackages = "com.tyty.tfs.dao.mapper", sqlSessionFactoryRef = "sqlSessionFactory")
public class DaoConfiguration {

    private static final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @Bean
    @ConfigurationProperties("spring.datasource.druid")
    public DataSource dataSource() {
        return DruidDataSourceBuilder.create().build();
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource,
                                               @Value("${mybatis.mapper-locations}") String[] mapperLocations)
            throws Exception {

        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        Resource[] resources = Stream.of(Optional.ofNullable(mapperLocations).orElse(new String[0]))
                .flatMap(location -> Stream.of(getResources(location)))
                .toArray(Resource[]::new);
        bean.setMapperLocations(resources);
        return bean.getObject();
    }

    private static Resource[] getResources(String location) {
        try {
            return resolver.getResources(location);
        } catch (IOException e) {
            return new Resource[0];
        }
    }
}
