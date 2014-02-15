package lab;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.DropMode;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;

import java.awt.FlowLayout;
import javax.swing.BoxLayout;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JList;
import javax.swing.AbstractListModel;
import javax.swing.ListSelectionModel;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.TextArea;

import javax.swing.JMenuBar;
import javax.swing.JMenu;
import java.awt.Insets;
import javax.swing.JMenuItem;
import java.awt.Font;
import javax.swing.JSeparator;
import javax.swing.Box;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.awt.SystemColor;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.UnknownHostException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

import lab.Configuration.Group;

/*
 * Whisper - Application
 * Author: Ying Li	(yingl2)
 *         Utsav Drolia (udrolia)
 * Date: 2/8/2014
 */

public class UIThread {

    private JFrame frame;
    //private JTextField sendField;
    //private JTextField kindField;
    private JPanel sendPanel;
   // private JPanel sendPanel;
    private JButton sendButton;
    private JList hostList;
    private JScrollPane listPanel;
    static JTextArea textArea;
    static JTextArea statusArea;
    private JScrollPane textPanel;
    private JScrollPane statusPanel;
    private JLabel lblHosts;
    private JPanel hostPanel;
    private DefaultListModel listModel;
    private String[] hostnameList1;
    private String configuration_filename;
    private Configuration configuration;
    private ArrayList<String> hostnameList= new ArrayList<String>();
    private String[] displayedHostnameList;
    private String[] newMsgHostnameList;
    private Map<String, Integer> hostnameMap = new HashMap<String, Integer>();

    private String localName;
    private String currentPartner;
    private Map<String, StringBuffer> dialogMap = new HashMap<String, StringBuffer>();
    
    private File confFile = null;
    private Path confPath = null;
    private WatchService watcher = null;

    private final Object lock = new Object();

    private MessagePasser mp;
    private ClockService clockService;
	private JButton releaseButton;
    private final static String[] clocks = {"Logical","Vector"};
    private static HashMap<String,Clock> option = new HashMap<String,Clock>();

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIThread window = new UIThread();
                    window.frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the application.
     * 
     * @throws Exception
     */
    public UIThread() throws Exception {
        // ask user for configuration file
        configuration_filename = getInput("Welcome, please enter the path of configuration file:", "Welcome", JOptionPane.INFORMATION_MESSAGE);

        // read configuration file
        BufferedReader in = new BufferedReader(new FileReader(configuration_filename));

        // parse YAML
        Constructor constructor = new Constructor(Configuration.class);
        TypeDescription carDescription = new TypeDescription(Configuration.class);
        carDescription.putListPropertyType("configuration", Configuration.Host.class);
        carDescription.putListPropertyType("sendRules", Configuration.Rule.class);
        carDescription.putListPropertyType("receiveRules", Configuration.Rule.class);
        carDescription.putListPropertyType("groups", Configuration.Group.class);
        constructor.addTypeDescription(carDescription);
        Yaml yamlParser = new Yaml(constructor);

        configuration = (Configuration) yamlParser.load(in);
        configuration.initialize();
        //hostnameList = new String[configuration.configuration.size()];
        hostnameList1 = new String[configuration.groups.size()];
        newMsgHostnameList = new String[configuration.groups.size()];
        displayedHostnameList = new String[configuration.groups.size()];
        
        option.put(clocks[0], Clock.LOGICAL);
        option.put(clocks[1], Clock.VECTOR);

        // ask user for local name
        localName = getInput("Welcome, please enter your local name:", "Welcome", JOptionPane.INFORMATION_MESSAGE);
        // check if user is valid
        while (!configuration.hostMap.containsKey(localName)) {
            localName = getInput("Invalid local name\nPlease enter your local name:", "Welcome", JOptionPane.WARNING_MESSAGE);
        }

        int i = 0;
        for (Group host : configuration.getMyGroups(localName)) {
            if (host.name.equals(localName)) {
                displayedHostnameList[i] = String.format("%s (youself)", host.name);
            } else {
                displayedHostnameList[i] = host.name;
            }

            hostnameList1[i] = host.name;
            newMsgHostnameList[i] = String.format("%s (new message!)", displayedHostnameList[i]);
            hostnameMap.put(host.name, new Integer(i++));
        }
        for (Configuration.Host host : configuration.configuration) {
           hostnameList.add( host.name);
            }
        ArrayList<String> tempArr = new ArrayList<String>();

        for (String host : hostnameList1) {
            dialogMap.put(host, new StringBuffer());
            tempArr.add(host);
        }
        
       
        
        Clock c = option.get(getClock("Select Clock", "Welcome", JOptionPane.INFORMATION_MESSAGE, clocks));
        System.out.println(c);
        System.out.println("hostnameList :"+hostnameList);
        clockService = ClockFactory.getClockService(c, localName, hostnameList);
        mp = new MessagePasser(statusArea,configuration_filename, localName, clockService);
     
        // register watcher for configuration file
        confFile = new File(configuration_filename);
        confPath = Paths.get(confFile.getParentFile().getPath());
        watcher = FileSystems.getDefault().newWatchService();
        try {
            WatchKey key = confPath.register(watcher, ENTRY_MODIFY);
        } catch (IOException x) {
            System.err.println(x);
        }
        
        initialize();

        // start background receive thread
        (new Thread(new ReceiveThread(mp))).start();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        frame = new JFrame(String.format("Wisper -- %s", localName));
        frame.getContentPane().setBackground(Color.LIGHT_GRAY);
        frame.getContentPane().setLayout(new BorderLayout(0, 0));

        sendPanel = new JPanel();
        frame.getContentPane().add(sendPanel, BorderLayout.SOUTH);
        sendPanel.setLayout(new BoxLayout(sendPanel, BoxLayout.X_AXIS));
        // sendPanel.setLayout(new GridBagLayout());

        
        sendButton = new JButton("Request");
        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                synchronized (lock) {
                    String sendMsg = "this is request msg";
                    //String kind = kindField.getText();
                    String displayMsg = String.format("me: %s\n", sendMsg);
                    textArea.append(displayMsg);
                     // Store it in dialog and send
                    dialogMap.get(currentPartner).append(displayMsg);
                   // mp.multiSend(new TimeStampedMessage(currentPartner, kind, sendMsg, null),currentPartner);
                    mp.requestLock();
                }
            }
        });
        sendPanel.add(sendButton);
        sendPanel.setAutoscrolls(true);
        
        
        releaseButton = new JButton("Release");
        releaseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                synchronized (lock) {
                    String sendMsg = "this is release msg";
                    String displayMsg = String.format("me: %s\n", sendMsg);
                    textArea.append(displayMsg);
                    // Store it in dialog and send
                    dialogMap.get(currentPartner).append(displayMsg);
                   // mp.multiSend(new TimeStampedMessage(currentPartner, kind, sendMsg, null),currentPartner);
                    mp.releaseLock();
                }
            }
        });
        sendPanel.add(releaseButton);
        
        

        hostList = new JList();
        hostList.setValueIsAdjusting(true);
        hostList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        hostList.setFont(new Font("Lucida Grande", Font.PLAIN, 14));
        listModel = new DefaultListModel(); 
        for (String value : displayedHostnameList) {
            listModel.addElement(value);
        }

        hostList.setModel(listModel);
        hostList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent arg0) {
                if (!arg0.getValueIsAdjusting()) {
                    currentPartner = hostnameList1[hostList.getSelectedIndex()];
                    textArea.setText(dialogMap.get(currentPartner).toString());
                    // remove new message notification
                    int pos = hostnameMap.get(currentPartner).intValue();
                    listModel.setElementAt(displayedHostnameList[pos], pos);
                }
            }
        });
        hostList.setSelectedIndex(0);
        currentPartner = hostnameList1[0];

        listPanel = new JScrollPane();
        listPanel.setPreferredSize(new Dimension(150, 200));
        listPanel.setViewportView(hostList);

        textArea = new JTextArea();
        statusArea = new JTextArea();
        statusArea.setText("Status: ");

        textPanel = new JScrollPane();
        statusPanel = new JScrollPane();
        frame.getContentPane().add(textPanel, BorderLayout.CENTER);
        textPanel.setViewportView(textArea);
        //frame.getContentPane().add(statusPanel, BorderLayout.CENTER);
        sendPanel.add(statusArea);
        //textPanel.setViewportView(statusArea);
        lblHosts = new JLabel("Groups");
        lblHosts.setFont(new Font("Lucida Grande", Font.PLAIN, 14));
        lblHosts.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblHosts.setAlignmentY(Component.BOTTOM_ALIGNMENT);

        hostPanel = new JPanel();
        hostPanel.setBackground(SystemColor.text);
        frame.getContentPane().add(hostPanel, BorderLayout.WEST);
        hostPanel.setLayout(new BoxLayout(hostPanel, BoxLayout.Y_AXIS));
        hostPanel.add(lblHosts);

        hostPanel.add(listPanel);
        // textPanel.setViewportView(textArea);

        frame.setBackground(Color.LIGHT_GRAY);
        frame.setBounds(100, 100, 520, 445);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    // background thread to keep receiving and updating message
    private class ReceiveThread implements Runnable {
        private MessagePasser mp;

        // constructor
        public ReceiveThread(MessagePasser mp) {
            this.mp = mp;
        }

        public void run() {
            TimeStampedMessage msg = null;
            String src = "";
            String data;
            String srchost;
            int pos;

            while (true) {
                try {
            	msg =  mp.receive();
                }
                catch (IllegalArgumentException e)
                {
                	System.out.println("Clocks Do Not Match!");
                	e.printStackTrace();
                }
                if (msg != null) {
                	if(msg instanceof MultiCastMessage)
                    src = ((MultiCastMessage)msg).getGroupID();
                    srchost = msg.getSource();
                    data = (String) msg.getData();
                    if (dialogMap.containsKey(src)) {
                        synchronized (lock) {
                            dialogMap.get(src).append(String.format("%s: %s\n", srchost, data));
                            // update JList to remind a new message received
                            if (!currentPartner.equals(src)) {
                                pos = hostnameMap.get(src).intValue();
                                listModel.setElementAt(newMsgHostnameList[pos], pos);
                            } else {
                                textArea.setText(dialogMap.get(currentPartner).toString());
                            }
                        }
                    }
                }
            }
        }
    }

    // helper function to pop up a input dialog
    private String getInput(String msg, String title, int opt) {
        JFrame frame = new JFrame();
        return JOptionPane.showInputDialog(frame, msg, title, opt);
    }
    
    private String getClock(String msg, String title, int opt, String[] options) {
        JFrame frame = new JFrame();
        return (String) JOptionPane.showInputDialog (frame, msg, title, opt, null, options, 0);
        //return JOptionPane.showInputDialog(frame, msg, title, opt);
    }

}
