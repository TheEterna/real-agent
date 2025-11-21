import type {Component} from "vue";

export interface Session {
    id: string
    title: string
    type: AgentType
    createdTime: Date,
    updatedTime: Date,
    isTemp: boolean
}
export enum AgentType {
    ReAct = 'ReAct',
    ReAct_Plus = 'ReAct+',
    Coding = 'coding',
}

