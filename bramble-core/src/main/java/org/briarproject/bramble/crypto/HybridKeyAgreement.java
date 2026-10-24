package org.briarproject.bramble.crypto;

import org.briarproject.bramble.api.crypto.SecretKey;
import org.briarproject.nullsafety.NotNullByDefault;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.NamedParameterSpec;
import java.util.Arrays;

import javax.crypto.KEM;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.SecretKeySpec;

/**
 * Hybrid classical + post-quantum key agreement primitive.
 *
 * The existing Briar agreement remains the authentication mechanism. This
 * class provides the PQ KEM half and a deterministic combiner so a future
 * wire-protocol extension can negotiate it without replacing legacy peers.
 */
@NotNullByDefault
public final class HybridKeyAgreement {

	private static final String KEM_ALGORITHM = "ML-KEM";
	private static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";
	private static final int KEY_BITS = 256;
	private static final byte[] LABEL = "Briar-Hybrid-KEM-v1".getBytes();

	private HybridKeyAgreement() {}

	public static boolean isSupported() {
		try {
			KEM.getInstance(KEM_ALGORITHM);
			return true;
		} catch (GeneralSecurityException e) {
			return false;
		}
	}

	public static KeyPair generateKemKeyPair() throws GeneralSecurityException {
		KeyPairGenerator generator = KeyPairGenerator.getInstance(KEM_ALGORITHM);
		generator.initialize(new NamedParameterSpec("ML-KEM-768"), new SecureRandom());
		return generator.generateKeyPair();
	}

	public static byte[] encapsulate(PublicKey publicKey) throws GeneralSecurityException {
		KEM kem = KEM.getInstance(KEM_ALGORITHM);
		KEM.Encapsulator encapsulator = kem.newEncapsulator(publicKey);
		KEM.Encapsulated encapsulated = encapsulator.encapsulate();
		return pack(encapsulated.key().getEncoded(), encapsulated.encapsulation());
	}

	public static byte[] decapsulate(PrivateKey privateKey, byte[] packed)
			throws GeneralSecurityException {
		byte[][] parts = unpack(packed);
		KEM kem = KEM.getInstance(KEM_ALGORITHM);
		KEM.Decapsulator decapsulator = kem.newDecapsulator(privateKey);
		return decapsulator.decapsulate(parts[1]).getEncoded();
	}

	/**
	 * Combines the existing classical secret with the ML-KEM secret.
	 */
	public static byte[] combine(byte[] classicalSecret, byte[] kemSecret)
			throws GeneralSecurityException {
		byte[] material = new byte[LABEL.length + classicalSecret.length + kemSecret.length];
		System.arraycopy(LABEL, 0, material, 0, LABEL.length);
		System.arraycopy(classicalSecret, 0, material, LABEL.length, classicalSecret.length);
		System.arraycopy(kemSecret, 0, material, LABEL.length + classicalSecret.length,
				kemSecret.length);
		SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF_ALGORITHM);
		SecretKeySpec key = new SecretKeySpec(new byte[32], "HmacSHA256");
		return factory.generateSecret(new javax.crypto.spec.PBEKeySpec(
				bytesToChars(material), new byte[16], 1, KEY_BITS)).getEncoded();
	}

	private static char[] bytesToChars(byte[] input) {
		char[] chars = new char[input.length];
		for (int i = 0; i < input.length; i++) chars[i] = (char) (input[i] & 0xff);
		return chars;
	}

	private static byte[] pack(byte[] secret, byte[] encapsulation) {
		byte[] out = new byte[4 + secret.length + encapsulation.length];
		out[0] = (byte) (secret.length >>> 24);
		out[1] = (byte) (secret.length >>> 16);
		out[2] = (byte) (secret.length >>> 8);
		out[3] = (byte) secret.length;
		System.arraycopy(secret, 0, out, 4, secret.length);
		System.arraycopy(encapsulation, 0, out, 4 + secret.length, encapsulation.length);
		return out;
	}

	private static byte[][] unpack(byte[] packed) throws GeneralSecurityException {
		if (packed.length < 4) throw new GeneralSecurityException("Invalid ML-KEM payload");
		int secretLength = ((packed[0] & 0xff) << 24) | ((packed[1] & 0xff) << 16)
				| ((packed[2] & 0xff) << 8) | (packed[3] & 0xff);
		if (secretLength < 0 || secretLength > packed.length - 4)
			throw new GeneralSecurityException("Invalid ML-KEM payload");
		return new byte[][] {
				Arrays.copyOfRange(packed, 4, 4 + secretLength),
				Arrays.copyOfRange(packed, 4 + secretLength, packed.length)
		};
	}
}
