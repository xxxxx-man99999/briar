package org.briarproject.bramble.keyagreement;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPublicKeyParameters;
import org.briarproject.bramble.api.crypto.CryptoComponent;
import org.briarproject.bramble.api.crypto.KeyAgreementCrypto;
import org.briarproject.bramble.api.crypto.KeyPair;
import org.briarproject.bramble.api.crypto.KeyParser;
import org.briarproject.bramble.api.crypto.PostQuantumCapabilities;
import org.briarproject.bramble.api.crypto.PostQuantumCapabilitiesCodec;
import org.briarproject.bramble.api.crypto.PublicKey;
import org.briarproject.bramble.api.crypto.SecretKey;
import org.briarproject.bramble.api.keyagreement.Payload;
import org.briarproject.bramble.api.keyagreement.PayloadEncoder;
import org.briarproject.bramble.crypto.HybridKeyAgreement;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import static org.briarproject.bramble.api.keyagreement.KeyAgreementConstants.MASTER_KEY_LABEL;
import static org.briarproject.bramble.api.keyagreement.KeyAgreementConstants.PROTOCOL_VERSION;
import static org.briarproject.bramble.api.keyagreement.KeyAgreementConstants.SHARED_SECRET_LABEL;

/**
 * BQP protocol with mandatory hybrid classical + ML-KEM-768 key agreement.
 * A session cannot succeed with a classical-only key.
 */
@NotNullByDefault
class KeyAgreementProtocol {

	interface Callbacks {
		void connectionWaiting();
		void initialRecordReceived();
	}

	private final Callbacks callbacks;
	private final CryptoComponent crypto;
	private final KeyAgreementCrypto keyAgreementCrypto;
	private final PayloadEncoder payloadEncoder;
	private final KeyAgreementTransport transport;
	private final Payload theirPayload, ourPayload;
	private final KeyPair ourKeyPair;
	private final boolean alice;

	KeyAgreementProtocol(Callbacks callbacks, CryptoComponent crypto,
			KeyAgreementCrypto keyAgreementCrypto, PayloadEncoder payloadEncoder,
			KeyAgreementTransport transport, Payload theirPayload,
			Payload ourPayload, KeyPair ourKeyPair, boolean alice) {
		this(callbacks, crypto, keyAgreementCrypto, payloadEncoder, transport,
				theirPayload, ourPayload, ourKeyPair, alice, true);
	}

	// pqEnabled is retained for source compatibility; PQ is now mandatory.
	KeyAgreementProtocol(Callbacks callbacks, CryptoComponent crypto,
			KeyAgreementCrypto keyAgreementCrypto, PayloadEncoder payloadEncoder,
			KeyAgreementTransport transport, Payload theirPayload,
			Payload ourPayload, KeyPair ourKeyPair, boolean alice,
			boolean pqEnabled) {
		this.callbacks = callbacks;
		this.crypto = crypto;
		this.keyAgreementCrypto = keyAgreementCrypto;
		this.payloadEncoder = payloadEncoder;
		this.transport = transport;
		this.theirPayload = theirPayload;
		this.ourPayload = ourPayload;
		this.ourKeyPair = ourKeyPair;
		this.alice = alice;
	}

	SecretKey perform() throws AbortException, IOException {
		try {
			if (!HybridKeyAgreement.isSupported()) throw new AbortException();

			PostQuantumCapabilities localPq = PostQuantumCapabilities.local(true);
			transport.sendPqCapabilities(localPq);
			PostQuantumCapabilities peerPq = transport.receivePqCapabilities();
			if (!localPq.canUseHybridWith(peerPq)) throw new AbortException();

			PublicKey theirPublicKey;
			if (alice) {
				sendKey();
				callbacks.connectionWaiting();
				theirPublicKey = receiveKey();
			} else {
				theirPublicKey = receiveKey();
				sendKey();
			}

			SecretKey classical = deriveSharedSecret(theirPublicKey);
			SecretKey sessionSecret = deriveHybridSecret(classical, localPq, peerPq);

			if (alice) {
				sendConfirm(sessionSecret, theirPublicKey);
				receiveConfirm(sessionSecret, theirPublicKey);
			} else {
				receiveConfirm(sessionSecret, theirPublicKey);
				sendConfirm(sessionSecret, theirPublicKey);
			}
			return crypto.deriveKey(MASTER_KEY_LABEL, sessionSecret);
		} catch (AbortException e) {
			sendAbort(e.getCause() != null);
			throw e;
		}
	}

	private void sendKey() throws IOException {
		transport.sendKey(ourKeyPair.getPublic().getEncoded());
	}

	private PublicKey receiveKey() throws AbortException {
		byte[] publicKeyBytes = transport.receiveKey();
		callbacks.initialRecordReceived();
		KeyParser keyParser = crypto.getAgreementKeyParser();
		try {
			PublicKey publicKey = keyParser.parsePublicKey(publicKeyBytes);
			byte[] expected = keyAgreementCrypto.deriveKeyCommitment(publicKey);
			if (!Arrays.equals(expected, theirPayload.getCommitment())) throw new AbortException();
			return publicKey;
		} catch (GeneralSecurityException e) {
			throw new AbortException();
		}
	}

	private SecretKey deriveSharedSecret(PublicKey theirPublicKey) throws AbortException {
		try {
			byte[] ourPublicKeyBytes = ourKeyPair.getPublic().getEncoded();
			byte[] theirPublicKeyBytes = theirPublicKey.getEncoded();
			byte[][] inputs = {
					new byte[] {PROTOCOL_VERSION},
					alice ? ourPublicKeyBytes : theirPublicKeyBytes,
					alice ? theirPublicKeyBytes : ourPublicKeyBytes
			};
			return crypto.deriveSharedSecret(SHARED_SECRET_LABEL, theirPublicKey, ourKeyPair, inputs);
		} catch (GeneralSecurityException e) {
			throw new AbortException(e);
		}
	}

	private SecretKey deriveHybridSecret(SecretKey classical,
			PostQuantumCapabilities localPq, PostQuantumCapabilities peerPq)
			throws AbortException {
		try {
			byte[] aliceCapabilities = PostQuantumCapabilitiesCodec.encode(localPq);
			byte[] bobCapabilities = PostQuantumCapabilitiesCodec.encode(peerPq);
			byte[] kemPublicKey;
			byte[] kemCiphertext;
			byte[] kemSecret;
			if (alice) {
				AsymmetricCipherKeyPair pair = HybridKeyAgreement.generateKemKeyPair();
				kemPublicKey = ((MLKEMPublicKeyParameters) pair.getPublic()).getEncoded();
				transport.sendPqPublicKey(kemPublicKey);
				kemCiphertext = transport.receivePqCiphertext();
				kemSecret = HybridKeyAgreement.decapsulate(
						(MLKEMPrivateKeyParameters) pair.getPrivate(), kemCiphertext);
			} else {
				kemPublicKey = transport.receivePqPublicKey();
				MLKEMPublicKeyParameters publicKey = HybridKeyAgreement.parsePublicKey(kemPublicKey);
				HybridKeyAgreement.Encapsulation encapsulation = HybridKeyAgreement.encapsulate(publicKey);
				kemCiphertext = encapsulation.getCiphertext();
				kemSecret = encapsulation.getSecret();
				transport.sendPqCiphertext(kemCiphertext);
			}
			byte[] context = concat(aliceCapabilities, bobCapabilities, kemPublicKey, kemCiphertext);
			return new SecretKey(HybridKeyAgreement.combine(classical.getBytes(), kemSecret, context));
		} catch (Exception e) {
			throw new AbortException(e);
		}
	}

	private static byte[] concat(byte[]... values) {
		int length = 0;
		for (byte[] value : values) length += value.length;
		byte[] result = new byte[length];
		int offset = 0;
		for (byte[] value : values) {
			System.arraycopy(value, 0, result, offset, value.length);
			offset += value.length;
		}
		return result;
	}

	private void sendConfirm(SecretKey s, PublicKey theirPublicKey) throws IOException {
		byte[] confirm = keyAgreementCrypto.deriveConfirmationRecord(s,
				payloadEncoder.encode(theirPayload), payloadEncoder.encode(ourPayload),
				theirPublicKey, ourKeyPair, alice, alice);
		transport.sendConfirm(confirm);
	}

	private void receiveConfirm(SecretKey s, PublicKey theirPublicKey) throws AbortException {
		byte[] confirm = transport.receiveConfirm();
		byte[] expected = keyAgreementCrypto.deriveConfirmationRecord(s,
				payloadEncoder.encode(theirPayload), payloadEncoder.encode(ourPayload),
				theirPublicKey, ourKeyPair, alice, !alice);
		if (!Arrays.equals(expected, confirm)) throw new AbortException();
	}

	private void sendAbort(boolean exception) {
		transport.sendAbort(exception);
	}
}
