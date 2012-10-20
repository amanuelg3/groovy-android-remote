package com.linuxfunkar.mousekeysremote;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import android.util.Base64;

class Security {
	private static final String algorithm = "PBEWithMD5AndDES";
	private static final byte[] salt = new byte[] { 67, (byte) 222, 18,
			(byte) 174, 59, (byte) 243, 69, 125 };

	private SecretKey key;
	private Cipher cipher;

	public Security(String password) throws NoSuchAlgorithmException,
			InvalidKeyException, InvalidKeySpecException,
			NoSuchPaddingException, InvalidAlgorithmParameterException {
		SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm);
		char[] pass = password.toCharArray();
		key = factory.generateSecret(new PBEKeySpec(pass));
		cipher = Cipher.getInstance(algorithm);
		cipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(salt, 2048));
	}

	public String encrypt(String msg) throws InvalidKeyException,
			BadPaddingException, IllegalBlockSizeException,
			UnsupportedEncodingException {
		byte[] inputBytes = msg.getBytes("UTF-8");
		byte[] encbytes = cipher.doFinal(inputBytes);
		String ret = Base64.encodeToString(encbytes, Base64.NO_WRAP);
		return ret;
	}
}
