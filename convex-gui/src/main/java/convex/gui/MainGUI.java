package convex.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.net.ConnectException;
import java.net.InetSocketAddress;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import convex.api.Convex;
import convex.core.crypto.AKeyPair;
import convex.core.data.Address;
import convex.core.data.Blob;
import convex.core.util.Utils;
import convex.gui.client.ConvexClient;
import convex.gui.components.AbstractGUI;
import convex.gui.components.ActionPanel;
import convex.gui.components.Toast;
import convex.gui.manager.mainpanels.HomePanel;
import convex.gui.tools.HackerTools;
import convex.gui.utils.Toolkit;
import net.miginfocom.swing.MigLayout;

@SuppressWarnings("serial")
public class MainGUI extends AbstractGUI {
	public MainGUI() {
		MigLayout layout=new MigLayout("center");
		setLayout(layout);
		
		add(new HomePanel(),"dock center");
		
		ActionPanel actionPanel=new ActionPanel();
		actionPanel.setLayout(new MigLayout("center,align center,fillx"));
		
		JComponent wallet=createLaunchButton("Wallet",Toolkit.WALLET_ICON,this::launchTestNet);
		actionPanel.add(wallet);

		JComponent testNet=createLaunchButton("Launch TestNet",Toolkit.TESTNET_ICON,this::launchTestNet);
		actionPanel.add(testNet);
		
		JComponent terminal=createLaunchButton("Convex Terminal",Toolkit.TERMINAL_ICON,this::launchTerminalClient);
		actionPanel.add(terminal);
		
		JComponent hacker=createLaunchButton("Hacker Tools",Toolkit.HACKER_ICON,this::launchTools);
		actionPanel.add(hacker);

		JComponent discord=createLaunchButton("Community Discord",Toolkit.ECOSYSTEM_ICON,this::launchDiscord);
		actionPanel.add(discord);

		JComponent www=createLaunchButton("https://convex.world",Toolkit.WWW_ICON,this::launchWebsite);
		actionPanel.add(www);
		
		add(actionPanel,"dock south");
	}
	
	public void launchTestNet() {
		JPanel pan=new JPanel();
		pan.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		pan.setLayout(new MigLayout("fill,wrap 2","","[fill]10[fill]"));
		
		pan.add(new JLabel("Number of Peers:"));
		JSpinner peerCountSpinner = new JSpinner();
		// Note: about 300 max number of clients before hitting juice limits for account creation
		peerCountSpinner.setModel(new SpinnerNumberModel(PeerGUI.DEFAULT_NUM_PEERS, 1, 100, 1));
		pan.add(peerCountSpinner);

		pan.add(new JLabel("Genesis Key:   "));
		AKeyPair kp=AKeyPair.generate();
		JTextField keyField=new JTextField("0x"+kp.getSeed().toHexString());
		keyField.setMinimumSize(new Dimension(200,25));
		pan.add(keyField);


		int result = JOptionPane.showConfirmDialog(this, pan, 
	               "Enter Testnet Details", JOptionPane.OK_CANCEL_OPTION);
	    if (result == JOptionPane.OK_OPTION) {
	    	try {
	    		int numPeers=(Integer)peerCountSpinner.getValue();
	    		
	       		Blob b=Blob.parse(keyField.getText());
	    		if ((b!=null)&&(!b.isEmpty())) {
	    			kp=AKeyPair.create(b);
	    		} else {
	    			kp=null;
	    		}
	    		if (kp==null) throw new Exception("Invalid Genesis Key!");
	    		PeerGUI.launchPeerGUI(numPeers, kp,false);
	    	} catch (Exception e) {
	    		Toast.display(this, "Launch Failed: "+e.getMessage(), Color.RED);
	    		e.printStackTrace();
	    	}
	    }
		
		
	}
	
	public void launchDiscord() {
		Toolkit.launchBrowser("https://discord.com/invite/xfYGq4CT7v");
	}
	
	public void launchTerminalClient() {
		JPanel pan=new JPanel();
		pan.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		pan.setLayout(new MigLayout("fill,wrap 2","","[fill]10[fill]"));
		pan.add(new JLabel("Host:"));
		JTextField hostField=new JTextField("localhost:18888");
		pan.add(hostField);
		
		pan.add(new JLabel("Address:"));
		JTextField addressField=new JTextField("#12");
		pan.add(addressField);

		pan.add(new JLabel("Private Key:   "));
		JTextField keyField=new JTextField("");
		keyField.setMinimumSize(new Dimension(200,25));
		pan.add(keyField);


		int result = JOptionPane.showConfirmDialog(this, pan, 
	               "Enter Connection Details", JOptionPane.OK_CANCEL_OPTION);
	    if (result == JOptionPane.OK_OPTION) {
	    	try {
	    		InetSocketAddress sa=Utils.toInetSocketAddress(hostField.getText());
	    		System.err.println("MainGUI attemptiong connect to: "+sa);
	    		Convex convex=Convex.connect(sa);
	    		convex.setAddress(Address.parse(addressField.getText()));
	    		
	    		Blob b=Blob.parse(keyField.getText());
	    		if ((b!=null)&&(!b.isEmpty())) {
	    			AKeyPair kp=AKeyPair.create(b);
	    			convex.setKeyPair(kp);
	    		}
	    		ConvexClient.launch(convex);
	    	} catch (ConnectException e) {
	    		Toast.display(this, "Connection Refused! "+e.getMessage(), Color.RED);
	    		e.printStackTrace();
	    	} catch (Exception e) {
	    		Toast.display(this, "Connect Failed: "+e.getMessage(), Color.RED);
	    		e.printStackTrace();
	    	}
	    }
		
	}
	
	public void launchTools() {
		HackerTools.launch();
	}
	
	public void launchWebsite() {
		Toolkit.launchBrowser("https://convex.world");
	}
	
	public JPanel createLaunchButton(String label, ImageIcon icon, Runnable cmd) {
		JButton butt=new JButton(icon);
		butt.addActionListener(e->{
			EventQueue.invokeLater(cmd);
		});
		
		JLabel lab = new JLabel(label);
		lab.setHorizontalAlignment(SwingConstants.CENTER);
		
		JPanel panel=new JPanel();
		panel.setLayout(new MigLayout());
		panel.add(butt,"dock center");
		panel.add(lab,"dock south");
		return panel;
	}

	/**
	 * Launch the application.
	 * @param args Command line args
	 */
	public static void main(String[] args) {
		// call to set up Look and Feel
		Toolkit.init();
		new MainGUI().run();
	}

}
