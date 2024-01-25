package org.apache.nutch.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.LayoutManager;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.EventObject;
import java.util.List;
import java.util.Objects;
import java.util.TooManyListenersException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.FailableFunction;
import org.apache.commons.lang3.stream.Streams.FailableStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;

import io.github.toolfactory.narcissus.Narcissus;
import net.miginfocom.swing.MigLayout;

public class DeflateUtilsGui extends JFrame implements ActionListener, DropTargetListener {

	private static final long serialVersionUID = 6847377909951406736L;

	private static Logger LOG = LoggerFactory.getLogger(DeflateUtilsGui.class);

	private static final String COMPONENT = "component";

	private JTextComponent jtcFile, jtcFileLength, jtcDigest, jtcDeflated, jtcDeflatedLength = null;

	private AbstractButton absFile, absFileRefresh, absCopyDeflated = null;

	private JPanel jpFile = null;

	private DropTarget dtFile = null;

	private ComboBoxModel<?> cbmMethod = null;

	private File file = null;

	private TableModel tmContentInfo = null;

	private DeflateUtilsGui() {
	}

	private void init() throws TooManyListenersException {
		//
		final JTabbedPane jTabbedPane = new JTabbedPane();
		//
		final LayoutManager lm1 = isRootPaneCheckingEnabled() ? getLayout(getContentPane()) : getLayout();
		//
		jTabbedPane.addTab("Defalte", createDeflatePanel(cloneLayoutManager(lm1)));
		//
		if (lm1 instanceof MigLayout) {
			//
			add(jTabbedPane);
			//
		} // if
			//
	}

	private static LayoutManager getLayout(final Container instance) {
		return instance != null ? instance.getLayout() : null;
	}

	private JPanel createDeflatePanel(final LayoutManager layoutManager) throws TooManyListenersException {
		//
		final JPanel panel = new JPanel();
		//
		panel.setLayout(layoutManager);
		//
		final String top = "top";
		//
		add(panel, new JLabel("File"), String.format("%1$s,span 1 2", top));
		//
		final String wrap = "wrap";
		//
		final Border border = BorderFactory.createMatteBorder(1, 1, 1, 1, Color.BLACK);
		//
		(jpFile = new JPanel()).setBorder(border);
		//
		jpFile.setDropTarget(dtFile = new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, null));
		//
		dtFile.addDropTargetListener(this);
		//
		final String growx = "growx";
		//
		add(panel, jpFile, String.format("%1$s,span %2$s %3$s,height %4$s", growx, 4, 2, 100));
		//
		add(panel, absFile = new JButton("Browse"), String.format("%1$s,%2$s", top, wrap));
		//
		add(panel, absFileRefresh = new JButton("Refresh"), String.format("%1$s,%2$s", top, wrap));
		//
		add(panel, new JLabel("File"));
		//
		add(panel, jtcFile = new JTextField(), String.format("width %1$s,%2$s,span %3$s", 200, growx, 3));
		//
		add(panel, jtcFileLength = new JTextField(), String.format("%1$s,%2$s,wmin %3$s", growx, wrap, 60));
		//
		add(panel, new JLabel("Content Type"), "span 2");
		//
		add(panel,
				new JScrollPane(new JTable(
						tmContentInfo = new DefaultTableModel(Arrays.stream(getDeclaredFields(ContentInfo.class))
								.filter(f -> f != null && !Modifier.isStatic(f.getModifiers()))
								.map(DeflateUtilsGui::getName).toList().toArray(new String[] {}), 0))),
				String.format("%1$s,%2$s,span %3$s,hmax %4$s", growx, wrap, 3, 39));
		//
		add(panel, new JLabel("Digest"));
		//
		final JComboBox<?> jcb = new JComboBox<>(cbmMethod = new DefaultComboBoxModel<>(ArrayUtils.insert(0,
				Arrays.stream(DigestUtils.class.getDeclaredMethods())
						.filter(m -> m != null && Modifier.isStatic(m.getModifiers())
								&& Arrays.equals(m.getParameterTypes(), new Class<?>[] { byte[].class })
								&& Objects.equals(String.class, m.getReturnType()))
						.sorted((a, b) -> StringUtils.compare(getName(a), getName(b))).toArray(),
				(Object) null)));
		//
		final ListCellRenderer<?> render = jcb.getRenderer();
		//
		jcb.setRenderer(new ListCellRenderer<Object>() {

			@Override
			public Component getListCellRendererComponent(final JList<? extends Object> list, final Object value,
					final int index, final boolean isSelected, final boolean cellHasFocus) {
				//
				return DeflateUtilsGui.getListCellRendererComponent(((ListCellRenderer) render), list,
						testAndApply(Objects::nonNull, value, x -> getName(cast(Method.class, x)), null), index,
						isSelected, cellHasFocus);
				//
			}

		});
		//
		add(panel, jcb, String.format("span %1$s", 2));
		//
		add(panel, jtcDigest = new JTextField(), String.format("%1$s,%2$s,span %3$s", growx, wrap, 2));
		//
		add(panel, new JLabel("Deflated"));
		//
		add(panel, jtcDeflated = new JTextField(), String.format("%1$s,span %2$s", growx, 3));
		//
		add(panel, jtcDeflatedLength = new JTextField(), String.format("%1$s", growx));
		//
		add(panel, absCopyDeflated = new JButton("Copy"), String.format("%1$s,%2$s", growx, wrap));
		//
		addActionListener(this, absFile, absFileRefresh, absCopyDeflated);
		//
		return panel;
		//
	}

	private static Field[] getDeclaredFields(final Class<?> instnace) {
		return instnace != null ? instnace.getDeclaredFields() : null;
	}

	private void addActionListener(ActionListener l, final AbstractButton ab, final AbstractButton... as) {
		//
		if (ab != null) {
			//
			ab.addActionListener(l);
			//
		} // if
			//
		AbstractButton a = null;
		//
		for (int i = 0; as != null && i < as.length; i++) {
			//
			if ((a = as[i]) != null) {
				//
				a.addActionListener(l);
				//
			} // if
				//
				//
		} // for
			//
	}

	private static String getName(final Member instance) {
		return instance != null ? instance.getName() : null;
	}

	private static <T, R, E extends Throwable> R testAndApply(final Predicate<T> predicate, final T value,
			final FailableFunction<T, R, E> functionTrue, final FailableFunction<T, R, E> functionFalse) throws E {
		return test(predicate, value) ? apply(functionTrue, value) : apply(functionFalse, value);
	}

	static <T, R, E extends Throwable> R apply(final FailableFunction<T, R, E> instance, final T value) throws E {
		return instance != null ? instance.apply(value) : null;
	}

	private static final <T> boolean test(final Predicate<T> instance, final T value) {
		return instance != null && instance.test(value);
	}

	private static <E> Component getListCellRendererComponent(final ListCellRenderer<E> instance,
			final JList<? extends E> list, final E value, final int index, final boolean isSelected,
			final boolean cellHasFocus) {
		//
		return instance != null ? instance.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
				: null;
		//
	}

	private static void add(final Container instance, final Component comp) {
		//
		if (instance == null) {
			//
			return;
			//
		} // if
			//
		try {
			//
			if (Narcissus.getObjectField(instance, getDeclaredField(Container.class, COMPONENT)) == null) {
				//
				return;
				//
			} // if
				//
		} catch (final NoSuchFieldException e) {
			//
			error(LOG, e.getMessage(), e);
			//
		} // try
			//
		if (comp != null) {
			//
			instance.add(comp);
			//
		} // if
			//
	}

	private static Field getDeclaredField(final Class<?> instance, final String name) throws NoSuchFieldException {
		return instance != null ? instance.getDeclaredField(name) : null;
	}

	private static void add(final Container instance, final Component comp, final Object constraints) {
		//
		if (instance == null) {
			//
			return;
			//
		} // if
			//
		try {
			//
			if (Narcissus.getObjectField(instance, getDeclaredField(Container.class, COMPONENT)) == null) {
				//
				return;
				//
			} // if
				//
		} catch (final NoSuchFieldException e) {
			//
			error(LOG, e.getMessage(), e);
			//
		} // try
			//
		if (comp != null) {
			//
			instance.add(comp, constraints);
			//
		} // if
			//
	}

	private static void error(final Logger instance, final String msg, final Throwable t) {
		//
		if (instance != null) {
			//
			instance.error(msg, t);
			//
		} // if
			//
	}

	private static LayoutManager cloneLayoutManager(final LayoutManager instance) {
		//
		final LayoutManager layoutManagerDefault = null;
		//
		LayoutManager lm = ObjectUtils.defaultIfNull(instance, layoutManagerDefault);
		//
		if (lm instanceof MigLayout) {
			//
			lm = new MigLayout();
			//
		} else if (lm instanceof Serializable serializable) {
			//
			lm = cast(LayoutManager.class, SerializationUtils.clone(serializable));
			//
		} // if
			//
		return ObjectUtils.defaultIfNull(lm, layoutManagerDefault);
		//
	}

	private static <T> T cast(final Class<T> clz, final Object value) {
		return clz != null && clz.isInstance(value) ? clz.cast(value) : null;
	}

	public static void main(final String[] args) throws TooManyListenersException {
		//
		final DeflateUtilsGui instance = new DeflateUtilsGui();
		//
		instance.setLayout(new MigLayout());
		//
		instance.init();
		//
		instance.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		//
		instance.pack();
		//
		instance.setVisible(true);
		//
	}

	@Override
	public void actionPerformed(final ActionEvent evt) {
		//
		final Object source = getSource(evt);
		//
		if (Objects.equals(source, absFile)) {
			//
			final JFileChooser jfc = new JFileChooser();
			//
			if (jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
				//
				set(file = jfc.getSelectedFile());
				//
			} // if
				//
		} else if (Objects.equals(source, absFileRefresh)) {
			//
			set(file);
			//
		} else if (Objects.equals(source, absCopyDeflated)) {
			//
			setContents(getSystemClipboard(getToolkit()), new StringSelection(getText(jtcDeflated)), null);
			//
		} // if
			//
	}

	private static String getText(final JTextComponent instance) {
		return instance != null ? instance.getText() : null;
	}

	private static void setContents(final Clipboard instance, final Transferable contents, final ClipboardOwner owner) {
		if (instance != null) {
			instance.setContents(contents, owner);
		}
	}

	private static Clipboard getSystemClipboard(final Toolkit instance) {
		return instance != null ? instance.getSystemClipboard() : null;
	}

	private void set(final File file) {
		//
		setText(jtcFile, getAbsolutePath(file));
		//
		setText(jtcFileLength, file != null ? Long.toString(file.length()) : null);
		//
		byte[] bs = null;
		//
		try {
			//
			final ContentInfo ci = new ContentInfoUtil().findMatch(bs = FileUtils.readFileToByteArray(file));
			//
			final DefaultTableModel dtm = cast(DefaultTableModel.class, tmContentInfo);
			//
			for (int i = (dtm != null ? dtm.getRowCount() : 0) - 1; i >= 0; i--) {
				//
				dtm.removeRow(i);
				//
			} // for
				//
			dtm.addRow(new FailableStream<>(Arrays.stream(getDeclaredFields(ci != null ? ci.getClass() : null))
					.filter(f -> f != null && !Modifier.isStatic(f.getModifiers()))).map(f -> {
						//
						if (f == null) {
							//
							return null;
							//
						} // if
							//
						f.setAccessible(true);
						//
						final Object o = f.get(ci);
						//
						if (o instanceof Object[] os) {
							//
							return StringUtils.joinWith(",", os);
							//
						} // if
							//
						return o;
						//
					}).collect(Collectors.toList()).toArray());
			//
		} catch (final IOException e) {
			//
			error(LOG, e.getMessage(), e);
			//
		} // try
			//
		final Method method = cast(Method.class, getSelectedItem(cbmMethod));
		//
		try {
			//
			setText(jtcDigest,
					toString(method != null ? method.invoke(null, FileUtils.readFileToByteArray(file)) : null));
			//
		} catch (final IllegalAccessException | InvocationTargetException | IOException e) {
			//
			error(LOG, e.getMessage(), e);
			//
		} // try
			//
		final byte[] bsDeflated = DeflateUtils.deflate(bs);
		//
		setText(jtcDeflated, encodeToString(Base64.getEncoder(), bsDeflated));
		//
		setText(jtcDeflatedLength, bsDeflated != null ? Long.toString(bsDeflated.length) : null);
		//
	}

	private static String encodeToString(final Encoder instance, final byte[] src) {
		return instance != null ? instance.encodeToString(src) : null;
	}

	private static String toString(final Object instance) {
		return instance != null ? instance.toString() : null;
	}

	private static Object getSelectedItem(final ComboBoxModel<?> instance) {
		return instance != null ? instance.getSelectedItem() : null;
	}

	private static Object getSource(final EventObject instance) {
		return instance != null ? instance.getSource() : null;
	}

	private static String getAbsolutePath(final File instance) {
		return instance != null ? instance.getAbsolutePath() : null;
	}

	private static void setText(final JTextComponent instance, final String text) {
		if (instance != null) {
			instance.setText(text);
		}
	}

	@Override
	public void dragEnter(final DropTargetDragEvent dtde) {
	}

	@Override
	public void dragOver(final DropTargetDragEvent dtde) {
	}

	@Override
	public void dropActionChanged(final DropTargetDragEvent dtde) {
	}

	@Override
	public void dragExit(final DropTargetEvent dte) {
	}

	@Override
	public void drop(final DropTargetDropEvent dtde) {
		//
		final Object source = getSource(dtde);
		//
		if (Objects.equals(source, dtFile)) {
			//
			acceptDrop(dtde, DnDConstants.ACTION_COPY_OR_MOVE);
			//
			final List<?> list = getList(getTransferable(dtde));
			//
			final int size = IterableUtils.size(list);
			//
			if (size > 1) {
				//
				throw new IllegalStateException();
				//
			} // if
				//
			set(file = cast(File.class, size == 1 ? IterableUtils.get(list, 0) : null));
			//
		} // if
			//
	}

	private static void acceptDrop(final DropTargetDropEvent instance, final int dropAction) {
		if (instance != null) {
			instance.acceptDrop(dropAction);
		}
	}

	private static List<?> getList(final Transferable transferable) {
		//
		final DataFlavor[] dataFlavors = transferable != null ? transferable.getTransferDataFlavors() : null;
		//
		DataFlavor dataFlavour = null;
		//
		List<?> list = null;
		//
		for (int i = 0; dataFlavors != null && i < dataFlavors.length; i++) {
			//
			if ((dataFlavour = dataFlavors[i]) == null
					|| !Objects.equals(List.class, dataFlavour.getRepresentationClass())) {
				//
				continue;
				//
			} // if
				//
			if (list == null) {
				//
				try {
					//
					list = cast(List.class, transferable != null ? transferable.getTransferData(dataFlavour) : null);
					//
				} catch (final UnsupportedFlavorException | IOException e) {
					//
					error(LOG, e.getMessage(), e);
					//
				} // try
					//
			} else {
				//
				throw new IllegalStateException();
				//
			} // if
				//
		} // for
			//
		return list;
		//
	}

	private static Transferable getTransferable(final DropTargetDropEvent instance) {
		//
		if (instance == null) {
			//
			return null;
			//
		} // if
			//
		try {
			//
			if (Narcissus.getObjectField(instance.getDropTargetContext(),
					DropTargetContext.class.getDeclaredField("dropTargetContextPeer")) == null) {
				//
				return null;
				//
			} // if
				//
		} catch (final NoSuchFieldException e) {
			//
			error(LOG, e.getMessage(), e);
			//
		} // try
			//
		return instance.getTransferable();
		//
	}

}