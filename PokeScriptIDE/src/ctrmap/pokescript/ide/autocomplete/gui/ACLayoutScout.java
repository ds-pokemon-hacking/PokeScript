package ctrmap.pokescript.ide.autocomplete.gui;

import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import javax.swing.JWindow;

public class ACLayoutScout {

	public static void setUpLayout(ACMainWindow m, ACDocWindow d, Point loc, int h) {
		//Main window prefers being to the bottom right of the caret
		AnchoredWindowMember mainWLoc = applyBestSeqAnchor(m, loc, h, new AnchoredWindowMember[0],
				AnchorSet.BOTTOM_RIGHT,
				AnchorSet.TOP_RIGHT,
				AnchorSet.BOTTOM_LEFT,
				AnchorSet.TOP_LEFT
		);
		AnchoredWindowMember docWLoc = applyBestSeqAnchor(d, loc, h, new AnchoredWindowMember[]{mainWLoc},
				AnchorSet.BOTTOM_RIGHT,
				AnchorSet.TOP_RIGHT,
				AnchorSet.TOP_LEFT,
				AnchorSet.BOTTOM_LEFT
		);
	}

	private static AnchoredWindowMember applyBestSeqAnchor(JWindow window, Point loc, int h, AnchoredWindowMember[] usedAnchors, AnchorSet... anchors) {
		Point test;
		MainLoop:
		for (AnchorSet a : anchors) {
			test = getPosAbsolute(window, loc, h, a.h, a.v);
			if (canExistAt(window, test)) {
				for (AnchoredWindowMember u : usedAnchors) {
					if (u.anchorSet == a) {
						//shift to left or right of existing window member, right has priority
						test = new Point(u.window.getX() + u.window.getWidth() + 10, u.window.getY());
						if (canExistAt(window, test)) {
							window.setLocation(test);
							return new AnchoredWindowMember(window, a);
						}
						test = new Point(u.window.getX() - window.getWidth() - 10, u.window.getY());
						if (canExistAt(window, test)) {
							window.setLocation(test);
							return new AnchoredWindowMember(window, a);
						}
					}
				}
				window.setLocation(test);
				return new AnchoredWindowMember(window, a);
			}
		}
		throw new UnsupportedOperationException("ASSERTION FAILED: Could not determine layout - is caret outside of the screen?"
				+ "\nDEBUG INFO:\n"
				+ "Screen size:\n" + getMultiMonitorScreenSize()
				+ "\nTried to position at:\n" + loc);	//failsafe
	}

	private static boolean canExistAt(JWindow w, Point loc) {
		Dimension screenSize = getMultiMonitorScreenSize();
		return loc.x >= 0 && loc.y >= 0 && loc.x + w.getWidth() < screenSize.width && loc.y + w.getHeight() < screenSize.height;
	}

	private static Point getPosAbsolute(JWindow w, Point loc, int h, HorizontalAnchor ha, VerticalAnchor va) {
		int x = ha == HorizontalAnchor.R ? loc.x : loc.x - w.getWidth();
		int y = va  == VerticalAnchor.B ? loc.y + h : loc.y - w.getHeight();
		return new Point(x, y);
	}

	private static Dimension getMultiMonitorScreenSize() {
		GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] devices = env.getScreenDevices();
		int totalWidth = 0;
		int totalHeight = 0;
		for (GraphicsDevice gd : devices) {
			DisplayMode dm = gd.getDisplayMode();
			totalWidth += dm.getWidth();
			totalHeight += dm.getHeight();
		}
		return new Dimension(totalWidth, totalHeight);
	}

	private static enum HorizontalAnchor {
		L,
		R,
	}

	private static enum VerticalAnchor {
		T,
		B,
	}

	private static enum RelativePosition {
		T,
		B,
		L,
		R
	}

	private static class AnchoredWindowMember {

		public JWindow window;
		public AnchorSet anchorSet;

		public AnchoredWindowMember(JWindow w, AnchorSet a) {
			window = w;
			anchorSet = a;
		}
	}

	private static class AnchorSet {

		public static AnchorSet BOTTOM_RIGHT = new AnchorSet(HorizontalAnchor.R, VerticalAnchor.B);
		public static AnchorSet BOTTOM_LEFT = new AnchorSet(HorizontalAnchor.L, VerticalAnchor.B);
		public static AnchorSet TOP_RIGHT = new AnchorSet(HorizontalAnchor.R, VerticalAnchor.T);
		public static AnchorSet TOP_LEFT = new AnchorSet(HorizontalAnchor.L, VerticalAnchor.T);

		public HorizontalAnchor h;
		public VerticalAnchor v;

		private AnchorSet(HorizontalAnchor h, VerticalAnchor v) {
			this.h = h;
			this.v = v;
		}

		@Override
		public String toString() {
			return v.toString() + "," + h.toString();
		}
	}
}
