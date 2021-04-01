package de.upb.userstudy;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class MessageDigestExample {

	public static void main(String[] args) throws NoSuchAlgorithmException {
		Scanner sc = new Scanner(System.in);
		System.out.println("Enter the message");
		String message = sc.nextLine();
		sc.close();
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(message.getBytes());
	}
}
