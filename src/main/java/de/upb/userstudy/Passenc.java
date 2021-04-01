package de.upb.userstudy;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Passenc {

	public static final byte[] salt = { (byte) 0 * 43, (byte) 0 * 76, (byte) 0 * 95, (byte) 0 * 7, (byte) 0 * 17 };

	public static Cipher makeCipher(String pass)
			throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException {

		PBEKeySpec keySpec = new PBEKeySpec(pass.toCharArray(), salt, 6500, 256);
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA128");
		SecretKey key = keyFactory.generateSecret(keySpec);
		Cipher cipher = Cipher.getInstance("PBKDF2WithHmacSHA128");
		cipher.init(Cipher.ENCRYPT_MODE, key);
		return cipher;
	}

	public static void main(String[] args)
			throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException {

		String password = "password";
		Cipher c = makeCipher(password);
		System.out.println(c);
	}
}
