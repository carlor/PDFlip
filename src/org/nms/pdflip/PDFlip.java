/* PDFlip.java
 * Copyright (C) 2012 Nathan M. Swan
 * Distributed under the Boost Software License (see LICENSE file)
 */
package org.nms.pdflip;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.jpedal.PdfDecoder;
import org.jpedal.exception.PdfException;
import org.jpedal.fonts.FontMappings;

public class PDFlip extends JFrame 
        implements KeyEventDispatcher, AWTEventListener {
    
    // entry point
    public static void main(String[] args) {
        try {
            new PDFlip(getFilename());
        } catch (Throwable e) {
            error(e);
        }
    }
    
    // opens the file dialog
    private static String getFilename() {
        FileDialog fd = new FileDialog((JFrame)null, 
                "Select PDF File", FileDialog.LOAD);
        fd.setFilenameFilter(
                new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".pdf");
                    }
                });
        fd.setVisible(true);
        
        String r = fd.getFile();
        if (r == null) {
            error(null);
            return null;
        } else {
            return fd.getDirectory() + "/" + r;
        }
    }
    
    // shows an error, closes program
    private static void error(Object obj) {
        try {
            if (obj != null) {
                JOptionPane.showMessageDialog
                        (null, obj, "Error!", JOptionPane.ERROR_MESSAGE);
            }
        } finally {
            fullscreen(null);
            System.exit(obj == null ? 0 : 1);
        }
    }
    
    // sets the full screen window to f for every screen
    private static void fullscreen(Frame f) throws HeadlessException {
        for(GraphicsDevice gd : GraphicsEnvironment
                                .getLocalGraphicsEnvironment()
                                .getScreenDevices()) {
            if (gd.isFullScreenSupported()) {
                gd.setFullScreenWindow(f);
            }
        }
    }
    
    // shows the window
    private PDFlip(String fname) {
        super();
        readFile(fname);
        initUI();
        setVisible(true);
    }
    
    // reads the images from the pdf into pageImages
    private void readFile(String fname) {
        PdfDecoder pdf = new PdfDecoder();
        FontMappings.setFontReplacements();
        try {
            pdf.openPdfFile(fname);
            int pc = pdf.getPageCount();
            pageImages = new ArrayList<BufferedImage>(pc);
            for(int i=1; i <= pc; i++) {
                pageImages.add(pdf.getPageAsImage(i));
            }
            pdf.closePdfFile();
        } catch (PdfException pe) {
            error(pe);
        }
    }
    
    // initializes the ui
    private void initUI() throws HeadlessException {
        initListeners();
        initContentPane();
        initLabel();
        initFullscreen();
    }
    
    // makes this the main keyboard/mouse listener
    private void initListeners() {
        KeyboardFocusManager
                .getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(this);
        Toolkit.getDefaultToolkit()
                .addAWTEventListener(this, AWTEvent.MOUSE_EVENT_MASK);
    }
    
    // makes the content pane black, BorderLayout
    private void initContentPane() {
        getContentPane().setBackground(Color.BLACK);
        getContentPane().setLayout(new BorderLayout());
    }
    
    // the label holds the icon with the shown image
    private void initLabel() {
        label = new JLabel(new ImageIcon(pageImages.get(0)));
        getContentPane().add(label, BorderLayout.CENTER);
    }
    
    // makes this fullscreen
    private void initFullscreen() throws HeadlessException {
        boolean useFullscreen = true; // usefull in debugging
        if (useFullscreen) {
            setUndecorated(true);
            fullscreen(this);
        } else {
            setState(JFrame.MAXIMIZED_BOTH);
        }
    }
    
    private void refresh() {
        ((ImageIcon)label.getIcon()).setImage(pageImages.get(currentPage));
        repaint();
    }
    
    public static final int UP_CLICK = MouseEvent.BUTTON3;
    public static final int DOWN_CLICK = MouseEvent.BUTTON1;
    
    private List<BufferedImage> pageImages;
    private JLabel label;
    private int currentPage = 0;

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            setVisible(false);
            error(null);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void eventDispatched(AWTEvent awte) {
        if (awte instanceof MouseEvent) {
            MouseEvent e = (MouseEvent)awte;
            if (e.getID() == MouseEvent.MOUSE_CLICKED) {
                if (e.getButton() == UP_CLICK) {
                    if (currentPage > 0) {
                        currentPage--;
                    }
                } else if (e.getButton() == DOWN_CLICK) {
                    if (currentPage < pageImages.size()-1) {
                        currentPage++;
                    }
                }
                refresh();
            }
        }
    }
}
