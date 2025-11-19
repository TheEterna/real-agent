import { PlanData, PlanPhase, PlanPhaseStatus, PlanStatus } from '@/types/events'
import { nanoid } from 'nanoid'

/**
 * 生成测试用的计划数据
 */
export function generateTestPlan(): PlanData {
  const phases: PlanPhase[] = [
    {
      id: nanoid(8),
      title: '需求分析和系统设计',
      description: '分析用户需求，设计系统架构和技术方案。包括：1) 收集和整理功能需求；2) 设计数据库结构；3) 制定技术选型方案；4) 创建系统架构图；5) 评估开发工作量和时间计划。',
      isParallel: false,
      status: PlanPhaseStatus.COMPLETED,
      index: 0
    },
    {
      id: nanoid(8),
      title: '环境搭建和基础配置',
      description: '搭建开发环境和项目基础配置。主要工作包括：1) 配置开发环境（IDE、版本控制、依赖管理）；2) 创建项目骨架和目录结构；3) 配置构建工具和脚本；4) 设置代码规范和质量检查工具；5) 搭建持续集成环境。',
      isParallel: false,
      status: PlanPhaseStatus.COMPLETED,
      index: 1
    },
    {
      id: nanoid(8),
      title: '核心功能模块开发',
      description: '并行开发各核心功能模块。包括：1) 用户管理模块（注册、登录、权限）；2) 数据管理模块（CRUD操作、数据验证）；3) 业务逻辑模块（核心算法、业务规则）；4) 接口层开发（API设计、文档编写）；5) 前端界面开发（组件化、响应式设计）。',
      isParallel: true,
      status: PlanPhaseStatus.TODO,
      index: 2
    },
    {
      id: nanoid(8),
      title: '系统集成和测试',
      description: '进行系统集成测试和质量保证工作。主要任务：1) 单元测试编写和执行；2) 集成测试和系统测试；3) 性能测试和优化；4) 安全性测试和漏洞修复；5) 用户验收测试；6) Bug修复和功能完善。',
      isParallel: false,
      status: PlanPhaseStatus.TODO,
      index: 3
    },
    {
      id: nanoid(8),
      title: '部署上线和维护',
      description: '完成系统部署和后期维护工作。包括：1) 生产环境配置和部署；2) 数据迁移和备份策略；3) 监控和日志系统配置；4) 用户培训和文档编写；5) 运维流程建立；6) 后期维护和版本更新计划。',
      isParallel: false,
      status: PlanPhaseStatus.TODO,
      index: 4
    }
  ]

  return {
    goal: '开发一个高质量的全栈Web应用系统',
    phases,
    currentPhaseId: phases[1].id, // 当前在第二阶段
    status: PlanStatus.EXECUTING,
    createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000), // 2小时前创建
    updatedAt: new Date(Date.now() - 10 * 60 * 1000) // 10分钟前更新
  }
}

/**
 * 生成简单的测试计划
 */
export function generateSimplePlan(): PlanData {
  const phases: PlanPhase[] = [
    {
      id: nanoid(8),
      title: '项目初始化',
      description: '创建项目结构，配置开发环境',
      isParallel: false,
      status: PlanPhaseStatus.COMPLETED,
      index: 0
    },
    {
      id: nanoid(8),
      title: '功能开发',
      description: '实现核心功能模块',
      isParallel: true,
      status: PlanPhaseStatus.RUNNING,
      index: 1
    },
    {
      id: nanoid(8),
      title: '测试部署',
      description: '进行测试并部署到生产环境',
      isParallel: false,
      status: PlanPhaseStatus.TODO,
      index: 2
    }
  ]

  return {
    goal: '快速原型开发',
    phases,
    currentPhaseId: phases[1].id,
    status: PlanStatus.EXECUTING,
    createdAt: new Date(),
    updatedAt: new Date()
  }
}

/**
 * 模拟计划推进
 */
export function simulatePlanAdvancement(plan: PlanData): PlanData {
  const phases = plan.phases.map((phase, index) => {
    if (phase.status === PlanPhaseStatus.RUNNING) {
      // 将当前执行的阶段标记为完成
      return { ...phase, status: PlanPhaseStatus.COMPLETED }
    } else if (phase.status === PlanPhaseStatus.TODO &&
               plan.phases.slice(0, index).every(p => p.status === PlanPhaseStatus.COMPLETED)) {
      // 将下一个待执行的阶段标记为运行中
      return { ...phase, status: PlanPhaseStatus.RUNNING }
    }
    return phase
  })

  const currentPhase = phases.find(p => p.status === PlanPhaseStatus.RUNNING)
  const allCompleted = phases.every(p => p.status === PlanPhaseStatus.COMPLETED)

  return {
    ...plan,
    phases,
    currentPhaseId: currentPhase?.id,
    status: allCompleted ? PlanStatus.COMPLETED : PlanStatus.EXECUTING,
    updatedAt: new Date()
  }
}