package uecide.plugin;

import uecide.app.*;
import uecide.app.debug.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;


public class Grapher extends BasePlugin implements MessageConsumer
{
    JFrame win;
    JGrapher graph;
    Serial port;
    JComboBox baudRates;
    JScrollBar scrollbackBar;

    int baudRate;

    boolean ready = false;

    static boolean isOpen = false;

    public void init(Editor editor)
    {
        this.editor = editor;
    }

    public void run()
    {
        if (Grapher.isOpen) {
            return;
        }
        Grapher.isOpen = true;
        win = new JFrame(Translate.t("Grapher"));
        win.getContentPane().setLayout(new BorderLayout());
        win.setResizable(false);

        Box box = Box.createVerticalBox();

        Box line = Box.createHorizontalBox();

        graph = new JGrapher();
        Font f = Preferences.getFont("serial.font");
        if (f == null) {
            f = new Font("Monospaced", Font.PLAIN, 12);
            Preferences.set("serial.font", "Monospaced,plain,12");
        }
        graph.setFont(f);

        line.add(graph);
        box.add(line);
        
        line = Box.createHorizontalBox();

        line.add(Box.createHorizontalGlue());

        JLabel label = new JLabel(Translate.t("Baud Rate") + ": ");
        line.add(label);
        baudRates = new JComboBox(new String[] { "300", "1200", "2400", "4800", "9600", "14400", "28800", "38400", "57600", "115200", "230400", "460800", "500000", "576000", "1000000", "1152000"});
        final MessageConsumer mc = this;
        baudRates.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (ready) {
                    String value = (String) baudRates.getSelectedItem();
                    baudRate = Integer.parseInt(value);
                    Preferences.set("serial.debug_rate", value);
                    try {
                        if (port != null) {
                            port.dispose();
                            port = null;
                        }
                    } catch (Exception e) {
                        System.err.println("Unable to release port");
                    }
                    try {
                        port = new Serial(baudRate);
                        port.addListener(mc);
                    } catch (Exception e) {
                        System.err.println("Unable to reopen port");
                    }
                }
            }
        });

        line.add(baudRates);

        box.add(line);

        win.getContentPane().add(box);
        win.pack();

        Dimension size = win.getSize();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        win.setLocation((screen.width - size.width) / 2,
                          (screen.height - size.height) / 2);

        win.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        win.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                close();
            }
        });
//        Base.registerWindowCloseKeys(win.getRootPane(), new ActionListener() {
//            public void actionPerformed(ActionEvent actionEvent) {
//                close();
//            }
//        });
        Base.setIcon(win);

        try {
            baudRate = Integer.parseInt(Preferences.get("serial.debug_rate"));
            baudRates.setSelectedItem(Preferences.get("serial.debug_rate"));
            port = new Serial(baudRate);
        } catch(Exception e) {
            System.err.println("Unable to open serial port");
            return;
        }
        port.addListener(this);
        win.setVisible(true);
        ready = true;
    }

    public void close()
    {
        port.dispose();
        win.dispose();
        ready = false;
        Grapher.isOpen = false;
    }

    public String getMenuTitle()
    {
        return(Translate.t("Grapher"));
    }

    char command = 0;
    String data = "";

    public void message(String m) {
        for (char c : m.toCharArray()) {
            if (command == 0) {
                switch(c) {
                    case 'A':
                    case 'V':
                    case 'S':
                    case 'M':
                    case 'B':
                    case 'F':
                    case 'Y':
                        command = c;
                        break;
                }
            } else {
                if(c == '\n') {
                    executeCommand();
                    command = 0;
                    data = "";
                } else {
                    data += Character.toString(c);
                }
            }
        }
    }

    public void executeCommand() {
        String params[] = data.split(":");
        switch(command) {
            case 'A':
                if(params.length == 4) {
                    graph.addSeries(params[0], new Color(
                        Integer.parseInt(params[1]),
                        Integer.parseInt(params[2]),
                        Integer.parseInt(params[3])
                    ));
                }
                break;
            case 'V':
                float vals[] = new float[params.length];
                for (int i = 0; i < params.length; i++) {
                    vals[i] = Float.parseFloat(params[i]);
                }
                graph.addDataPoint(vals);
                break;
            case 'S':
                if (params.length == 2) {
                    graph.setScreenSize(new Dimension(
                        Integer.parseInt(params[0]),
                        Integer.parseInt(params[1])
                    ));
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            resizeWindow();
                        }
                    });
                }
                break;
            case 'M':
                if (params.length == 4) {
                    graph.setTopMargin(Integer.parseInt(params[0]));
                    graph.setRightMargin(Integer.parseInt(params[1]));
                    graph.setBottomMargin(Integer.parseInt(params[2]));
                    graph.setLeftMargin(Integer.parseInt(params[3]));
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            resizeWindow();
                        }
                    });
                } 
                break;

            case 'B':
                if (params.length == 3) {
                    graph.setBackgroundColor(new Color(
                        Integer.parseInt(params[0]),
                        Integer.parseInt(params[1]),
                        Integer.parseInt(params[2])
                    ));
                }
                break;
            case 'F':
                if (params.length == 3) {
                    graph.setAxisColor(new Color(
                        Integer.parseInt(params[0]),
                        Integer.parseInt(params[1]),
                        Integer.parseInt(params[2])
                    ));
                }
                break;
            case 'Y':
                if (params.length == 3) {
                    graph.setYMinimum(Integer.parseInt(params[0]));
                    graph.setYMaximum(Integer.parseInt(params[1]));
                    graph.setYStep(Integer.parseInt(params[2]));
                }
                break;
        }
    }
    
    public void message(String m, int c) {
        message(m);
    }

    public ImageIcon toolbarIcon() {
        ImageIcon icon = new ImageIcon(getResourceURL("uecide/plugin/Grapher/grapher.png"));
        return icon;
    }

    public void resizeWindow() {
        win.pack();
        win.revalidate();
        win.repaint();
    }
}

