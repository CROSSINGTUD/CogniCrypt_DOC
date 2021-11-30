package de.upb.userstudy;

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

public class StringEncryption {

	public static final byte[] salt = { (byte) 43, (byte) 76, (byte) 95, (byte) 7, (byte) 17 };

	public static void main(String[] args)
			throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException {

		String password = "password";
		String plaintext = "Encrypt me!";
		String ciphertext = encrypt(password, plaintext );
		System.out.println(ciphertext);
	}

	public static String encrypt(String pass, String plaintext)
			throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException {

		PBEKeySpec keySpec = new PBEKeySpec(pass.toCharArray(), salt, 6500, 256);
		SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		SecretKey key = secretKeyFactory.generateSecret(keySpec);

		final byte[] nonce = new byte[32];
		SecureRandom random = SecureRandom.getInstanceStrong();
		random.nextBytes(nonce);
		GCMParameterSpec spec = new GCMParameterSpec(128, nonce);

		Cipher cipher = Cipher.getInstance("AES/GCM/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, key, spec);
		byte[] plainTextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
		byte[] cipherTextBytes = cipher.doFinal(plainTextBytes);
		return Base64.getEncoder().encodeToString(cipherTextBytes);
	}

}
