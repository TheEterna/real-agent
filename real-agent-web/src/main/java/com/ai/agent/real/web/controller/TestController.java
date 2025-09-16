package com.ai.agent.real.web.controller;

import com.ai.agent.kit.web.repository.*;
import com.ai.agent.real.web.repository.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.data.annotation.*;
import org.springframework.data.r2dbc.core.*;
import org.springframework.data.relational.core.query.*;
import org.springframework.r2dbc.core.*;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.*;

/**
 * @author han
 * @time 2025/8/28 14:42
 */

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private R2dbcEntityTemplate r2dbcEntityTemplate;

    @Autowired
    private DatabaseClient databaseClient;

    @Autowired
    private TestRepository testRepository;


    @RequestMapping("/hello")
    public Mono<String> hello() {
        return Mono.just("hello");
    }

    @RequestMapping("/sql")
    public Mono<String> sql() {

        Criteria criteria = Criteria.empty()
                .and("id").is(1);

        Query query = Query.query(criteria);

        return r2dbcEntityTemplate.select(Test.class)
                .matching(query)
                .all()
                .map(test -> test.name)
                .collectList()
                .map(list -> list.toString());
    }
    @RequestMapping("/sql2")
    public Mono<String> sql2() {

        return databaseClient.sql("select * from test where id = 1")
                .map(row -> {
                    Test test = new Test();
                    test.id = row.get("id", Integer.class);
                    test.name = row.get("name", String.class);
                    return test;
                })
                .one()
                .map(test -> test.name)
                .map(name -> name.toString());
    }
    @RequestMapping("/sql3")
    public Mono<String> sql3() {
        return testRepository.findById(1)
                .map(test -> test.name);
    }

    public static class Test {
        @Id
        public int id;
        public String name;
    }
}
