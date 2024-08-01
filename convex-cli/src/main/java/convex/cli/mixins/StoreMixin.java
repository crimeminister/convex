package convex.cli.mixins;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.UnrecoverableKeyException;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import convex.cli.CLIError;
import convex.cli.Constants;
import convex.cli.Main;
import convex.core.crypto.AKeyPair;
import convex.core.crypto.PFXTools;
import convex.core.util.Utils;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

public class StoreMixin extends AMixin {

	@Option(names = { "--keystore" }, 
			defaultValue = "${env:CONVEX_KEYSTORE:-" + Constants.KEYSTORE_FILENAME+ "}", 
			scope = ScopeType.INHERIT, 
			description = "Keystore filename. Default: ${DEFAULT-VALUE}")
	private String keyStoreFilename;

	
	static Logger log = LoggerFactory.getLogger(StoreMixin.class);

	/**
	 * Password for keystore. Option named to match Java keytool
	 */
	@Option(names = {"--storepass" }, 
			scope = ScopeType.INHERIT, 
			defaultValue = "${env:CONVEX_KEYSTORE_PASSWORD}", 
			description = "Password to read/write to the Keystore") 
	String keystorePassword;

	KeyStore keyStore = null;
	
	/**
	 * Gets the keystore file name currently used for the CLI
	 * 
	 * @return File name, or null if not specified
	 */
	public File getKeyStoreFile() {
		if (keyStoreFilename != null) {
			File f = Utils.getPath(keyStoreFilename);
			return f;
		}
		return null;
	}

	/**
	 * Get the currently configured password for the keystore. Will emit warning and
	 * default to blank password if not provided
	 * 
	 * @param main TODO
	 * @return Password string
	 */
	public char[] getStorePassword() {
		char[] storepass = null;
	
		if (keystorePassword != null) {
			storepass = keystorePassword.toCharArray();
		} else {
			if (isInteractive()) {
				storepass = readPassword("Enter Keystore Password: ");
				keystorePassword=new String(storepass);
			}
	
			if (storepass == null) {
				paranoia("Keystore password must be explicitly provided");
				log.warn("No password for keystore: defaulting to blank password");
				storepass = new char[0];
			}
		}
		return storepass;
	}

	/**
	 * Gets the current key store
	 * 
	 * @param main TODO
	 * @return KeyStore instance, or null if it does not exist
	 */
	public KeyStore getKeystore() {
		if (keyStore == null) {
			keyStore = loadKeyStore(false, getStorePassword());
		}
		return keyStore;
	}

	/**
	 * Loads the currently configured key Store
	 * 
	 * @param main TODO
	 * @return KeyStore instance, or null if does not exist
	 */
	public KeyStore loadKeyStore() {
		return loadKeyStore(false, getStorePassword());
	}

	/**
	 * Loads the currently configured key Store
	 * 
	 * @param isCreate Flag to indicate if keystore should be created if absent
	 * @param password TODO
	 * @return KeyStore instance, or null if does not exist
	 */
	public KeyStore loadKeyStore(boolean isCreate, char[] password) {
		File keyFile = getKeyStoreFile();
		try {
			if (keyFile.exists()) {
				keyStore = PFXTools.loadStore(keyFile, password);
			} else if (isCreate) {
				log.debug("No keystore exists, creating at: " + keyFile.getCanonicalPath());
				Utils.ensurePath(keyFile);
				keyStore = PFXTools.createStore(keyFile, password);
			}
		} catch (FileNotFoundException e) {
			return null;
		} catch (GeneralSecurityException e) {
			throw new CLIError("Unexpected security error: " + e.getClass(), e);
		} catch (IOException e) {
			if (e.getCause() instanceof UnrecoverableKeyException) {
				throw new CLIError("Invalid password for keystore: " + keyFile);
			}
			throw new CLIError("Unable to read keystore at: " + keyFile, e);
		}
		return keyStore;
	}
	
	public void saveKeyStore() {
		if (keystorePassword==null) throw new CLIError("Key store password not provided, unable to save");
		saveKeyStore(keystorePassword.toCharArray());
	}

	public void saveKeyStore(char[] storePassword) {
		// save the keystore file
		if (keyStore == null)
			throw new CLIError("Trying to save a keystore that has not been loaded!");
		try {
			PFXTools.saveStore(keyStore, getKeyStoreFile(), storePassword);
		} catch (Throwable t) {
			throw Utils.sneakyThrow(t);
		}
	}

	/**
	 * Adds key pair to store. Does not save keystore!
	 * 
	 * @param main TODO
	 * @param keyPair Keypair to add
	 * @param keyPassword TODO
	 */
	public void addKeyPairToStore(AKeyPair keyPair, char[] keyPassword) {
	
		KeyStore keyStore = getKeystore();
		if (keyStore == null) {
			throw new CLIError("Trying to add key pair but keystore does not exist");
		}
		try {
			// save the key in the keystore
			PFXTools.setKeyPair(keyStore, keyPair, keyPassword);
		} catch (Throwable t) {
			throw new CLIError("Cannot store the key to the key store " + t);
		}
	
	}

	/**
	 * Loads a keypair from configured keystore
	 * 
	 * @param main TODO
	 * @param publicKey String identifying the public key. May be a prefix
	 * @return Keypair instance, or null if not found
	 */
	public AKeyPair loadKeyFromStore(Main main, String publicKey) {
		if (publicKey == null)
			return null;
	
		AKeyPair keyPair = null;
	
		publicKey = publicKey.trim();
		publicKey = publicKey.toLowerCase().replaceFirst("^0x", "").strip();
		if (publicKey.isEmpty()) {
			return null;
		}
	
		char[] storePassword = getStorePassword();
	
		File keyFile = getKeyStoreFile();
		try {
			if (!keyFile.exists()) {
				throw new CLIError("Cannot find keystore file " + keyFile.getCanonicalPath());
			}
			KeyStore keyStore = PFXTools.loadStore(keyFile, storePassword);
	
			Enumeration<String> aliases = keyStore.aliases();
	
			while (aliases.hasMoreElements()) {
				String alias = aliases.nextElement();
				if (alias.indexOf(publicKey) == 0) {
					log.trace("found keypair " + alias);
					keyPair = PFXTools.getKeyPair(keyStore, alias, main.getKeyPassword());
					break;
				}
			}
		} catch (Exception t) {
			throw new CLIError("Cannot load key store", t);
		}
	
		return keyPair;
	}

}
