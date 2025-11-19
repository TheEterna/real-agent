package com.ai.agent.real.web.service;

/**
 * Server event types from Omni Realtime SDK (message.get("type")). Avoid magic strings by
 * mapping to enum.
 */
public enum OmniServerEventType {

	SESSION_CREATED("session.created"), SESSION_UPDATED("session.updated"), RESPONSE_CREATED("response.created"),
	RESPONSE_OUTPUT_ITEM_ADDED("response.output_item.added"), RESPONSE_TEXT_DELTA("response.text.delta"),
	RESPONSE_AUDIO_TRANSCRIPT_DELTA("response.audio_transcript.delta"), RESPONSE_AUDIO_DELTA("response.audio.delta"),
	RESPONSE_AUDIO_DONE("response.audio.done"), RESPONSE_DONE("response.done"),
	INPUT_AUDIO_BUFFER_COMMITTED("input_audio_buffer.committed"),
	INPUT_AUDIO_BUFFER_CLEARED("input_audio_buffer.cleared"),
	INPUT_AUDIO_BUFFER_SPEECH_STARTED("input_audio_buffer.speech_started"),
	INPUT_AUDIO_BUFFER_SPEECH_STOPPED("input_audio_buffer.speech_stopped"),
	CONVERSATION_ITEM_INPUT_AUDIO_TRANSCRIPTION_COMPLETED("conversation.item.input_audio_transcription.completed"),
	UNKNOWN("unknown");

	private final String type;

	OmniServerEventType(String type) {
		this.type = type;
	}

	public String type() {
		return type;
	}

	public static OmniServerEventType fromType(String t) {
		if (t == null)
			return UNKNOWN;
		for (OmniServerEventType v : values()) {
			if (v.type.equals(t))
				return v;
		}
		return UNKNOWN;
	}

}
