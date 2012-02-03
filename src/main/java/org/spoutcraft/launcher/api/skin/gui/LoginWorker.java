package org.spoutcraft.launcher.api.skin.gui;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.swing.JOptionPane;

import org.jdesktop.swingworker.SwingWorker;
import org.spoutcraft.launcher.api.Event;
import org.spoutcraft.launcher.api.Launcher;
import org.spoutcraft.launcher.api.skin.gui.LoginFrame.UserPasswordInformation;
import org.spoutcraft.launcher.api.util.Utils;
import org.spoutcraft.launcher.exceptions.BadLoginException;
import org.spoutcraft.launcher.exceptions.MCNetworkException;
import org.spoutcraft.launcher.exceptions.MinecraftUserNotPremiumException;
import org.spoutcraft.launcher.exceptions.OutdatedMCLauncherException;

public class LoginWorker extends SwingWorker<Object, Object> {

	private final LoginFrame loginFrame;
	private String user;
	private String pass;
	@SuppressWarnings("unused")
	private String[] values = null;

	public LoginWorker(LoginFrame loginFrame) {
		this.loginFrame = loginFrame;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPass() {
		return pass;
	}

	public void setPass(String pass) {
		this.pass = pass;
	}

	@Override
	protected Object doInBackground() throws Exception {
		loginFrame.getProgressBar().setVisible(true);
		loginFrame.getProgressBar().setString("Connecting to www.minecraft.net...");
		try {
			values = Utils.doLogin(user, pass, loginFrame.getProgressBar());
			Launcher.getGameUpdater().setMinecraftUser(values[2].trim());
			Launcher.getGameUpdater().setMinecraftSession(values[3].trim());
			Launcher.getGameUpdater().setDownloadTicket(values[1].trim());
			Launcher.getGameUpdater().setMinecraftPass(pass);
			
			loginFrame.onRawEvent(Event.SUCESSFUL_LOGIN);
			return true;
		} catch (BadLoginException e) {
			loginFrame.onRawEvent(Event.BAD_LOGIN);
		} catch (MinecraftUserNotPremiumException e) {
			loginFrame.onRawEvent(Event.USER_NOT_PREMIUM);
			loginFrame.getProgressBar().setVisible(false);
		} catch (MCNetworkException e) {
			UserPasswordInformation info = null;

			for (String username : loginFrame.usernames.keySet()) {
				if (username.equalsIgnoreCase(user)) {
					info = loginFrame.usernames.get(username);
					break;
				}
			}

			boolean authFailed = (info == null);

			if (!authFailed) {
				if (info.isHash) {
					try {
						MessageDigest digest = MessageDigest.getInstance("SHA-256");
						byte[] hash = digest.digest(pass.getBytes());
						for (int i = 0; i < hash.length; i++) {
							if (hash[i] != info.passwordHash[i]) {
								authFailed = true;
								break;
							}
						}
					} catch (NoSuchAlgorithmException ex) {
						authFailed = true;
					}
				} else {
					authFailed = !(pass.equals(info.password));
				}
			}

			if (authFailed) {
				loginFrame.offline = false;
				loginFrame.onRawEvent(Event.MINECRAFT_NETWORK_DOWN);
			} else {
				loginFrame.offline = true;
				loginFrame.onRawEvent(Event.MINECRAFT_NETWORK_DOWN);
			}
			this.cancel(true);
			loginFrame.getProgressBar().setVisible(false);
		} catch (OutdatedMCLauncherException e) {
			JOptionPane.showMessageDialog(loginFrame.getParent(), "Incompatible Login Version. Contact Spout about updating the Launcher!");
			loginFrame.getProgressBar().setVisible(false);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			this.cancel(true);
			loginFrame.getProgressBar().setVisible(false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
