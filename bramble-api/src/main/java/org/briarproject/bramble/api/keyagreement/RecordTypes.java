package org.briarproject.bramble.api.keyagreement;

/**
 * Record types for BQP.
 */
public interface RecordTypes {

	byte KEY = 0;
	byte CONFIRM = 1;
	byte ABORT = 2;

	/** Optional post-quantum capability advertisement. */
	byte PQ_CAPABILITIES = 3;

	/** ML-KEM public key used by the optional hybrid handshake. */
	byte PQ_PUBLIC_KEY = 4;

	/** ML-KEM ciphertext used by the optional hybrid handshake. */
	byte PQ_CIPHERTEXT = 5;
}
