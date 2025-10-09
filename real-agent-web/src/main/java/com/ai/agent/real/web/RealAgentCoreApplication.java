package com.ai.agent.real.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 多扫 一个包扫描com.ai.agent.real.application 下的所有类
@SpringBootApplication(scanBasePackages = { "com.ai.agent.real.application", "com.ai.agent.real.web" })
public class RealAgentCoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(RealAgentCoreApplication.class, args);
	}

}
