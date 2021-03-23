package yadokaris_Youtube_Chat_Viewer;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.jline.utils.InputStreamReader;

import com.github.windpapi4j.InitializationFailedException;
import com.github.windpapi4j.WinAPICallFailedException;
import com.github.windpapi4j.WinDPAPI;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class YoutubeCookie {

	private Map<String, String> ids = new HashMap<>();
	private Browser browser;
	private byte[] masterKey;
	private String pathJson, pathCookie;

	public YoutubeCookie(Browser browser) {
		this.browser = browser;

		switch (browser) {
		case CHROME:
			pathJson = System.getProperty("user.home") + "/AppData/Local/Google/Chrome/User Data/Local State";
			pathCookie = System.getProperty("user.home") + "/AppData/Local/Google/Chrome/User Data/Default/Cookies";
			break;
		case EDGE:
			pathJson = System.getProperty("user.home") + "/AppData/Local/Microsoft/Edge/User Data/Local State";
			pathCookie = System.getProperty("user.home") + "/AppData/Local/Microsoft/Edge/User Data/Default/Cookies";
			break;
		case OPERA:
			pathJson = System.getProperty("user.home") + "/AppData/Roaming/Opera Software/Opera Stable/Local State";
			pathCookie = System.getProperty("user.home") + "/AppData/Roaming/Opera Software/Opera Stable/Cookies";
			break;
		case BRAVE:
			pathJson = System.getProperty("user.home") + "/AppData/Local/BraveSoftware/Brave-Browser/User Data/Local State";
			pathCookie = System.getProperty("user.home") + "/AppData/Local/BraveSoftware/Brave-Browser/User Data/Default/Cookies";
			break;
		case VIVALDI:
			pathJson = System.getProperty("user.home") + "/AppData/Local/Vivaldi/User Data/Local State";
			pathCookie = System.getProperty("user.home") + "/AppData/Local/Vivaldi/User Data/Default/Cookies";
			break;
		case FIREFOX:
			System.out.println(new File(System.getProperty("user.home") + "/AppData/Roaming/Mozilla/Firefox/Profiles").listFiles(f -> {return f.getName().endsWith("default-release");})[0].getPath());
			pathCookie = new File(System.getProperty("user.home") + "/AppData/Roaming/Mozilla/Firefox/Profiles").listFiles(f -> {return f.getName().endsWith("default-release");})[0].getPath() + "/cookies.sqlite";
			getFFCookies();
			return;
		}

		getCookies();
	}

	public String getSAPISID() {
		return ids.get("SAPISID");
	}

	public String getHSID() {
		return ids.get("HSID");
	}

	public String getSSID() {
		return ids.get("SSID");
	}

	public String getAPISID() {
		return ids.get("APISID");
	}

	public String getSID() {
		return ids.get("SID");
	}

	public Browser getBrowser() {
		return browser;
	}

	public Map<String, String> getIDs() {
		return ids;
	}

	private String getDecryptValue(byte[] encryptedValue) {
		byte[] nonce = Arrays.copyOfRange(encryptedValue, 3, 3 + 12);
		byte[] cipherText = Arrays.copyOfRange(encryptedValue, 3 + 12, encryptedValue.length);
		byte[] cookie = null;
		try {
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, nonce);
			SecretKeySpec keySpec = new SecretKeySpec(getMasterKey(), "AES");
			cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
			cookie = cipher.doFinal(cipherText);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}

		try {
			return new String(cookie, "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		return "";
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private byte[] getMasterKey() {
		if (masterKey == null) {
			Gson gson = new Gson();
			Map map = null;
			try {
				map = gson.fromJson(new InputStreamReader(Files.newInputStream(Paths.get(pathJson)), StandardCharsets.UTF_8), Map.class);
			} catch (JsonSyntaxException | JsonIOException | IOException e) {
				e.printStackTrace();
			}
			String encryptedKeyB64 = ((Map<String, String>)map.get("os_crypt")).get("encrypted_key");
			byte[] encryptedKey = Base64.getDecoder().decode(encryptedKeyB64);
			try {
				WinDPAPI winDPAPI = WinDPAPI.newInstance(WinDPAPI.CryptProtectFlag.CRYPTPROTECT_UI_FORBIDDEN);
				this.masterKey = winDPAPI.unprotectData(Arrays.copyOfRange(encryptedKey, 5, encryptedKey.length));
			} catch (InitializationFailedException | WinAPICallFailedException e) {
				e.printStackTrace();
			}
		}

		return masterKey;
	}

	private void getCookies() {
		Connection connection = null;
		Statement statement = null;

		try {
			Class.forName("org.sqlite.JDBC");
			connection = DriverManager.getConnection("jdbc:sqlite:" + pathCookie);
			statement = connection.createStatement();
			String sql = "SELECT * FROM cookies WHERE host_key LIKE '%youtube.com';";
			ResultSet rs = statement.executeQuery(sql);
			while (rs.next()) {
				String name = rs.getString("name");
				byte[] encryptedValue = rs.getBytes("encrypted_value");
				if (name.equals("SAPISID")) ids.put("SAPISID", getDecryptValue(encryptedValue));
				else if (name.equals("HSID")) ids.put("HSID", getDecryptValue(encryptedValue));
				else if (name.equals("SSID")) ids.put("SSID", getDecryptValue(encryptedValue));
				else if (name.equals("APISID")) ids.put("APISID", getDecryptValue(encryptedValue));
				else if (name.equals("SID")) ids.put("SID", getDecryptValue(encryptedValue));
			}
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				if (statement != null) statement.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			try {
				if (connection != null) connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private void getFFCookies() {
		Connection connection = null;
		Statement statement = null;

		try {
			connection = DriverManager.getConnection("jdbc:sqlite:" + pathCookie);
			statement = connection.createStatement();
			String sql = "SELECT * FROM moz_cookies WHERE host LIKE '%youtube.com';";
			ResultSet rs = statement.executeQuery(sql);
			while (rs.next()) {
				String name = rs.getString("name");
				String value = rs.getString("value");
				if (name.equals("SAPISID")) ids.put("SAPISID", value);
				else if (name.equals("HSID")) ids.put("HSID", value);
				else if (name.equals("SSID")) ids.put("SSID", value);
				else if (name.equals("APISID")) ids.put("APISID", value);
				else if (name.equals("SID")) ids.put("SID", value);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (statement != null) statement.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			try {
				if (connection != null) connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public boolean hasAllIds() {
		return ids.containsKey("SAPISID") && ids.containsKey("HSID") && ids.containsKey("SSID") && ids.containsKey("APISID") && ids.containsKey("SID");
	}
}
