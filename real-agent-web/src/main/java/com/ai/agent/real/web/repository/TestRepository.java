package com.ai.agent.real.web.repository;

import com.ai.agent.real.web.controller.TestController.*;
import org.springframework.data.r2dbc.repository.*;
import org.springframework.stereotype.*;

/**
 * @author han
 * @time 2025/9/4 0:05
 */

@Repository
public interface TestRepository extends R2dbcRepository<Test, Integer> {
}
