package com.tyty.tfs.service;

import com.tyty.tfs.dao.entity.TmHouse;
import com.tyty.tfs.dao.mapper.TmHouseMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class TfsServiceApplicationTests {

    @Autowired
    private TmHouseMapper tmHouseMapper;

    @Transactional
    @Rollback
    @Test
    public void test2() {
        TmHouse tmHouse = tmHouseMapper.selectByPrimaryKey((long) 0);
    }

}
