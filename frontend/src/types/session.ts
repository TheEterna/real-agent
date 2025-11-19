import type {Component} from "vue";

export interface Session {
    id: string
    title: string
    agentType: AgentType
    updatedAt: Date
}
export enum AgentType {
    ReAct = 'ReAct',
    ReAct_Plus = 'ReAct+',
    Coding = 'coding',
}

