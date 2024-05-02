package convex.gui.components;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.util.function.Function;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.Scrollable;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import net.miginfocom.swing.MigLayout;

/**
 * Component that represents a convenient Scrollable list of child components,
 * based on a List model.
 * 
 * @param <E> Type of list model elements
 */
@SuppressWarnings("serial")
public class ScrollyList<E> extends JScrollPane {
	private final Function<E, Component> builder;
	private final ListModel<E> model;
	private final ScrollablePanel listPanel = new ScrollablePanel();

	private void refreshList() {
		EventQueue.invokeLater(()->{;
			listPanel.removeAll();
			int n = model.getSize();
			for (int i = 0; i < n; i++) {
				E we = model.getElementAt(i);
				listPanel.add(builder.apply(we),"span");
			}
			this.revalidate();
		});
	}

	private static class ScrollablePanel extends JPanel implements Scrollable {

		@Override
		public Dimension getPreferredScrollableViewportSize() {
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
			return 60;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
			// TODO Auto-generated method stub
			return 180;
		}

		@Override
		public boolean getScrollableTracksViewportWidth() {
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight() {
			return false;
		}
	}

	public ScrollyList(ListModel<E> model, Function<E, Component> builder) {
		super();
		this.builder = builder;
		this.model = model;
		this.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		listPanel.setLayout(new MigLayout("wrap 1"));
		setViewportView(listPanel);
		getViewport().setBackground(null);

		model.addListDataListener(new ListDataListener() {
			@Override
			public void intervalAdded(ListDataEvent e) {
				int start=e.getIndex0();
				int last=e.getIndex1();
				for (int i=start; i<=last; i++) {
					listPanel.add(builder.apply(model.getElementAt(i)),"span");
				}
			}

			@Override
			public void intervalRemoved(ListDataEvent e) {
				int start=e.getIndex0();
				int last=e.getIndex1();
				for (int i=start; i<=last; i++) {
					listPanel.remove(start);;
				}
			}

			@Override
			public void contentsChanged(ListDataEvent e) {
				refreshList();
			}
		});

		refreshList();
	}
}
