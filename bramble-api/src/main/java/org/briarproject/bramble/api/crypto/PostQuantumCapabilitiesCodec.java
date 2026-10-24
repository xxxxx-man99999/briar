package org.briarproject.bramble.api.crypto;

import org.briarproject.nullsafety.NotNullByDefault;

import java.io.IOException;

/**
 * Compact wire encoding for post-quantum capabilities negotiated by BQP.
 *
 * <p>The encoding is intentionally small and versioned so the capability
 * record can be ignored by older BQP implementations. The current encoding
 * advertises ML-KEM-768 only.</p>
 */
@NotNullByDefault
public final class PostQuantumCapabilitiesCodec {

	private static final byte ENCODING_VERSION = 1;
	private static final byte ML_KEM_768 = 1;

	private PostQuantumCapabilitiesCodec() {}

	/** Encodes the local capabilities for a BQP capability record. */
	public static byte[] encode(PostQuantumCapabilities capabilities) {
		if (!capabilities.isSupported() || !capabilities.isEnabled())
			return new byte[] {ENCODING_VERSION, 0};
		return new byte[] {ENCODING_VERSION, ML_KEM_768};
	}

	/**
	 * Decodes a capability record. Unknown algorithms are treated as
	 * unsupported rather than causing the BQP connection to fail.
	 */
	public static PostQuantumCapabilities decode(byte[] encoded)
			throws IOException {
		if (encoded.length != 2 || encoded[0] != ENCODING_VERSION)
			throw new IOException("Invalid PQ capability record");
		if (encoded[1] == ML_KEM_768)
			return PostQuantumCapabilities.local(true);
		if (encoded[1] == 0)
			return PostQuantumCapabilities.local(false);
		return PostQuantumCapabilities.local(false);
	}
}
