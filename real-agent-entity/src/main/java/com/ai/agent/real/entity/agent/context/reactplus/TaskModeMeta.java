package com.ai.agent.real.entity.agent.context.reactplus;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author han
 * @time 2025/11/3 0:33
 */
@Data
public class TaskModeMeta {

	/**
	 * 全局任务
	 */
	private String goal;

	private String currentTaskId;

	private List<TaskPhase> taskPhaseList;

	public TaskModeMeta(String goal, String currentTaskId, List<TaskPhase> taskPhaseList) {
		this.goal = goal;
		this.currentTaskId = currentTaskId;
		this.taskPhaseList = taskPhaseList;
	}

	public TaskPhase getCurrentTask() {
		return taskPhaseList.stream()
			.filter(taskPhase -> taskPhase.getId().equals(currentTaskId))
			.findFirst()
			.orElseGet(() -> null);
	}

	@Data
	@Accessors(chain = true)
	public static class TaskPhase {

		private String id;

		private String title;

		private String description;

		private boolean isParallel;

		private int index;

		/**
		 * 标识任务状态
		 */
		private TaskStatus taskStatus;

		public TaskPhase(String id, String title, String description, int index, boolean isParallel,
				TaskStatus taskStatus) {
			this.id = id;
			this.title = title;
			this.description = description;
			this.index = index;
			this.isParallel = isParallel;
			this.taskStatus = taskStatus;
		}

	}

	public enum TaskStatus {

		TODO, DONE, RUNNING, FAILED

	}

}