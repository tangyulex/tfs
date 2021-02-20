package com.tyty.tfs.web;

import com.tyty.tfs.dao.entity.TmHouse;
import com.tyty.tfs.dao.mapper.TmHouseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class LianJiaEsfController {

    @Autowired
    private TmHouseMapper tmHouseMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("getBySql")
    public RespWrapper<List<Map<String, Object>>> getBySql(@RequestParam("sql") String sql) {
        List<Map<String, Object>> maps = jdbcTemplate.queryForList(sql);
        return RespWrapper.success(maps);
    }

    @PostMapping("sendPreference")
    public RespWrapper<Void> sendPreference(@RequestParam("id") Long id,
                               @RequestParam("preference") String preference) {
        TmHouse h = new TmHouse();
        h.setId(id);
        h.setRemark(preference);
        tmHouseMapper.updateByPrimaryKeySelective(h);
        return RespWrapper.success(null);
    }

}
