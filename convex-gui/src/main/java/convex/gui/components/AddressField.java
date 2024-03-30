package convex.gui.components;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import convex.core.text.Text;
import convex.gui.utils.Toolkit;

@SuppressWarnings("serial")
public class AddressField extends JTextField {

	public AddressField() {
		super();
		//setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
		setFont(Toolkit.MONO_FONT);
		setBorder(null);
	}
	
	@Override 
	protected Document createDefaultModel() {
		return new AddressDocument();
	}
	
	public class AddressDocument extends PlainDocument {
		@Override
		public void insertString(int offset, String s, AttributeSet a) throws BadLocationException {
			if (s == null) return;

			char[] cs = s.toCharArray();
			int n=cs.length;


			for (int i = 0; i < n; i++ ) {
				char c=cs[i];
				if (Text.isASCIIDigit(c)) continue;
				if ((offset==0)&&(i==0)&&(c=='#')) continue;
				return; // not valid so exit function early
			}

			// Everything valid, so just insert as normal
	    	super.insertString(offset, s, a);
		}
	}
}
