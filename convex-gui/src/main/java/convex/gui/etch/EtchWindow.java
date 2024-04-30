package convex.gui.etch;

import javax.swing.JTabbedPane;

import convex.api.Convex;
import convex.api.ConvexLocal;
import convex.gui.components.AbstractGUI;
import convex.gui.components.PeerComponent;
import etch.EtchStore;
import net.miginfocom.swing.MigLayout;

@SuppressWarnings("serial")
public class EtchWindow extends AbstractGUI {
	EtchStore store;
	Convex peer;
	
	public EtchStore getEtchStore() {
		return store;
	}
	
	JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);

	public EtchWindow(ConvexLocal peer) {
		super ("Etch Storage View - "+peer.getLocalServer().getStore());
		this.store=(EtchStore) peer.getLocalServer().getStore();
		setLayout(new MigLayout());
		
		PeerComponent pcom=new PeerComponent(peer);
		add(pcom, "dock north");
		
		add(tabbedPane, "dock center");
	}

}