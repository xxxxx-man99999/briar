package org.briarproject.bramble.api.crypto;

import org.briarproject.nullsafety.NotNullByDefault;

import java.io.IOException;

/**
 * Compact, versioned wire encoding for post-quantum capabilities negotiated
 * by BQP. Version 1 is retained for backward decoding; version 2 carries the
 * full capability state, including send/receive direction flags.
 */
@NotNullByDefault
public final class PostQuantumCapabilitiesCodec {

	private static final byte ENCODING_VERSION_1 = 1;
	private static final byte ENCODING_VERSION_2 = 2;
	private static final byte ML_KEM_768 = 1;
	private static final byte SUPPORTED = 1;
	private static final byte ENABLED = 2;
	private static final byte SEND_ENABLED = 4;
	private static final byte RECEIVE_ENABLED = 8;

	private PostQuantumCapabilitiesCodec() {}

	/** Encodes all local capability state for a BQP capability record. */
	public static byte[] encode(PostQuantumCapabilities capabilities) {
		byte flags = 0;
		if (capabilities.isSupported()) flags |= SUPPORTED;
		if (capabilities.isEnabled()) flags |= ENABLED;
		if (capabilities.isSendEnabled()) flags |= SEND_ENABLED;
		if (capabilities.isReceiveEnabled()) flags |= RECEIVE_ENABLED;
		return new byte[] {ENCODING_VERSION_2, ML_KEM_768, flags};
	}

	/**
	 * Decodes a capability record. Version 1 remains accepted so a rolling
	 * upgrade can negotiate PQ without making classical BQP incompatible.
	 */
	public static PostQuantumCapabilities decode(byte[] encoded)
			throws IOException {
		if (encoded.length == 2 && encoded[0] == ENCODING_VERSION_1)
			return decodeV1(encoded[1]);
		if (encoded.length != 3 || encoded[0] != ENCODING_VERSION_2)
			throw new IOException("Invalid PQ capability record");
		if (encoded[1] != ML_KEM_768)
			return PostQuantumCapabilities.local(false);
		byte flags = encoded[2];
		return new PostQuantumCapabilities(
				(flags & SUPPORTED) != 0,
				(flags & ENABLED) != 0,
				(flags & SEND_ENABLED) != 0,
				(flags & RECEIVE_ENABLED) != 0);
	}

	private static PostQuantumCapabilities decodeV1(byte algorithm) {
		if (algorithm == ML_KEM_768)
			return PostQuantumCapabilities.local(true);
		return PostQuantumCapabilities.local(false);
	}
}
