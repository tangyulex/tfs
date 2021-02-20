package com.tyty.tfs.dao;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

@Configuration
@MapperScan(basePackages = "com.tyty.tfs.dao.mapper", sqlSessionFactoryRef = "sqlSessionFactory")
public class DaoConfiguration {

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource ds,
                                               @Value("${mybatis.mapper-locations}") String mapperLocations)
            throws Exception {

        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(ds);
        bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(mapperLocations));
        return bean.getObject();
    }
}
