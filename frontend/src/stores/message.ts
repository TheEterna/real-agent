import {UIMessage, EventType } from "@/types/events";

export const messages: UIMessage[] = [
    // 1. 系统欢迎消息
    {
        type: EventType.SYSTEM,
        sender: 'ReAct+ Assistant',
        message: `
    我是新一代增强版 ReAct 助手，具备以下能力：
    ✨ **智能工具审批** - 执行工具前会请求您的确认执行工具前会请求您的确认执行工具前会请求您的确认执行工具前会请求您的确认执行工具前会请求您的确认执行工具前会请求您的确认执行工具前会请求您的确认执行工具前会请求您的确认执行工具前会请求您的确认执行工具前会请求您的确认执行工具前会请求您的确认执行工具前会请求您的确认执行工具前会请求您的确认执行工具前会请求您的确认
    🧠 **深度推理** - 多层次思考和分析 \n🔧 **工具链协作** - 智能选择和组合使用工具\n📊 **结果验证** - 自动验证和优化执行结果
    🎯 **目标导向** - 始终聚焦于解决您的核心问题
    现在，请告诉我您希望我帮您解决什么问题？
    `,
        startTime: new Date(Date.now() - 300000),
        nodeId: 'welcome-msg'
    },

    // 2. 用户消息
    {
        type: EventType.USER,
        sender: '用户',
        message: `我正在开发一个项目，需要一个地图工具来获取项目地址的经纬度。请使用 map_geocode 工具来获取地址的经纬度。`,
        startTime: new Date(Date.now() - 250000),
        nodeId: 'user-msg-1'
    },

    // 3. Assistant 思考消息
    {
        type: EventType.ASSISTANT,
        sender: 'ReAct+ Assistant',
        message: `我需要分析您的项目结构。让我先思考一下分析的步骤：
    1. 首先查看项目的文件结构和架构
    2. 分析代码质量和组织方式
    3. 识别潜在的优化点
    4. 提供具体的改进建议
    让我开始执行这个任务...`,
        startTime: new Date(Date.now() - 240000),
        nodeId: 'thinking-msg-1',
        endTime: new Date(Date.now() - 230000)
    },
    // 5. 工具调用消息
    {
        type: EventType.TOOL,
        sender: 'map_geocode',
        data: {
            name: "map_geocode", // 工具名称，对应图标映射中的键
            id: "tool_call_123456789", // 工具调用ID
            responseData: [
                {
                    text: JSON.stringify({
                        status: "success",
                        results: [
                            {
                                formatted_address: "北京市朝阳区建国路88号",
                                location: {
                                    lat: 39.9042,
                                    lng: 116.4074
                                },
                                address_components: {
                                    country: "中国",
                                    province: "北京市",
                                    city: "北京市",
                                    district: "朝阳区",
                                    street: "建国路",
                                    street_number: "88号"
                                },
                                confidence: 0.95
                            }
                        ],
                        execution_time: 0.32 // 执行时间（秒）
                    })
                }
            ]
        },
        message: "地理编码工具调用", // 备选工具名称
        meta: {
            toolSchema: {
                address: "北京市朝阳区建国路88号",
                city: "北京市",
                output_format: "json",
                timeout: 5000,
                language: "zh-CN"
            },
            elapsedMs: 1000
        },
        startTime: new Date(Date.now() - 200000),
        nodeId: 'tool-msg-1'
    },


    // 7. 工具审批消息
    {
        type: EventType.TOOL_APPROVAL,
        sender: 'action_agent',
        message: '',
        startTime: new Date(Date.now() - 160000),
        nodeId: 'approval-msg-1',

        data: {
            toolCallId: "tool_call_123456789",
            toolName: "database_migration_tool",
            args: {
                migrationConfig: {
                    sourceDatabase: {
                        host: 'legacy-db.company.com',
                        port: 3306,
                        database: 'legacy_system',
                        username: 'migration_user',
                        password: '***masked***',
                        connectionOptions: {
                            ssl: true,
                            timeout: 30000,
                            maxConnections: 10,
                            charset: 'utf8mb4'
                        }
                    },
                    targetDatabase: {
                        host: 'new-db.company.com',
                        port: 5432,
                        database: 'modernized_system',
                        username: 'migration_user',
                        password: '***masked***',
                        connectionOptions: {
                            ssl: true,
                            timeout: 60000,
                            maxConnections: 20,
                            poolSize: 15
                        }
                    },
                    migrationStrategy: {
                        batchSize: 1000,
                        parallelTasks: 4,
                        retryPolicy: {
                            maxRetries: 3,
                            retryDelay: 5000,
                            backoffMultiplier: 2
                        },
                        dataTransformation: {
                            userTable: {
                                fieldMapping: {
                                    'old_user_id': 'user_id',
                                    'user_name': 'username',
                                    'email_addr': 'email'
                                },
                                dataValidation: {
                                    required: ['username', 'email'],
                                    constraints: {
                                        username: {minLength: 3, maxLength: 50},
                                        email: {format: 'email'}
                                    }
                                }
                            },
                            orderTable: {
                                fieldMapping: {
                                    'order_number': 'order_id',
                                    'customer_id': 'user_id',
                                    'order_total': 'total_amount'
                                },
                                dataValidation: {
                                    required: ['order_id', 'user_id', 'total_amount'],
                                    constraints: {
                                        total_amount: {type: 'decimal', min: 0}
                                    }
                                }
                            }
                        }
                    },
                    backupOptions: {
                        createBackup: true,
                        backupLocation: '/backup/migration_backup_2024',
                        compressionLevel: 6,
                        retentionPeriod: '30 days'
                    }
                },
                executionOptions: {
                    dryRun: false,
                    stopOnError: true,
                    generateReport: true,
                    notificationSettings: {
                        email: {
                            recipients: ['admin@company.com', 'dba@company.com'],
                            onSuccess: true,
                            onFailure: true,
                            onProgress: false
                        },
                        webhook: {
                            url: 'https://monitoring.company.com/webhook/migration',
                            method: 'POST',
                            headers: {
                                'Authorization': 'Bearer ***masked***',
                                'Content-Type': 'application/json'
                            }
                        }
                    }
                }
            },

        },
        meta: {
            toolSchema: {
                name: 'database_migration_tool',
                description: '执行复杂的数据库迁移任务，支持多数据库类型、数据转换、验证和备份功能',
                category: 'database',
                inputSchema: JSON.stringify({
                    type: 'object',
                    properties: {
                        migrationConfig: {
                            type: 'object',
                            description: '迁移配置信息',
                            properties: {
                                sourceDatabase: {
                                    type: 'object',
                                    description: '源数据库连接配置',
                                    properties: {
                                        host: {type: 'string', description: '数据库主机地址'},
                                        port: {type: 'number', description: '端口号'},
                                        database: {type: 'string', description: '数据库名称'},
                                        username: {type: 'string', description: '用户名'},
                                        password: {type: 'string', description: '密码'},
                                        connectionOptions: {
                                            type: 'object',
                                            description: '连接选项',
                                            properties: {
                                                ssl: {type: 'boolean'},
                                                timeout: {type: 'number'},
                                                maxConnections: {type: 'number'},
                                                charset: {type: 'string'}
                                            }
                                        }
                                    },
                                    required: ['host', 'port', 'database', 'username', 'password']
                                },
                                targetDatabase: {
                                    type: 'object',
                                    description: '目标数据库连接配置',
                                    properties: {
                                        host: {type: 'string'},
                                        port: {type: 'number'},
                                        database: {type: 'string'},
                                        username: {type: 'string'},
                                        password: {type: 'string'},
                                        connectionOptions: {type: 'object'}
                                    },
                                    required: ['host', 'port', 'database', 'username', 'password']
                                },
                                migrationStrategy: {
                                    type: 'object',
                                    description: '迁移策略配置',
                                    properties: {
                                        batchSize: {type: 'number', description: '批处理大小'},
                                        parallelTasks: {type: 'number', description: '并行任务数'},
                                        retryPolicy: {
                                            type: 'object',
                                            description: '重试策略',
                                            properties: {
                                                maxRetries: {type: 'number'},
                                                retryDelay: {type: 'number'},
                                                backoffMultiplier: {type: 'number'}
                                            }
                                        },
                                        dataTransformation: {
                                            type: 'object',
                                            description: '数据转换规则',
                                            additionalProperties: {
                                                type: 'object',
                                                properties: {
                                                    fieldMapping: {type: 'object'},
                                                    dataValidation: {type: 'object'}
                                                }
                                            }
                                        }
                                    }
                                },
                                backupOptions: {
                                    type: 'object',
                                    description: '备份选项',
                                    properties: {
                                        createBackup: {type: 'boolean'},
                                        backupLocation: {type: 'string'},
                                        compressionLevel: {type: 'number'},
                                        retentionPeriod: {type: 'string'}
                                    }
                                }
                            },
                            required: ['sourceDatabase', 'targetDatabase', 'migrationStrategy']
                        },
                        executionOptions: {
                            type: 'object',
                            description: '执行选项',
                            properties: {
                                dryRun: {type: 'boolean', description: '是否为试运行'},
                                stopOnError: {type: 'boolean', description: '遇到错误时是否停止'},
                                generateReport: {type: 'boolean', description: '是否生成报告'},
                                notificationSettings: {
                                    type: 'object',
                                    description: '通知设置',
                                    properties: {
                                        email: {
                                            type: 'object',
                                            properties: {
                                                recipients: {type: 'array', items: {type: 'string'}},
                                                onSuccess: {type: 'boolean'},
                                                onFailure: {type: 'boolean'},
                                                onProgress: {type: 'boolean'}
                                            }
                                        },
                                        webhook: {
                                            type: 'object',
                                            properties: {
                                                url: {type: 'string'},
                                                method: {type: 'string'},
                                                headers: {type: 'object'}
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    required: ['migrationConfig']
                })
            }
        }
    },

    // 8. 另一个工具调用消息（JSON数据）
    {
        type: EventType.TOOL,
        sender: 'Code Quality Tool',
        message: 'TypeScript 类型检查完成',
        data: {
            toolName: 'typescript_checker',
            result: {
                errors: 0,
                warnings: 3,
                typeCoverage: 94.5,
                issues: [
                    {
                        file: 'src/components/ToolBox.vue',
                        line: 23,
                        message: 'Implicit any type',
                        severity: 'warning'
                    },
                    {
                        file: 'src/pages/chat/ReAct.vue',
                        line: 156,
                        message: 'Unused import',
                        severity: 'warning'
                    }
                ],
                suggestions: [
                    '添加更严格的 TypeScript 配置',
                    '使用 ESLint 规则自动修复未使用的导入',
                    '考虑添加 Prettier 格式化工具'
                ]
            }
        },
        startTime: new Date(Date.now() - 140000),
        nodeId: 'tool-msg-2'
    },

    // 9. 错误消息
    {
        type: EventType.ERROR,
        sender: 'System Error',
        message: `❌ **网络连接超时**

    无法连接到代码质量检查服务。请检查：

    1. 网络连接是否正常
    2. 服务器是否可访问
    3. API 密钥是否有效

    **错误详情**: Connection timeout after 30s
    **错误代码**: NET_TIMEOUT_001`,
        startTime: new Date(Date.now() - 120000),
        nodeId: 'error-msg-1',
    },

    // 11. 用户回复
    {
        type: EventType.USER,
        sender: '用户',
        message: '感谢分析！请帮我生成一个改进代码质量的具体执行计划',
        startTime: new Date(Date.now() - 80000),
        nodeId: 'user-msg-2'
    },

    // 11. 用户回复
    {
        type: EventType.ASSISTANT,
        sender: 'bot',
        message: '# Hello\n' +
            '\n' +
            '这是一个测试页面。左侧编辑输入，右侧实时预览渲染结果。\n' +
            '\n' +
            '示例包含：\n' +
            '\n' +
            '  - **加粗**、*斜体*、`inline code`\n' +
            '- 代码块：\n' +
            '\n' +
            '```js\n' +
            'console.log(\'hello\')\n' +
            '```\n' +
            '\n' +
            '数学：$$E=mc^2$$\n' +
            'Mermaid 示例：\n' +
            '\n' +
            '```mermaid\n' +
            'graph TD\n' +
            '  A-->B\n' +
            '```\n',
        startTime: new Date(Date.now() - 80000),
        nodeId: 'user-msg-2'
    },

    // 12. 复杂的 Markdown 消息（代码块、表格、列表）
    {
        type: EventType.ASSISTANT,
        sender: 'ReAct+ Assistant',
        message: `# 🚀 代码质量改进执行计划

    基于刚才的分析，我为您制定了一个系统化的改进计划：

    ## 📅 阶段一：基础设施完善（预计 2-3 天）

    ### 1. 代码格式化工具链
    \`\`\`bash
    # 安装 ESLint 和 Prettier
    npm install -D eslint @vue/eslint-config-typescript prettier
    npm install -D @vue/eslint-config-prettier eslint-plugin-vue

    # 创建 .eslintrc.js 配置
    echo "module.exports = { extends: ['@vue/typescript/recommended'] }" > .eslintrc.js
    \`\`\`

    ### 2. TypeScript 配置优化
    \`\`\`js
  {
    "compilerOptions": {
      "strict": true,
      "noImplicitAny": true,
      "noUnusedLocals": true,
      "noUnusedParameters": true
    }
  }
  \`\`\`

  ## 📊 阶段二：代码质量提升（预计 3-4 天）

  | 优先级 | 任务 | 预计时间 | 负责人 |
  |--------|------|----------|---------|
  | 🔴 高 | 修复 TypeScript 警告 | 0.5天 | 开发者 |
  | 🟡 中 | 添加 ESLint 规则 | 1天 | 开发者 |
  | 🟢 低 | 统一代码风格 | 1天 | 全团队 |

  ### 具体修复清单：
  - [ ] **src/components/ToolBox.vue:23** - 添加明确类型注解
  - [ ] **src/pages/chat/ReAct.vue:156** - 移除未使用的导入
  - [ ] **全局** - 启用严格模式检查

  ## 🧪 阶段三：测试体系建设（预计 1-2 周）

  ### 单元测试框架
  \`\`\`bash
  # 安装 Vitest 测试框架
  npm install -D vitest @vue/test-utils jsdom

  # 创建测试配置
  npm run test:unit
  \`\`\`

  ### 测试覆盖率目标
  - **组件测试**: 达到 80% 覆盖率
  - **工具函数**: 达到 95% 覆盖率
  - **核心业务逻辑**: 达到 90% 覆盖率

  ## 📈 阶段四：性能优化（持续进行）

  ### 代码分割策略
  \`\`\`typescript
  // 路由级别的懒加载
  const Index = () => import('@/pages/chat/Index.vue')

  // 组件级别的异步加载
  const MessageItem = defineAsyncComponent(() => import('@/components/MessageItem.vue'))
  \`\`\`

  ---

  **💡 提示**: 这个计划可以根据团队情况和项目优先级进行调整。建议从阶段一开始，循序渐进地实施。

  您希望我详细说明哪个阶段的具体实施步骤？`,
        startTime: new Date(Date.now() - 60000),
        nodeId: 'complex-markdown-msg'
    },

    // 13. 系统状态消息
    {
        type: EventType.SYSTEM,
        sender: 'ReAct+ Assistant',
        message: `🔄 **系统状态更新**
    当前会话统计：
    - 消息总数: 13 条
    - 工具调用: 2 次
    - 代码分析: 已完成
    - 优化建议: 已生成
    系统运行正常，随时准备为您提供更多帮助。`,
        startTime: new Date(Date.now() - 40000),
        nodeId: 'system-status-msg'
    }
]
