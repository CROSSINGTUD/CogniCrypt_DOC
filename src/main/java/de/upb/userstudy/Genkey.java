package de.upb.userstudy;

import java.security.NoSuchAlgorithmException;
import javax.crypto.KeyGenerator;

public class Genkey {

	public byte[] generatingKey() throws NoSuchAlgorithmException {

		KeyGenerator keygen = KeyGenerator.getInstance("AES");
		keygen.init(128);
		byte[] key = keygen.generateKey().getEncoded();
		return key;
	}
}
