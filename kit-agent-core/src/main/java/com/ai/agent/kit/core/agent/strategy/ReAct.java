package com.ai.agent.kit.core.agent.strategy;

import com.ai.agent.kit.common.spec.*;
import com.ai.agent.kit.common.spec.AgentExecutionEvent.*;
import com.ai.agent.kit.core.agent.*;
import com.ai.agent.kit.core.agent.communication.*;
import com.ai.agent.kit.core.agent.communication.AgentMessage;
import com.ai.agent.kit.core.agent.impl.*;
import com.ai.agent.kit.core.tool.*;
import com.ai.agent.kit.core.tool.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage.*;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.*;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.model.tool.*;
import org.springframework.ai.support.*;
import reactor.core.publisher.*;

import java.util.*;

import static com.ai.agent.kit.common.constant.AgentConstants.*;
import static com.ai.agent.kit.common.constant.NounConstants.TASK_DONE;
import static com.ai.agent.kit.common.utils.TaskUtil.isTaskComplete;
import static com.ai.agent.kit.common.utils.ToolUtils.getToolsDescription;

/**
 * ReAct框架实现：Reasoning and Acting
 * 实现思考-行动-观察的循环推理模式
 * 
 * @author han
 * @time 2025/9/5 12:32
 */
@Slf4j
public class ReAct implements AgentStrategy {

    private static final int MAX_ITERATIONS = 10;
    private final ThinkingAgent thinkingAgent;
    private final ActionAgent actionAgent;
    private final ObservationAgent observationAgent;
    private final FinalAgent finalAgent;
    private final List<AgentTool> availableTools;

    public ReAct(ChatModel chatModel, ToolRegistry toolRegistry) {
        this.availableTools = toolRegistry.list();
        this.thinkingAgent = new ThinkingAgent(chatModel, toolRegistry);
        this.actionAgent = new ActionAgent(chatModel, toolRegistry);
        this.observationAgent = new ObservationAgent(chatModel, toolRegistry);
        this.finalAgent = new FinalAgent(chatModel, toolRegistry);
    }


    


    /**
     * 流式执行策略（推荐）
     * 返回实时的执行进度和中间结果
     *
     * @param task    任务描述
     * @param agents  可用的Agent列表
     * @param context 执行上下文
     * @return 流式执行结果
     */
    @Override
    public Flux<AgentExecutionEvent> executeStream(String task, List<Agent> agents, AgentContext context) {
        return Flux.create(sink -> {
            try {
                // 发送开始事件
                sink.next(AgentExecutionEvent.started("开始ReAct推理循环"));
                
                // 初始化上下文 - 添加任务消息
                context.addMessage(AgentMessage.user("任务: " + task, "user"));

                for (int iteration = 1; iteration <= MAX_ITERATIONS; iteration++) {
                    int finalIteration = iteration;
                    log.debug("ReAct循环第{}轮开始", iteration);
                    context.setCurrentIteration(iteration);
                    
                    // 发送进度事件
                    sink.next(AgentExecutionEvent.progress(
                        "ReAct循环第" + iteration + "轮",
                        (double) iteration / MAX_ITERATIONS
                    ));

                    // 1. 思考阶段 - 使用ThinkingAgent
                    sink.next(AgentExecutionEvent.thinking(ThinkingAgent.AGENT_ID, "开始分析任务...\n"));

                    String fullThinking = thinkingAgent.executeStream(task, context)
                            .doOnNext(thinking -> {
                                sink.next(AgentExecutionEvent.thinking(ThinkingAgent.AGENT_ID, thinking.getMessage()));
                            })

//                        .doOnComplete(() -> {
//                            String fullThinking = context.getLastThinking();
//                            // 添加思考消息到对话历史
//                            context.addMessage(AgentMessage.thinking(fullThinking, ThinkingAgent.AGENT_ID, finalIteration));
//                        })

                          .reduce("", (acc, cur) -> cur.getMessage() + acc)
                          .block();
                    context.addMessage(AgentMessage.thinking(fullThinking, ThinkingAgent.AGENT_ID, finalIteration));

                    // 2. 行动阶段 - 使用ActionAgent
                    sink.next(AgentExecutionEvent.action(ActionAgent.AGENT_ID, "开始执行行动...\n"));

                    String fullAction = actionAgent.executeStream(task, context)
                            .doOnNext(action -> {
                                sink.next(AgentExecutionEvent.action(ActionAgent.AGENT_ID, action.getMessage()));
                            })
//                        .doOnComplete(() -> {
//                            // 添加行动消息到对话历史
//                            context.addMessage(AgentMessage.action("Action: " + finalIteration + fullAction, ActionAgent.AGENT_ID, finalIteration));
//                        })
                            .reduce("", (acc, cur) -> cur.getMessage() + acc)
                            .block();
                    context.addMessage(AgentMessage.action(fullAction, ThinkingAgent.AGENT_ID, finalIteration));

                    // 检查是否通过 task_done 工具完成任务
                    if (isTaskCompletedByTool(fullAction) && validateTaskCompletion(task, context, fullAction)) {
                        // 进行额外的任务完成验证 使用final agent来结束总结任务

                        String endingMessage = finalAgent.executeStream(task, context)
                                .doOnNext(ending -> {
                                    sink.next(AgentExecutionEvent.executing(FinalAgent.AGENT_ID, ending.getMessage()));
                                })
                                .reduce("", (acc, cur) -> cur.getMessage() + acc)
                                .block();

                        context.addMessage(AgentMessage.done(endingMessage, FinalAgent.AGENT_ID, finalIteration));

                        sink.next(AgentExecutionEvent.completed(FinalAgent.AGENT_ID, TASK_DONE));
                        sink.complete();

                    } else {
                        context.addMessage(AgentMessage.action("Action: " + finalIteration + fullAction, ActionAgent.AGENT_ID, finalIteration));
                    }


                    // 3. 观察阶段 - 使用ObservationAgent
                    sink.next(AgentExecutionEvent.observing(ObservationAgent.AGENT_ID, "开始观察结果...\n"));

                    String fullObservation = observationAgent.executeStream(task, context)
                            .doOnNext(observation -> {
                                sink.next(AgentExecutionEvent.observing(ObservationAgent.AGENT_ID, observation.getMessage()));
                            })
//                        .doOnComplete(() -> {
//                            String fullObservation = context.getLastToolResult();
//                            // 添加观察消息到对话历史
//                            context.addMessage(AgentMessage.observing("Observation: " + finalIteration + fullObservation, ObservationAgent.AGENT_ID, finalIteration));
//
//                            sink.next(AgentExecutionEvent.partialResult(
//                                "react-agent",
//                                "第" + finalIteration + "轮ReAct循环完成"
//                            ));
//                        })
                        .reduce("", (acc, cur) -> cur.getMessage() + acc)
                        .block();
                    context.addMessage(AgentMessage.observing(fullObservation, ThinkingAgent.AGENT_ID, finalIteration));



                }

                // 达到最大迭代次数
                log.warn("ReAct循环达到最大迭代次数{}，强制结束", MAX_ITERATIONS);
                sink.next(AgentExecutionEvent.completed(FinalAgent.AGENT_ID, "达到最大迭代次数 " + MAX_ITERATIONS + "，任务未完成"));
                sink.complete();
                
            } catch (Exception e) {
                log.error("ReAct流式执行异常", e);
                sink.next(AgentExecutionEvent.error("ReAct执行异常: " + e.getMessage(), e));
                sink.error(e);
            }
        });
    }
    
    /**
     * 检查是否通过工具完成任务
     * 参考trae-agent的llm_indicates_task_completed方法
     */
    private boolean isTaskCompletedByTool(String response) {
        if (response == null) {
            return false;
        }
        
        // 1. 检查是否包含task_done工具调用
        if (response.contains(TASK_DONE)) {
            return true;
        }
        
        // 2. 检查其他完成指示词（参考trae-agent）
        String responseLower = response.toLowerCase();
        String[] completionIndicators = {
            "task completed", "task finished", "done", 
            "completed successfully", "finished successfully",
            "任务完成", "任务已完成", "完成了", "已完成"
        };
        
        for (String indicator : completionIndicators) {
            if (responseLower.contains(indicator)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 验证任务是否真正完成
     * 参考trae-agent的_is_task_completed方法，进行额外的业务逻辑验证
     */
    private boolean validateTaskCompletion(String task, AgentContext context, String response) {
        // 1. 检查是否有实际的执行结果
        if (response == null || response.trim().isEmpty()) {
            return false;
        }
        
        // 2. 检查对话历史中是否有实际的工具执行记录
        List<AgentMessage> conversationHistory = context.getConversationHistory();
        // 判断 conversationHistory 有没有tool的message
        boolean hasToolExecution = conversationHistory.stream()
            .anyMatch(msg -> msg.getAgentMessageType() == AgentMessage.AgentMessageType.TOOL);
        if (!hasToolExecution) { 
            return false;
        }

        // 4. 检查任务类型特定的完成条件
        return true;
    }
    




    /**
     * 执行
     *
     * @param task    任务描述
     * @param agents  可用的Agent列表
     * @param context 工具执行上下文
     * @return 执行结果
     */
    @Override
    public AgentResult execute(String task, List<Agent> agents, AgentContext context) {
//        StringBuilder conversationHistory = new StringBuilder();
//        conversationHistory.append("任务: ").append(task).append("\n");
//        conversationHistory.append("可用工具: ").append(getToolsDescription(availableTools)).append("\n\n");
//
//        for (int iteration = 1; iteration <= MAX_ITERATIONS; iteration++) {
//            log.debug("ReAct循环第{}轮开始", iteration);
//
//            // 1. 思考阶段 (Thought)
//            AgentResult thoughtAgentResult = generateThought(conversationHistory.toString(), availableTools);
//            String thought = thoughtAgentResult.getResult();
//            conversationHistory.append("思考").append(iteration).append(": ").append(thought).append("\n");
//
//            // 检查是否完成
//            if (isTaskComplete(thought)) {
//                String finalAnswer = extractFinalAnswer(thought);
//                log.info("ReAct循环在第{}轮完成", iteration);
//                return AgentResult.success(finalAnswer, task);
//            }
//
//            // 2. 行动阶段 (Action)
//            AgentResult actionAgentResult = generateActionWithTools(conversationHistory.toString(), chatModel, availableTools);
//            String action = actionAgentResult.getResult();
//            conversationHistory.append("行动").append(iteration).append(": ").append(action).append("\n");
//
//            // 3. 观察阶段 (Observation)
//            String observation = executeActionWithTools(action, context, availableTools);
//            conversationHistory.append("观察").append(iteration).append(": ").append(observation).append("\n\n");
//        }
//
//        // 达到最大迭代次数，返回当前最佳结果
//        log.warn("ReAct循环达到最大迭代次数{}，强制结束", MAX_ITERATIONS);
//        return generateFinalAnswer(conversationHistory.toString(), chatModel);

        return AgentResult.success(executeStream(task, agents, context).blockLast().getMessage(), "1");
    }




    /**
     * 带工具支持的行动执行
     */
    private String executeActionWithTools(String action, AgentContext context, List<AgentTool> availableTools) {
        // 解析行动，判断是否需要调用工具
        if (action.contains("使用工具[") && action.contains("]")) {
            String toolName = extractToolName(action);
            return executeToolAction(toolName, action, context, availableTools);
        } else {
            // 普通行动，返回模拟结果
            return "执行了行动: " + action + "，获得了相关信息。";
        }
    }

    /**
     * 提取工具名称
     */
    private String extractToolName(String action) {
        int start = action.indexOf("使用工具[") + 4;
        int end = action.indexOf("]", start);
        if (start > 3 && end > start) {
            return action.substring(start, end);
        }
        return "";
    }

    /**
     * 执行工具行动
     */
    private String executeToolAction(String toolName, String action, AgentContext context, List<AgentTool> availableTools) {
        // 查找对应的工具
        AgentTool targetTool = availableTools.stream()
                .filter(tool -> tool.getSpec().getName().equals(toolName))
                .findFirst()
                .orElse(null);

        if (targetTool == null) {
            return "错误: 未找到工具 [" + toolName + "]";
        }

        try {
            // 这里应该调用具体的工具执行逻辑
            // 目前简化为模拟执行
            return "使用工具 [" + toolName + "] 执行成功，获得结果: " + action;
        } catch (Exception e) {
            return "工具 [" + toolName + "] 执行失败: " + e.getMessage();
        }
    }



    /**
     * 提取最终答案
     */
    private String extractFinalAnswer(String thought) {
        // 简单的答案提取逻辑
        if (thought.contains("最终答案:")) {
            return thought.substring(thought.indexOf("最终答案:") + 5).trim();
        }
        return thought;
    }



}
