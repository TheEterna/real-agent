## function calling

### easy example:

1. first request:
```json
    messages: [
        {
            "role": "user",
            "content": "What is the weather like in Beijing?"
        }
    ],
    "tools": [
        {
            "name": "get_current_weather",
            "arguments": {
                "location": "Beijing, China",
                "unit": "celsius"
            }
        }
    ]
```
2. first response (md, different model have different response content, only have a common format, such as qwen, the content of field is null, but some model are not empty, fuck
make it difficult for font-end rendering to achieve universality
):
```json
    {
        "role": "assistant",
        "content": null or (use get_current_weather function to achieve this task),
        "tool_calls": [
            {
                "id": "call_123",
                "type": "function",
                "function": {
                    "name": "get_current_weather",
                    "arguments": "{\"location\": \"Beijing, China\", \"unit\": \"celsius\"}"
                }
            }
        ]
    }
```

after response, we need to send a second request or do some things to get the result of the function call
3. second request (need to carry the tool call result):
add the following message
``` json
    {
      "role": "tool",
      "content": "25 degrees celsius",
      "tool_call_id": "call_123"
    }
```
4. 回复, 结束对话 finish dialog

然后很重要的, springAI 默认封装了2-4, 使用 returnDirect = true 封装2-3, 所以我们难以通过 结构化的校验来监听tool的调用, 什么意思呢?


正常使用, 不加 returnDirect = true, 进入步骤1, 相当于步骤2-3都是不可见的, 直接返回回答结果,示例: 今天北京天气为28度
加 returnDirect = true, 进入步骤1, 直接给你返回
``` json
{
    [
        {
            "id": call_ef31a0f166f7423b88c5f9, 
            "name": 完成任务, 
            "responseData": "{
                "ok":true,
                "message": null,
                "code":null,
                "elapsedMs":0,
                "data":"task_done",
                "meta":{}
            }"
        }
    ], 
  "messageType": "TOOL",
  "metadata": {
    "messageType": "TOOL"
  }
}
```
springAi 进行了 封装, 封装代码如下:

``` java
	/**
	 * Build a list of {@link Generation} from the tool execution result, useful for
	 * sending the tool execution result to the client directly.
	 */
	static List<Generation> buildGenerations(ToolExecutionResult toolExecutionResult) {
		List<Message> conversationHistory = toolExecutionResult.conversationHistory();
		List<Generation> generations = new ArrayList<>();
		if (conversationHistory
			.get(conversationHistory.size() - 1) instanceof ToolResponseMessage toolResponseMessage) {
			toolResponseMessage.getResponses().forEach(response -> {
				AssistantMessage assistantMessage = new AssistantMessage(response.responseData());
				Generation generation = new Generation(assistantMessage,
						ChatGenerationMetadata.builder()
							.metadata(METADATA_TOOL_ID, response.id())
							.metadata(METADATA_TOOL_NAME, response.name())
							.finishReason(FINISH_REASON)
							.build());
				generations.add(generation);
			});
		}
		return generations;
	}
```
可使用optionsBuilder.internalToolExecutionEnabled(false); 使用原生方式, 不进行任何封装
