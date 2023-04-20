/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


import java.awt.image.BufferedImage;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

/*
 * FileChooserDemo.java uses these files:
 *   images/Open16.gif
 *   images/Save16.gif
 */
public class FileChooserDemo extends JPanel implements ActionListener {
    static private final String newline = "\n";
    static JButton openButton, saveButton;
    static JTextPane log;
    static final long GB = 1024L * 1024L * 1024L; // 1GB
    JFileChooser fc;

    ExecutorService pool = Executors.newFixedThreadPool(1);

    static final String GDAL_PATH = "gdalwin32-1.6/bin/gdal_translate.exe";
    static final String PARAMS = " -co COMPRESS=DEFLATE ";

    File in = null;

    public FileChooserDemo() {
        super(new BorderLayout());
        //Create the log first, because the action listeners
        //need to refer to it.
        log = new JTextPane();
        log.setSize(650, 750);
        log.setBorder(new EmptyBorder(new Insets(10, 10, 10, 10)));
        //tPane.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        log.setMargin(new Insets(5, 5, 5, 5));

        this.add(log);
        JScrollPane logScrollPane = new JScrollPane(log);

        //Create a file chooser
        fc = new JFileChooser();

        //Uncomment one of the following lines to try a different
        //file selection mode.  The first allows just directories
        //to be selected (and, at least in the Java look and feel,
        //shown).  The second allows both files and directories
        //to be selected.  If you leave these lines commented out,
        //then the default mode (FILES_ONLY) will be used.
        //
        //fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        //fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        //Create the open button.  We use the image from the JLF
        //Graphics Repository (but we extracted it from the jar).
        fc.addChoosableFileFilter(new ImageFilter());
        fc.setAcceptAllFileFilterUsed(false);

        //Add custom icons for file types.
        fc.setFileView(new ImageFileView());

        //Add the preview pane.
        fc.setAccessory(new ImagePreview(fc));

        openButton = new JButton("Open a File...", createImageIcon("images/Open16.gif"));
        openButton.addActionListener(this);

        //Create the save button.  We use the image from the JLF
        //Graphics Repository (but we extracted it from the jar).
        saveButton = new JButton("Save a File...", createImageIcon("images/Save16.gif"));
        saveButton.addActionListener(this);

        //For layout purposes, put the buttons in a separate panel
        JPanel buttonPanel = new JPanel(); //use FlowLayout
        buttonPanel.add(openButton);
        buttonPanel.add(saveButton);

        add(buttonPanel, BorderLayout.PAGE_START);
        add(logScrollPane, BorderLayout.CENTER);
    }

    public void actionPerformed(ActionEvent e) {
        //Handle open button action.
        if (e.getSource() == openButton) {
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int returnVal = fc.showDialog(FileChooserDemo.this, "Open");
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                in = file;
                //This is where a real application would open the file.
                appendToPane("Opening: " + file.getName() + "." + newline);
            } else {
                appendToPane("Open command cancelled by user." + newline);
            }
            log.setCaretPosition(log.getDocument().getLength());

            //Handle save button action.
        } else if (e.getSource() == saveButton) {
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnVal = fc.showDialog(FileChooserDemo.this, "Open");
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                //This is where a real application would save the file.
                if (in == null) {
                    appendToPane("There is no selected input file!", Color.black);
                } else {
                    File input = in;
                    if (check(input.length())) {
                        appendToPane("Splitting the file..." + newline);
                        pool.execute(() -> {
                            List<String> splitImage;
                            if (file.isDirectory()) {
                                //exec(input.getAbsolutePath(), file.getAbsolutePath() + "/" + input.getName());
                                splitImage = split(input, file.getAbsolutePath() + "/" + input.getName());
                            } else {
                                // exec(input.getAbsolutePath(), file.getAbsolutePath());
                                splitImage = split(input, file.getAbsolutePath());
                            }
                            appendToPane("File successfully split into: " + splitImage + newline, Color.BLUE);
                        });
                    } else {
                        appendToPane("There is no need to split the file." + newline, Color.RED);
                    }

                }

                in = null;
            } else {
                appendToPane("Save command cancelled by user." + newline, Color.RED);
            }
            log.setCaretPosition(log.getDocument().getLength());
        }
        fc.setSelectedFile(null);
    }

    /**
     * Returns an ImageIcon, or null if the path was invalid.
     */
    protected static ImageIcon createImageIcon(String path) {
        URL imgURL = FileChooserDemo.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    private void appendToPane(String msg) {
        appendToPane(msg, Color.black);
    }

    private void appendToPane(String msg, Color c) {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);

        aset = sc.addAttribute(aset, StyleConstants.FontFamily, "Lucida Console");
        aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);

        int len = log.getDocument().getLength();
        log.setCaretPosition(len);
        log.setCharacterAttributes(aset, false);
        log.replaceSelection(msg);
    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event dispatch thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("FileChooserDemo");
        frame.setSize(650, 750);
        frame.setResizable(true);
        int windowWidth = frame.getWidth();
        int windowHeight = frame.getHeight();
        Toolkit kit = Toolkit.getDefaultToolkit();
        Dimension screenSize = kit.getScreenSize();
        int screenWidth = screenSize.width;
        int screenHeight = screenSize.height;
        frame.setLocation(screenWidth / 2 - windowWidth / 2 + 200, screenHeight / 2 - windowHeight / 2 + 200);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Add content to the window.
        frame.add(new FileChooserDemo());

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }


    private static String nextName(String originalPath, int id) {
        String[] split = originalPath.split("\\.");
        return split[0] + "_" + id + "." + split[1];
    }

    private static List<String> split(File file, String outputPath) {
        try {
            BufferedImage image = ImageIO.read(file);

            //Split the image into smaller TIFF files
            int w = image.getWidth();
            int h = image.getHeight();

            int numCols = 2;
            int numRows = 2;
            int cellWidth = w / numCols;
            int cellHeight = h / numRows;

            List<String> res = new ArrayList<>();
            int i = 0;
            //Iterate through the image and write out each of the smaller TIFF files
            for (int y = 0; y < numRows; y++) {
                for (int x = 0; x < numCols; x++) {
                    BufferedImage subImage = image.getSubimage(x * cellWidth, y * cellHeight, cellWidth, cellHeight);
                    String newName = nextName(outputPath, i++);
                    File outputFile = new File(newName);
                    res.add(newName);
                    ImageIO.write(subImage, "tiff", outputFile);
                }
            }

            return res;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static boolean check(long fileSize) {
        return fileSize / GB >= 2;
    }


    public static void main(String[] args) {
        //Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
        SwingUtilities.invokeLater(() -> {
            //Turn off metal's use of bold fonts
            UIManager.put("swing.boldMetal", Boolean.FALSE);
            createAndShowGUI();
        });
    }
}
