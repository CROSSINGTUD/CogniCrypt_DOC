package de.upb.userstudy;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Password-based AES/GCM encryption example.
 *
 * <p>This is study material, so it is written to satisfy the CrySL rules this project
 * bundles for the APIs it uses — notably {@code PBEKeySpec.crysl}, which requires an
 * iteration count of at least 10000, forbids a hard-coded or {@code String}-typed
 * password, and requires {@code clearPassword()} after construction.
 */
public class StringEncryption {

	/** PBKDF2 salt length in bytes. */
	private static final int SALT_LENGTH = 16;
	/** GCM standard nonce length in bytes. */
	private static final int NONCE_LENGTH = 12;
	/** GCM authentication tag length in bits. */
	private static final int TAG_LENGTH_BITS = 128;
	/** PBKDF2 iterations; the bundled rule requires >= 10000. */
	private static final int ITERATION_COUNT = 600_000;
	/** Derived AES key length in bits. */
	private static final int KEY_LENGTH_BITS = 256;

	/**
	 * Demo entry point. The password is read from the environment rather than hard-coded,
	 * because the bundled rule forbids a hard-coded password.
	 */
	public static void main(String[] args)
			throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException,
			InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException {

		String fromEnv = System.getenv("DEMO_PASSWORD");
		if (fromEnv == null || fromEnv.isEmpty()) {
			System.err.println("Set DEMO_PASSWORD to run this example.");
			return;
		}

		char[] password = fromEnv.toCharArray();
		try {
			String ciphertext = encrypt(password, "Encrypt me!");
			System.out.println(ciphertext);
		} finally {
			// Do not leave the password sitting in memory.
			java.util.Arrays.fill(password, '\0');
		}
	}

	/**
	 * Derive a key from the password with PBKDF2 and encrypt the plaintext with AES/GCM.
	 *
	 * <p>The salt and nonce are freshly generated per call and prepended to the output, so
	 * the result is self-contained and decryptable. The password is taken as a
	 * {@code char[]} rather than a {@code String} so the caller can clear it.
	 *
	 * @return Base64 of {@code salt || nonce || ciphertext}
	 */
	public static String encrypt(char[] pass, String plaintext)
			throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException,
			InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException {

		SecureRandom random = SecureRandom.getInstanceStrong();

		// A fresh random salt per encryption; a fixed salt would let one attack table
		// cover every password encrypted by this program.
		final byte[] salt = new byte[SALT_LENGTH];
		random.nextBytes(salt);

		// Derive the key, then clear the key spec as the CrySL rule requires.
		PBEKeySpec keySpec = new PBEKeySpec(pass, salt, ITERATION_COUNT, KEY_LENGTH_BITS);
		SecretKey key;
		try {
			SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			key = new SecretKeySpec(secretKeyFactory.generateSecret(keySpec).getEncoded(), "AES");
		} finally {
			keySpec.clearPassword();
		}

		// GCM takes a 12-byte nonce and NoPadding - it is a stream mode.
		final byte[] nonce = new byte[NONCE_LENGTH];
		random.nextBytes(nonce);
		GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BITS, nonce);

		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		cipher.init(Cipher.ENCRYPT_MODE, key, spec);
		byte[] cipherTextBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

		// Prepend salt and nonce so the ciphertext can actually be decrypted later.
		ByteBuffer out = ByteBuffer.allocate(salt.length + nonce.length + cipherTextBytes.length);
		out.put(salt).put(nonce).put(cipherTextBytes);
		return Base64.getEncoder().encodeToString(out.array());
	}

}
