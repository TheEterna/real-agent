package com.ai.agent.real.web.service;

/**
 * POJO for parsing server event messages from Omni Realtime SDK.
 * Only includes fields we actually consume. Extend as needed.
 */
/**
 * POJO for parsing server event messages from Omni Realtime SDK. Only includes fields we
 * actually consume. Extend as needed.
 *
 * @author han
 * @time 2025/9/25 23:50
 */
public class OmniServerEvent {

	private OmniServerEventType type;

	private String delta; // for response.text.delta or response.audio.delta

	private AudioPayload audio; // optional nested payload for audio.delta

	private String transcript; // for
								// conversation.item.input_audio_transcription.completed

	private String sessionId; // for session.created -> session.id

	public OmniServerEvent() {
	}

	public OmniServerEventType getType() {
		return type;
	}

	public void setType(OmniServerEventType type) {
		this.type = type;
	}

	public String getDelta() {
		return delta;
	}

	public void setDelta(String delta) {
		this.delta = delta;
	}

	public AudioPayload getAudio() {
		return audio;
	}

	public void setAudio(AudioPayload audio) {
		this.audio = audio;
	}

	public String getTranscript() {
		return transcript;
	}

	public void setTranscript(String transcript) {
		this.transcript = transcript;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public static class AudioPayload {

		private String delta;

		public AudioPayload() {
		}

		public String getDelta() {
			return delta;
		}

		public void setDelta(String delta) {
			this.delta = delta;
		}

	}

}
