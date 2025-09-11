package com.ai.agent.kit.web.repository;

import com.ai.agent.kit.web.controller.TestController.*;
import org.springframework.data.r2dbc.repository.*;
import org.springframework.stereotype.*;

/**
 * @author han
 * @time 2025/9/4 0:05
 */

@Repository
public interface TestRepository extends R2dbcRepository<Test, Integer> {
}
