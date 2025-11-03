package com.ai.agent.real.entity.agent.context.reactplus;

import lombok.Data;

import java.util.List;

/**
 * @author han
 * @time 2025/11/2 23:59
 */
@Data
public class ReActPlusAgentContextMeta {

	private AgentMode agentMode;

	private TaskModeMeta taskModeMeta;

	private ReActPlusAgentContextMeta(AgentMode agentMode, TaskModeMeta taskModeMeta) {
		this.agentMode = agentMode;
		this.taskModeMeta = taskModeMeta;
	}

	public static TaskModeMetaBuilder taskModeMetaBuilder() {
		return new TaskModeMetaBuilder();
	}

	public static class TaskModeMetaBuilder {

		private String goal;

		private String currentTaskId;

		private List<TaskModeMeta.TaskPhase> taskPhaseList;

		public TaskModeMetaBuilder() {
		}

		public TaskModeMetaBuilder goal(String goal) {
			this.goal = goal;
			return this;
		}

		public TaskModeMetaBuilder currentTaskId(String currentTaskId) {
			this.currentTaskId = currentTaskId;
			return this;
		}

		public TaskModeMetaBuilder taskPhaseList(List<TaskModeMeta.TaskPhase> taskPhaseList) {
			this.taskPhaseList = taskPhaseList;
			return this;
		}

		public ReActPlusAgentContextMeta build() {
			return new ReActPlusAgentContextMeta(AgentMode.PLAN, new TaskModeMeta(goal, currentTaskId, taskPhaseList));
		}

	}

}
