package es.unican.ctr.marte2mast.files;

/*---------------------------------------------------------------------
 --                           Marte2Mast                              --
 --      Converter of Schedulability analysis models made with UML2   --
 --   and The UML Profile for MARTE to MAST, the Analysis Suite for   --
 --                      Real-Time Applications                       --
 --                                                                   --
 --                     Copyright (C) 2010-2011                       --
 --                 Universidad de Cantabria, SPAIN                   --
 --                                                                   --
 --                                                                   --
 --           URL: http://mast.unican.es/umlmast/marte2mast           --
 --                                                                   --
 --  Authors: Alvaro Garcia Cuesta   alvaro@binarynonsense.com        --
 --           Julio Medina           julio.medina@unican.es           --
 --                                                                   --
 -- This program is free software; you can redistribute it and/or     --
 -- modify it under the terms of the GNU General Public               --
 -- License as published by the Free Software Foundation; either      --
 -- version 2 of the License, or (at your option) any later version.  --
 --                                                                   --
 -- This program is distributed in the hope that it will be useful,   --
 -- but WITHOUT ANY WARRANTY; without even the implied warranty of    --
 -- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU --
 -- General Public License for more details.                          --
 --                                                                   --
 -- You should have received a copy of the GNU General Public         --
 -- License along with this program; if not, write to the             --
 -- Free Software Foundation, Inc., 59 Temple Place - Suite 330,      --
 -- Boston, MA 02111-1307, USA.                                       --
 --                                                                   --
 ---------------------------------------------------------------------*/

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

/**
 * Class to create a GUI that asks for a file.
 */
public class GUI extends JFrame implements ActionListener {
	// refs:
	// http://www.particle.kth.se/~lindsey/JavaCourse/Book/Part1/Java/Chapter09/chooser.html
	private static final long serialVersionUID = 1L;// set to avoid warning

	public static String mFilePath = "";

	/**
	 * This class is used to filter the type of files shown by JFileChooser. We
	 * just want .xml file.
	 *
	 */
	public class fileFilter extends javax.swing.filechooser.FileFilter {
		public boolean accept(File file) {
			return file.getName().toLowerCase().endsWith(".xml") || file.isDirectory();
		}

		public String getDescription() {
			return "XML files (*.xml)";
		}
	}// class fileFilter

	JMenuItem menuOpen = null;
	JMenuItem menuClose = null;

	JTextArea textArea;

	fileFilter xmlFileFilter = new fileFilter();
	File theFile = null;

	GUI(String windowTitle) {

		super(windowTitle);

		mFilePath = "";

		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());

		textArea = new JTextArea("\n  If you want to recover the results from MAST into a uml model,\n select MAST's xml output file from File>Open\n\n" + "  If you don't want to recover the results just close this window or go to File>Quit");
		textArea.setLineWrap(true);

		contentPane.add(textArea, "Center");

		// menu
		JMenu menu = new JMenu("File");
		// sub menus
		menu.add(menuOpen = createMenuItem("Open"));
		menu.add(menuClose = createMenuItem("Quit"));
		// menu bar
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(menu);
		setJMenuBar(menuBar);

		setSize(450, 150);
		this.setLocation(100, 100);
	}

	// custom implementation of ActionListener's method:
	public void actionPerformed(ActionEvent e) {

		boolean status = false;

		String command = e.getActionCommand();
		if (command.equals("Open")) {
			// Open a file
			status = openFile();
			if (!status) {
				JOptionPane.showMessageDialog(null, "Error opening file!", "File Open Error", JOptionPane.ERROR_MESSAGE);
			} else {
				dispose();
			}
		}// end if open
		else if (command.equals("Quit")) {
			dispose();
		}// end if quit

	} // actionPerformed

	private JMenuItem createMenuItem(String itemName) {
		JMenuItem m = new JMenuItem(itemName);
		m.addActionListener(this);
		return m;
	} // makeMenuItem

	boolean openFile() {

		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Open File");
		// only files
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setCurrentDirectory(new File("."));
		// fileChooser.setCurrentDirectory (new File
		// (System.getProperties().get("osgi.instance.area").toString().substring(6)));
		// set filter
		fileChooser.setFileFilter(xmlFileFilter);

		int result = fileChooser.showOpenDialog(this);
		if (result == JFileChooser.CANCEL_OPTION) {
			return true;
		} else if (result == JFileChooser.APPROVE_OPTION) {
			theFile = fileChooser.getSelectedFile();
			// Log.println("filechooser: "+theFile.toString());
			mFilePath = theFile.toString();
		} else {
			return false;
		}

		return true;
	}// end openFile

}// GUI