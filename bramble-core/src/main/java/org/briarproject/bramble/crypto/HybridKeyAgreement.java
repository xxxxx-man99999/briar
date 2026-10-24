package org.briarproject.bramble.crypto;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.SecretWithEncapsulation;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMExtractor;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMGenerator;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyGenerationParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyPairGenerator;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPublicKeyParameters;
import org.briarproject.nullsafety.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;

/** Hybrid classical + post-quantum key agreement primitive using ML-KEM-768. */
@NotNullByDefault
public final class HybridKeyAgreement {

	private static final byte[] LABEL =
			"Briar-Hybrid-KEM-v1".getBytes(StandardCharsets.US_ASCII);

	private HybridKeyAgreement() {}

	public static boolean isSupported() {
		try {
			new MLKEMGenerator(new SecureRandom());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static AsymmetricCipherKeyPair generateKemKeyPair() {
		MLKEMKeyPairGenerator generator = new MLKEMKeyPairGenerator();
		generator.init(new MLKEMKeyGenerationParameters(new SecureRandom(),
				MLKEMParameters.ml_kem_768));
		return generator.generateKeyPair();
	}

	public static MLKEMPublicKeyParameters parsePublicKey(byte[] encoded) {
		return new MLKEMPublicKeyParameters(MLKEMParameters.ml_kem_768, encoded);
	}

	public static Encapsulation encapsulate(MLKEMPublicKeyParameters publicKey) {
		SecretWithEncapsulation result = new MLKEMGenerator(new SecureRandom())
				.generateEncapsulated(publicKey);
		try {
			return new Encapsulation(result.getEncapsulation(), result.getSecret());
		} finally {
			result.dispose();
		}
	}

	public static byte[] decapsulate(MLKEMPrivateKeyParameters privateKey,
			byte[] encapsulation) {
		return new MLKEMExtractor(privateKey).extractSecret(encapsulation);
	}

	/** Combines the existing classical secret and the ML-KEM secret with SHA-256. */
	public static byte[] combine(byte[] classicalSecret, byte[] kemSecret)
			throws GeneralSecurityException {
		return combine(classicalSecret, kemSecret, new byte[0]);
	}

	/**
	 * Combines both secrets and an authenticated handshake context. The context
	 * binds capability negotiation and the PQ exchange to the final secret,
	 * preventing a silent downgrade from being accepted as the same key.
	 */
	public static byte[] combine(byte[] classicalSecret, byte[] kemSecret,
			byte[] context) throws GeneralSecurityException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		digest.update(LABEL);
		updateLengthPrefixed(digest, classicalSecret);
		updateLengthPrefixed(digest, kemSecret);
		updateLengthPrefixed(digest, context);
		return digest.digest();
	}

	private static void updateLengthPrefixed(MessageDigest digest, byte[] value) {
		digest.update((byte) (value.length >>> 24));
		digest.update((byte) (value.length >>> 16));
		digest.update((byte) (value.length >>> 8));
		digest.update((byte) value.length);
		digest.update(value);
	}

	public static final class Encapsulation {
		private final byte[] ciphertext;
		private final byte[] secret;

		private Encapsulation(byte[] ciphertext, byte[] secret) {
			this.ciphertext = ciphertext.clone();
			this.secret = secret.clone();
		}

		public byte[] getCiphertext() { return ciphertext.clone(); }
		public byte[] getSecret() { return secret.clone(); }
	}
}
