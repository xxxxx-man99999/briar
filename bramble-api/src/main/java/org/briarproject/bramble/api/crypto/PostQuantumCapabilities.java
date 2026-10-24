package org.briarproject.bramble.api.crypto;

import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

/** Negotiated post-quantum capabilities, modeled after SimpleX's per-connection PQ state. */
@Immutable
@NotNullByDefault
public final class PostQuantumCapabilities {

	private final boolean supported;
	private final boolean enabled;
	private final boolean sendEnabled;
	private final boolean receiveEnabled;

	public PostQuantumCapabilities(boolean supported, boolean enabled,
			boolean sendEnabled, boolean receiveEnabled) {
		this.supported = supported;
		this.enabled = enabled;
		this.sendEnabled = sendEnabled;
		this.receiveEnabled = receiveEnabled;
	}

	public static PostQuantumCapabilities local(boolean supported) {
		return new PostQuantumCapabilities(supported, supported, supported, supported);
	}

	public boolean isSupported() { return supported; }
	public boolean isEnabled() { return enabled; }
	public boolean isSendEnabled() { return sendEnabled; }
	public boolean isReceiveEnabled() { return receiveEnabled; }

	/** Hybrid PQ is usable only when both peers can send and receive it. */
	public boolean canUseHybridWith(PostQuantumCapabilities peer) {
		return enabled && peer.enabled && supported && peer.supported
				&& sendEnabled && peer.receiveEnabled
				&& receiveEnabled && peer.sendEnabled;
	}
}
