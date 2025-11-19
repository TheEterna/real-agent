export type ConnectionState = 'disconnected' | 'connecting' | 'connected' | 'error'
export type TaskState = 'idle' | 'running' | 'completed' | 'error'

export class ConnectionStatus {
  private _value: ConnectionState
  constructor(v: ConnectionState) {
    this._value = v
  }
  get value(): ConnectionState { return this._value }
  set(v: ConnectionState) { this._value = v }
  is(v: ConnectionState): boolean { return this._value === v }
}

export class TaskStatus {
  private _value: TaskState
  constructor(v: TaskState) {
    this._value = v
  }
  get value(): TaskState { return this._value }
  set(v: TaskState) { this._value = v }
  is(v: TaskState): boolean { return this._value === v }
}

export class ProgressInfo {
  text: string
  startTime: Date
  agentId?: string
  constructor(text: string, startTime: Date, agentId?: string) {
    this.text = text
    this.startTime = startTime
    this.agentId = agentId
  }
}
