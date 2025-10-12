// package com.ai.agent.real.web.config.validator;
//
// import com.ai.agent.real.web.config.properties.IRealAgentProperties;
// import com.ai.agent.real.web.config.properties.IRealAgentProperties.ApprovalMode;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.boot.ApplicationArguments;
// import org.springframework.boot.ApplicationRunner;
// import org.springframework.core.env.Environment;
// import org.springframework.stereotype.Component;
// import org.springframework.util.Assert;
//
// import java.time.Duration;
//
/// ** TODO Agent 配置验证器 在应用启动时验证配置的正确性和一致性
// *
// * @author han
// * @time 2025/10/12
// */
// @Slf4j
// @Component
// @RequiredArgsConstructor
// public class AgentConfigValidator implements ApplicationRunner {
//
// private final IRealAgentProperties agentProperties;
//
// private final Environment environment;
//
// @Override
// public void run(ApplicationArguments args) throws Exception {
// log.info("Starting Agent configuration validation...");
//
// validateActionConfig();
// validateEnvironmentSpecificConfig();
// logConfigurationSummary();
//
// log.info("Agent configuration validation completed successfully");
// }
//
// /**
// * 验证 Action 配置
// */
// private void validateActionConfig() {
// IRealAgentProperties.Action action = agentProperties.getAction();
//
// // 验证审批模式
// Assert.notNull(action.getApprovalMode(), "Approval mode must not be null");
// log.debug("Approval mode validation passed: {}", action.getApprovalMode());
//
// // 验证执行超时时间
// Duration timeout = action.getExecutionTimeout();
// Assert.notNull(timeout, "Execution timeout must not be null");
// Assert.isTrue(!timeout.isNegative(), "Execution timeout must not be negative");
// Assert.isTrue(timeout.getSeconds() <= 600, "Execution timeout should not exceed 10
// minutes");
// log.debug("Execution timeout validation passed: {}", timeout);
//
//
// }
//
//
//
// /**
// * 验证环境特定配置
// */
// private void validateEnvironmentSpecificConfig() {
// String[] activeProfiles = environment.getActiveProfiles();
// ApprovalMode approvalMode = agentProperties.getAction().getApprovalMode();
//
// // 生产环境配置检查
// if (isProductionEnvironment(activeProfiles)) {
// validateProductionConfig(approvalMode);
// }
//
// // 开发环境配置检查
// if (isDevelopmentEnvironment(activeProfiles)) {
// validateDevelopmentConfig(approvalMode);
// }
// }
//
// /**
// * 验证生产环境配置
// */
// private void validateProductionConfig(ApprovalMode approvalMode) {
// // 生产环境建议使用审批模式
// if (approvalMode == ApprovalMode.AUTO) {
// log.warn(
// "Production environment is using AUTO approval mode. Consider using REQUIRE_APPROVAL
// for better security.");
// }
//
// // 检查超时时间是否合理
// Duration timeout = agentProperties.getAction().getExecutionTimeout();
// if (timeout.getSeconds() < 30) {
// log.warn("Production environment timeout ({}) is quite short. Consider increasing it.",
// timeout);
// }
// }
//
// /**
// * 验证开发环境配置
// */
// private void validateDevelopmentConfig(ApprovalMode approvalMode) {
// // 开发环境可以使用自动模式
// if (approvalMode == ApprovalMode.REQUIRE_APPROVAL) {
// log.info(
// "Development environment is using REQUIRE_APPROVAL mode. You may want to use AUTO for
// faster development.");
// }
// }
//
// /**
// * 记录配置摘要
// */
// private void logConfigurationSummary() {
// IRealAgentProperties.Action action = agentProperties.getAction();
//
// log.info("=== Agent Configuration Summary ===");
// log.info("Approval Mode: {}", action.getApprovalMode());
// log.info("Execution Timeout: {}", action.getExecutionTimeout());
// log.info("Max Concurrent Executions: {}", action.getMaxConcurrentExecutions());
// log.info("Tools Approval Mode: {}", action.getTools().getApprovalMode());
// log.info("Active Profiles: {}", String.join(", ", environment.getActiveProfiles()));
// log.info("===================================");
// }
//
// private boolean isProductionEnvironment(String[] profiles) {
// for (String profile : profiles) {
// if ("prod".equals(profile) || "production".equals(profile)) {
// return true;
// }
// }
// return false;
// }
//
// private boolean isDevelopmentEnvironment(String[] profiles) {
// for (String profile : profiles) {
// if ("dev".equals(profile) || "development".equals(profile) || "local".equals(profile))
// {
// return true;
// }
// }
// return profiles.length == 0; // 默认环境视为开发环境
// }
//
// }