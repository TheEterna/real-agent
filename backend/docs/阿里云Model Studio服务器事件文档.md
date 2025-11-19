# 阿里云 Model Studio 服务器事件文档

## 概述

本文档详细描述了阿里云 Model Studio 实时多模态 API 的服务器事件。这些事件用于客户端与服务器之间的实时通信，涵盖会话管理、音频处理、对话交互和响应生成等各个环节。

## 事件分类

### 1. 错误事件



*   [error](#error)

### 2. 会话管理事件



*   [session.created](#sessioncreated)

*   [session.updated](#sessionupdated)

### 3. 音频缓冲区事件



*   [input\_audio\_buffer.speech\_started](#input_audio_bufferspeech_started)

*   [input\_audio\_buffer.speech\_stopped](#input_audio_bufferspeech_stopped)

*   [input\_audio\_buffer.committed](#input_audio_buffercommitted)

*   [input\_audio\_buffer.cleared](#input_audio_buffercleared)

### 4. 对话项事件



*   [conversation.item.created](#conversationitemcreated)

*   [conversation.item.input\_audio\_transcription.completed](#conversationiteminput_audio_transcriptioncompleted)

*   [conversation.item.input\_audio\_transcription.failed](#conversationiteminput_audio_transcriptionfailed)

### 5. 响应事件



*   [response.created](#responsecreated)

*   [response.done](#responsedone)

*   [response.text.delta](#responsetextdelta)

*   [response.text.done](#responsetextdone)

*   [response.audio.delta](#responseaudiodelta)

*   [response.audio.done](#responseaudiodone)

*   [response.audio\_transcript.delta](#responseaudio_transcriptdelta)

*   [response.audio\_transcript.done](#responseaudio_transcriptdone)

*   [response.output\_item.added](#responseoutput_itemadded)

*   [response.output\_item.done](#responseoutput_itemdone)

*   [response.content\_part.added](#responsecontent_partadded)

*   [response.content\_part.done](#responsecontent_partdone)



***

## 事件详细说明

### error

**描述**：不论是遇到客户端错误还是服务端错误，服务端会返回错误信息。通常来说，大多数错误都是可恢复的，不会影响会话继续进行。

**参数说明**：



| 参数            | 类型     | 说明                              |
| ------------- | ------ | ------------------------------- |
| type          | string | 事件类型，该事件下固定为 `error`            |
| error         | object | 错误的详细信息                         |
| error.type    | string | 错误类型                            |
| error.code    | string | 错误码                             |
| error.message | string | 错误信息                            |
| error.param   | string | 与错误相关的参数，如 `session.modalities` |

**示例**：



```
{

&#x20; "event\_id": "event\_RoUu4T8yExPMI37GKwaOC",

&#x20; "type": "error",

&#x20; "error": {

&#x20;   "type": "invalid\_parameter",

&#x20;   "code": "INVALID\_PARAMETER",

&#x20;   "message": "Invalid modalities: \['audio']. Supported combinations are: \['text'] and \['audio', 'text'].",

&#x20;   "param": "session.modalities"

&#x20; }

}
```



***

### session.created

**描述**：当客户端链接到服务端后，服务端响应的第一个事件，该事件返回时会携带服务端对此次链接的默认配置信息。

**参数说明**：



| 参数                 | 类型     | 说明                                                              |
| ------------------ | ------ | --------------------------------------------------------------- |
| type               | string | 事件类型，该事件下固定为 `session.created`                                  |
| session            | object | session 配置                                                      |
| session.modalities | array  | 模型输出模态设置，支持设置 \["text"] 或 \["text","audio"]，不支持单独设置为 \["audio"] |
| session.voice      | string | 模型生成音频的音色，支持："Cherry", "Serena", "Ethan", "Cherry"             |



***

### session.updated

**描述**：当接受到用户的 `session.update` 请求并正确处理后返回。如果出现错误，则直接返回 `error` 事件。

**参数说明**：



| 参数                  | 类型     | 说明                                                         |
| ------------------- | ------ | ---------------------------------------------------------- |
| type                | string | 事件类型，该事件下固定为 `session.updated`                             |
| session             | object | session 配置                                                 |
| session.temperature | float  | 模型的温度参数，范围是 \[0,2)                                         |
| session.modalities  | array  | 模型输出模态设置，支持设置 \["text"]（仅输出文本）或 \["text","audio"]（输出音频和文本） |
| session.voice       | string | 模型生成音频的音色，支持："Cherry", "Serena", "Ethan", "Cherry"        |



***

### input\_audio\_buffer.speech\_started

**描述**：`server_vad` 模式下，服务器在音频缓冲区中检测到语音开始时，系统会返回服务器 `input_audio_buffer.speech_started` 事件。每当音频添加到缓冲区时，此事件都可能发生（除非已检测到语音）。

**参数说明**：



| 参数               | 类型      | 说明                                               |
| ---------------- | ------- | ------------------------------------------------ |
| event\_id        | string  | 本次事件的标识                                          |
| type             | string  | 事件类型，该事件下固定为 `input_audio_buffer.speech_started` |
| audio\_start\_ms | integer | 在会话期间，从音频开始写入缓冲区到首次检测到语音时的毫秒数                    |
| item\_id         | string  | 将创建的用户消息项的 ID                                    |

**示例**：



```
{

&#x20; "event\_id": "event\_MOcdMTKH1QQRP5mbGWPHA",

&#x20; "type": "input\_audio\_buffer.speech\_started",

&#x20; "audio\_start\_ms": 1234,

&#x20; "item\_id": "item\_123456789"

}
```



***

### input\_audio\_buffer.speech\_stopped

**描述**：`server_vad` 模式下，服务器在音频缓冲区中检测到语音结束时，系统会返回服务器 `input_audio_buffer.speech_stopped` 事件。服务器还将发送一个带有由音频缓冲区创建的用户消息项的 `conversation.item.created` 事件。

**参数说明**：



| 参数             | 类型      | 说明                                               |
| -------------- | ------- | ------------------------------------------------ |
| event\_id      | string  | 本次事件的标识                                          |
| type           | string  | 事件类型，该事件下固定为 `input_audio_buffer.speech_stopped` |
| audio\_end\_ms | integer | 自会话开始以来，语音停止时所经过的毫秒数                             |
| item\_id       | string  | 当语音停止时将创建的用户消息项的 ID                              |

**示例**：



```
{

&#x20; "event\_id": "event\_YmcGFfICPRXBDfgqcpKit",

&#x20; "type": "input\_audio\_buffer.speech\_stopped",

&#x20; "audio\_end\_ms": 2823,

&#x20; "item\_id": "item\_Fu4Bf8idul8hfJ"

}
```



***

### input\_audio\_buffer.committed

**描述**：在 `server_vad` 模式下，当检测到用户说话结束时，服务端会自动提交并返回此事件。在非 `server_vad` 模式下，当客户端完成音频发送 `input_audio_buffer.commit` 事件的服务端响应。

**参数说明**：



| 参数        | 类型     | 说明                                          |
| --------- | ------ | ------------------------------------------- |
| event\_id | string | 本次事件的标识                                     |
| type      | string | 事件类型，该事件下固定为 `input_audio_buffer.committed` |
| item\_id  | string | 将创建的用户消息项的 ID                               |

**示例**：



```
{

&#x20; "event\_id": "event\_WwJkVCuJE8CNaRlMWdn8U",

&#x20; "type": "input\_audio\_buffer.committed",

&#x20; "item\_id": "item\_Fu4Bf8idul8hfJ"

}
```



***

### input\_audio\_buffer.cleared

**描述**：客户端发送 `input_audio_buffer.clear` 事件后，服务端将返回 `input_audio_buffer.cleared` 事件。

**参数说明**：



| 参数        | 类型     | 说明                                        |
| --------- | ------ | ----------------------------------------- |
| event\_id | string | 本次事件的标识                                   |
| type      | string | 事件类型，该事件下固定为 `input_audio_buffer.cleared` |

**示例**：



```
{

&#x20; "event\_id": "event\_1121",

&#x20; "type": "input\_audio\_buffer.cleared"

}
```



***

### conversation.item.created

**描述**：当对话项创建时返回该事件。

**参数说明**：



| 参数           | 类型     | 说明                                       |
| ------------ | ------ | ---------------------------------------- |
| event\_id    | string | 本次事件的标识                                  |
| type         | string | 事件类型，该事件下固定为 `conversation.item.created` |
| item         | object | 要添加到对话中的条目                               |
| item.id      | string | 对话项的唯一 ID                                |
| item.object  | string | 始终为 `realtime.item`                      |
| item.status  | string | 对话项的状态                                   |
| item.role    | string | 消息发送的角色                                  |
| item.content | array  | 消息的内容                                    |

**示例**：



```
{

&#x20; "event\_id": "event\_FecZDYhYi4LlVyjsbtyMa",

&#x20; "type": "conversation.item.created",

&#x20; "item": {

&#x20;   "id": "item\_Fu4Bf8idul8hfJ",

&#x20;   "object": "realtime.item",

&#x20;   "status": "in\_progress",

&#x20;   "role": "user",

&#x20;   "content": \[]

&#x20; }

}
```



***

### conversation.item.input\_audio\_transcription.completed

**描述**：此事件是用户音频写入用户音频缓冲区后的音频转录输出。Realtime 模型可以接收音频输入，输入音频的转录是一个在单独的自动语音识别模型上运行的独立过程，目前始终为 `gummy-realtime-v1`。转录文本可能与模型的解释有所不同，可以被作为参考。

**参数说明**：



| 参数             | 类型      | 说明                                                                   |
| -------------- | ------- | -------------------------------------------------------------------- |
| event\_id      | string  | 本次事件的标识                                                              |
| type           | string  | 事件类型，该事件下固定为 `conversation.item.input_audio_transcription.completed` |
| item\_id       | string  | 包含音频的用户消息项的 ID                                                       |
| content\_index | integer | 包含音频的内容部分的索引                                                         |

**示例**：



```
{

&#x20; "event\_id": "event\_OHMnbeeCHHoVrJDhFBMNY",

&#x20; "type": "conversation.item.input\_audio\_transcription.completed",

&#x20; "item\_id": "item\_Fu4Bf8idul8hfJ",

&#x20; "content\_index": 0

}
```



***

### conversation.item.input\_audio\_transcription.failed

**描述**：当开启输入音频转写且用户音频转写失败时，系统会返回服务器 `conversation.item.input_audio_transcription.failed` 事件。此事件是与其他 `error` 事件分开的，以便客户端能够识别相关项。

**参数说明**：



| 参数             | 类型      | 说明                                                                |
| -------------- | ------- | ----------------------------------------------------------------- |
| type           | string  | 事件类型，该事件下固定为 `conversation.item.input_audio_transcription.failed` |
| item\_id       | string  | 用户消息条目的 ID                                                        |
| content\_index | integer | 包含音频的内容部分的索引                                                      |
| error          | object  | 转录的文本内容                                                           |
| error.code     | string  | 错误码                                                               |
| error.message  | string  | 错误消息                                                              |
| error.param    | string  | 错误相关的参数                                                           |



***

### response.created

**描述**：当服务端生成新的模型响应时，会先发送此事件。

**参数说明**：



| 参数                        | 类型     | 说明                                                             |
| ------------------------- | ------ | -------------------------------------------------------------- |
| type                      | string | 该事件下固定为 `response.created`                                     |
| event\_id                 | string | 本次事件的标识                                                        |
| response                  | object | 响应对象                                                           |
| response.id               | string | 响应的唯一 ID                                                       |
| response.conversation\_id | string | 当前会话的唯一 id                                                     |
| response.object           | string | 对象类型，此事件下固定为 `realtime.response`                               |
| response.status           | string | 响应的最终状态，取值范围 \[completed, failed, in\_progress, or incomplete] |
| response.modalities       | array  | 响应的模态                                                          |
| response.voice            | string | 模型生成音频的音色                                                      |
| response.output           | array  | 此事件下目前为空                                                       |

**示例**：



```
{

&#x20; "event\_id": "event\_JIyHMxVNc9gWgflLBiH1w",

&#x20; "type": "response.created",

&#x20; "response": {

&#x20;   "id": "resp\_P79OOMs8LnrXVpiIHUCKR",

&#x20;   "object": "realtime.response",

&#x20;   "conversation\_id": "conv\_NGFEtyikW1PRDopyZ52Yv",

&#x20;   "status": "in\_progress",

&#x20;   "modalities": \["text", "audio"],

&#x20;   "voice": "Cherry",

&#x20;   "output\_audio\_format": "pcm16",

&#x20;   "output": \[]

&#x20; }

}
```



***

### response.done

**描述**：当响应生成完成时，服务端会返回此事件。该事件中包含的 Response 对象将包含 Response 中的所有输出项，但不包括已返回的原始音频数据。

**参数说明**：



| 参数                        | 类型     | 说明                                                             |
| ------------------------- | ------ | -------------------------------------------------------------- |
| type                      | string | 固定为 `response.done`                                            |
| response                  | object | 响应对象                                                           |
| response.id               | string | 响应的唯一 ID                                                       |
| response.object           | string | 对象类型，此事件下固定为 `realtime.response`                               |
| response.conversation\_id | string | 当前会话的唯一 id                                                     |
| response.status           | string | 响应的最终状态，取值范围 \[completed, failed, in\_progress, or incomplete] |
| response.modalities       | array  | 响应的模态                                                          |
| response.voice            | string | 模型生成音频的音色                                                      |
| response.output           | array  | 响应的输出                                                          |
| response.output.id        | string | 响应输出对应的 ID                                                     |
| response.output.object    | string | 输出项的对象类型，当前固定为 "realtime.item"                                 |
| response.output.type      | string | 输出项的类型，当前固定为 "message"                                         |
| response.output.status    | string | 输出项的状态，取值范围 \["completed","incompleted"]                       |
| response.output.role      | string | 输出项的角色，取值范围 \["user","assistant","system"]                     |
| response.output.content   | object | 输出项的具体内容                                                       |
| response.usage            | object | 本次响应对应的 usage 信息                                               |

**content 说明**：



*   当用户输入是文本时，则返回 `type=text`，`text={大模型推理结果}`

*   当用户输入是音频时，则返回 `type=audio`，`transcript={大模型推理结果}`

**示例**：



```
{

&#x20; "event\_id": "event\_W7QDmx8EWyInRnRp1O7Df",

&#x20; "type": "response.done",

&#x20; "response": {

&#x20;   "id": "resp\_P79OOMs8LnrXVpiIHUCKR",

&#x20;   "object": "realtime.response",

&#x20;   "conversation\_id": "conv\_NGFEtyikW1PRDopyZ52Yv",

&#x20;   "status": "completed",

&#x20;   "modalities": \["text", "audio"],

&#x20;   "voice": "Cherry",

&#x20;   "output\_audio\_format": "pcm16",

&#x20;   "output": \[

&#x20;     {

&#x20;       "id": "item\_123456789",

&#x20;       "object": "realtime.item",

&#x20;       "type": "message",

&#x20;       "status": "completed",

&#x20;       "role": "assistant",

&#x20;       "content": \[

&#x20;         {

&#x20;           "type": "audio",

&#x20;           "transcript": "你好，我是阿里云研发的大规模语言模型，我叫通义千问。"

&#x20;         }

&#x20;       ]

&#x20;     }

&#x20;   ],

&#x20;   "usage": {

&#x20;     "total\_tokens": 261,

&#x20;     "cached\_tokens": 0,

&#x20;     "input\_tokens": 127,

&#x20;     "output\_tokens": 134,

&#x20;     "audio\_tokens": 120

&#x20;   }

&#x20; }

}
```



***

### response.text.delta

**描述**：模型增量生成新的文本时，系统会返回服务器 `response.text.delta` 事件。

**参数说明**：



| 参数            | 类型      | 说明                        |
| ------------- | ------- | ------------------------- |
| type          | string  | 固定为 `response.text.delta` |
| response      | object  | 响应对象                      |
| item\_id      | string  | 消息项 ID，可以关联同一个消息项         |
| output\_index | integer | 响应中输出项的索引，目前固定为 0         |
| delta         | string  | 返回的增量文本                   |



***

### response.text.done

**描述**：当模型生成的文本结束时，系统会返回服务器 `response.text.done` 事件。当响应中断、不完整或取消时，系统也会返回此事件。

**参数说明**：



| 参数            | 类型      | 说明          |
| ------------- | ------- | ----------- |
| output\_index | integer | 响应输出项的索引    |
| text          | string  | 模型输出的最终完整文本 |

**示例**：



```
{

&#x20; "event\_id": "event\_B1lIeE2Nac33zn5V7h2mm",

&#x20; "type": "response.text.done",

&#x20; "output\_index": 0,

&#x20; "text": "你好，我是阿里云研发的大规模语言模型，我叫通义千问。"

}
```



***

### response.audio.delta

**描述**：当模型增量生成新的音频数据时，系统会返回服务器 `response.audio.delta` 事件。

**参数说明**：



| 参数    | 类型     | 说明                            |
| ----- | ------ | ----------------------------- |
| delta | string | 模型增量输出的 audio 数据，使用 Base64 编码 |



***

### response.audio.done

**描述**：当模型完成生成音频数据时，系统会返回服务器 `response.audio.done` 事件。当响应中断、不完整或取消时，系统也会返回此事件。

**参数说明**：



| 参数   | 类型     | 说明                        |
| ---- | ------ | ------------------------- |
| type | string | 固定为 `response.audio.done` |

**示例**：



```
{

&#x20; "event\_id": "event\_123456789",

&#x20; "type": "response.audio.done"

}
```



***

### response.audio\_transcript.delta

**描述**：当模型增量生成新的音频对应的文本时，系统会返回服务器 `response.audio_transcript.delta` 事件。

**参数说明**：



| 参数           | 类型     | 说明                                  |
| ------------ | ------ | ----------------------------------- |
| response\_id | string | response\_id，可以关联同一个 response 的所有输出 |
| delta        | string | 增量文本内容                              |

**示例**：



```
{

&#x20; "event\_id": "event\_OcoAVmmbMQnirKeVFag9x",

&#x20; "type": "response.audio\_transcript.delta",

&#x20; "response\_id": "resp\_P79OOMs8LnrXVpiIHUCKR",

&#x20; "delta": "你好，我是阿里云研发的大规模语言模型，我叫通义千问。"

}
```



***

### response.audio\_transcript.done

**描述**：当模型完成生成新的音频对应的文本时，系统会返回服务器 `response.audio_transcript.done` 事件。

**参数说明**：



| 参数             | 类型      | 说明                                   |
| -------------- | ------- | ------------------------------------ |
| type           | string  | 固定为 `response.audio_transcript.done` |
| response\_id   | string  | response\_id，可以关联同一个 response 的所有输出  |
| item\_id       | string  | 消息项 id，可以关联同一个消息 item                |
| output\_index  | integer | response 中输出项的索引，目前固定为 0             |
| content\_index | integer | response 中输出项中内部部分的索引，目前固定为 0        |
| part           | object  | 最终生成的完整文本                            |

**示例**：



```
{

&#x20; "event\_id": "event\_VN4Q4GJugLcc1S23viW8E",

&#x20; "type": "response.audio\_transcript.done",

&#x20; "response\_id": "resp\_P79OOMs8LnrXVpiIHUCKR",

&#x20; "item\_id": "item\_123456789",

&#x20; "output\_index": 0,

&#x20; "content\_index": 0,

&#x20; "part": {

&#x20;   "text": "你好，我是阿里云研发的大规模语言模型，我叫通义千问。"

&#x20; }

}
```



***

### response.output\_item.added

**描述**：当新的输出项需要输出时，服务端返回此事件。

**参数说明**：



| 参数            | 类型      | 说明                                  |
| ------------- | ------- | ----------------------------------- |
| type          | string  | 固定为 `response.output_item.added`    |
| response\_id  | string  | response\_id，可以关联同一个 response 的所有输出 |
| output\_index | integer | response 中输出项的索引，目前固定为 0            |
| item          | object  | 输出项信息                               |
| item.id       | string  | 输出项的唯一 ID                           |
| item.object   | string  | 始终为 `realtime.item`                 |
| item.status   | string  | 输出项的状态                              |
| item.role     | string  | 消息发送的角色                             |

**示例**：



```
{

&#x20; "event\_id": "event\_B4O5yPt3Gjnjy5eYH3plG",

&#x20; "type": "response.output\_item.added",

&#x20; "response\_id": "resp\_P79OOMs8LnrXVpiIHUCKR",

&#x20; "output\_index": 0,

&#x20; "item": {

&#x20;   "id": "item\_123456789",

&#x20;   "object": "realtime.item",

&#x20;   "status": "in\_progress",

&#x20;   "role": "assistant"

&#x20; }

}
```



***

### response.output\_item.done

**描述**：当新的输出项输出完成时，服务端返回此事件。

**参数说明**：



| 参数            | 类型      | 说明                              |
| ------------- | ------- | ------------------------------- |
| type          | string  | 固定为 `response.output_item.done` |
| response\_id  | string  | 响应的 ID                          |
| output\_index | integer | response 中输出项的索引，目前固定为 0        |
| item          | object  | 输出项信息                           |
| item.id       | string  | 输出项的唯一 ID                       |
| item.object   | string  | 始终为 `realtime.item`             |
| item.status   | string  | 输出项的状态                          |
| item.role     | string  | 消息发送的角色                         |

**示例**：



```
{

&#x20; "event\_id": "event\_XkiwbYTBC9Wcdwy6uYJ2G",

&#x20; "type": "response.output\_item.done",

&#x20; "response\_id": "resp\_P79OOMs8LnrXVpiIHUCKR",

&#x20; "output\_index": 0,

&#x20; "item": {

&#x20;   "id": "item\_123456789",

&#x20;   "object": "realtime.item",

&#x20;   "status": "completed",

&#x20;   "role": "assistant",

&#x20;   "content": \[

&#x20;     {

&#x20;       "type": "audio",

&#x20;       "transcript": "你好，我是阿里云研发的大规模语言模型，我叫通义千问。"

&#x20;     }

&#x20;   ]

&#x20; }

}
```



***

### response.content\_part.added

**描述**：当新的内容项需要输出时，服务端返回此事件。

**参数说明**：



| 参数             | 类型      | 说明                                |
| -------------- | ------- | --------------------------------- |
| type           | string  | 固定为 `response.content_part.added` |
| response\_id   | string  | 响应的 ID                            |
| item\_id       | string  | 消息项 ID                            |
| output\_index  | integer | 响应输出项的索引，目前固定为 0                  |
| content\_index | integer | 响应输出项中内部部分的索引，目前固定为 0             |
| part           | object  | 已完成的内容部分                          |
| part.type      | string  | 内容部分的类型                           |
| part.text      | string  | 内容部分的文本                           |

**示例**：



```
{

&#x20; "event\_id": "event\_J2UixwYKZsXg7c9YXZetL",

&#x20; "type": "response.content\_part.added",

&#x20; "response\_id": "resp\_P79OOMs8LnrXVpiIHUCKR",

&#x20; "item\_id": "item\_OFaPGtzfWCPyGzxnuEX9i",

&#x20; "output\_index": 0,

&#x20; "content\_index": 0,

&#x20; "part": {

&#x20;   "type": "audio",

&#x20;   "text": "你好，我是阿里云研发的大规模语言模型，我叫通义千问。"

&#x20; }

}
```



***

### response.content\_part.done

**描述**：当新的内容项输出完成时，服务端返回此事件。

**参数说明**：



| 参数             | 类型      | 说明                               |
| -------------- | ------- | -------------------------------- |
| type           | string  | 固定为 `response.content_part.done` |
| response\_id   | string  | 响应的 ID                           |
| item\_id       | string  | 消息项 ID                           |
| output\_index  | integer | 响应输出项的索引，目前固定为 0                 |
| content\_index | integer | 响应输出项中内部部分的索引，目前固定为 0            |
| part           | object  | 已完成的内容部分                         |
| part.type      | string  | 内容部分的类型                          |

**示例**：



```
{

&#x20; "event\_id": "event\_FdVUyXIa8WVk4BZJv8swq",

&#x20; "type": "response.content\_part.done",

&#x20; "response\_id": "resp\_QeZcSlvzRmmjIURRMafY8",

&#x20; "item\_id": "item\_OFaPGtzfWCPyGzxnuEX9i",

&#x20; "output\_index": 0,

&#x20; "content\_index": 0,

&#x20; "part": {

&#x20;   "type": "audio"

&#x20; }

}
```



***

## 总结

本文档详细介绍了阿里云 Model Studio 实时多模态 API 的所有服务器事件，包括事件类型、参数说明和示例。这些事件涵盖了从会话建立到音频处理、从对话交互到响应生成的完整流程。

### 关键要点：



1.  **会话管理**：通过 `session.created` 和 `session.updated` 事件管理会话配置

2.  **音频处理**：使用 `input_audio_buffer` 相关事件处理语音输入

3.  **错误处理**：统一的 `error` 事件机制处理各种错误情况

4.  **响应生成**：多层次的响应事件（`response.created`、`response.done`、增量事件等）支持实时交互

5.  **状态跟踪**：通过各种 `done` 事件跟踪操作完成状态

### 使用建议：



*   根据业务需求监听相应的事件

*   合理处理增量事件以实现流畅的用户体验

*   妥善处理错误事件确保系统稳定性

*   利用事件 ID 和关联 ID 建立完整的事件链路



***

**文档版本**：v1.0

**最后更新**：2025 年 9 月 26 日

> （注：文档部分内容可能由 AI 生成）