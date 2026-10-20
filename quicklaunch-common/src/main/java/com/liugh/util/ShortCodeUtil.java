package com.liugh.util;

import java.security.MessageDigest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShortCodeUtil {
	private static final Logger LOG = LoggerFactory.getLogger(ShortCodeUtil.class);
	private static final String key = "ShortUrl";
	private static final String[] chars = new String[] { "a", "b", "c", "d",
			"e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q",
			"r", "s", "t", "u", "v", "w", "x", "y", "z", "0", "1", "2", "3",
			"4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "G",
			"H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
			"U", "V", "W", "X", "Y", "Z" };

	private final static String[] hexDigits = { "0", "1", "2", "3", "4", "5",
			"6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

	private static String byteArrayToHexString(byte[] b) {
		StringBuffer resultSb = new StringBuffer();
		for (int i = 0; i < b.length; i++) {
			resultSb.append(byteToHexString(b[i]));
		}
		return resultSb.toString();
	}

	private static String byteToHexString(byte b) {
		int n = b;
		if (n < 0)
			n = 256 + n;
		int d1 = n / 16;
		int d2 = n % 16;
		return hexDigits[d1] + hexDigits[d2];
	}

	private static String MD5Encode(String origin) {
		String resultString = null;
		try {

			resultString = new String(origin);
			MessageDigest md = MessageDigest.getInstance("MD5");

			resultString.trim();

			resultString = byteArrayToHexString(md.digest(resultString
					.getBytes("UTF-8")));
		} catch (Exception ex) {
		}
		return resultString;
	}

	public static void main(String[] args) {
		// String url = "722111057550710111223333445555667201409191706";
		//
		// print(shortText(url, 5));

		String s = "";
		print("start...");
		for (int i = 0; i < 10000000; i++) {
			s = shortText(System.currentTimeMillis() + i + "", 6);
			if (s.length() != 6) {
				System.out.println(s);
			}
		}
		print("end...");
	}

	public static String shortText(String string, int length) {
		int outLength = length;

		String hex = MD5Encode(key + string);

		String outChars = "";

		String subHex = hex.substring(0, 8);
		long idx = Long.parseLong("3FFFFFFF", 16) & Long.parseLong(subHex, 16);

		for (int k = 0; k < outLength; k++) {
			int index = (int) (Long.valueOf("0000003D", 16) & idx);
			outChars += chars[index];
			idx = idx >> 5;
		}

		return outChars;
	}

	private static void print(Object messagr) {
		System.out.println(messagr);
	}
}
