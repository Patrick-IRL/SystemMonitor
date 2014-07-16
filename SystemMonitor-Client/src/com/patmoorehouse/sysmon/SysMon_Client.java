package com.patmoorehouse.sysmon;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;


public class SysMon_Client extends JComponent {

		public static Socket socket;
		public static int port = 56565;
		public static String ip = "";
		
		public static String sysInfo = "init";
		
		public String username = System.getProperty("user.name");
		
		public int state = 0;
		public boolean connected = true;
		
		public static JPanel content;
		public static JPanel panel1;
		public static JPanel panel2;
		public static JPanel panel3;
		public static JLabel syst;
		
		public static void main(String[] args) {
			
			JFrame frame = new JFrame();
			frame.setTitle("Client");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			
			frame.add(new SysMon_Client());
			
			panel2 = new JPanel();
			syst = new JLabel(sysInfo);
			panel2.add(syst);
			
			panel3 = new JPanel();
			panel3.setLayout(new BorderLayout(1, 1));
			panel3.add(panel2, BorderLayout.SOUTH);
			
			content = new JPanel();
			content.setLayout(new GridLayout(1, 1, 1, 1));
			content.add(panel3);
			
			content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			
			frame.setContentPane(content);
			frame.pack();
			frame.setSize(200, 80);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		}
		
		public SysMon_Client(){
			
			try {
				String local;
				
				try {
					local = InetAddress.getLocalHost().getHostAddress() + ":" + port;
				} catch (UnknownHostException e) {
					local = "Network Error";
				}
				
				ip = (String) JOptionPane.showInputDialog(null, "IP: ", "Info", JOptionPane.INFORMATION_MESSAGE, null, null, local);
				
				port = Integer.parseInt(ip.substring(ip.indexOf(":") + 1));
				ip = ip.substring(0, ip.indexOf(":"));
				
				socket = new Socket(ip, port);
				
				String username = System.getProperty("user.name");
				username = (String) JOptionPane.showInputDialog(null, "Username: ", "Info", JOptionPane.INFORMATION_MESSAGE, null, null, username);
				
				
				ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
				oos.writeObject(username);
				
				ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
				String response = (String) ois.readObject();
				JOptionPane.showMessageDialog(null, response, "Response", JOptionPane.INFORMATION_MESSAGE);
				
				if (response.equals("Your name is already taken!"))
				{
					System.exit(0);
				}
				
				new Thread(send).start();
				new Thread(receive).start();
				
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, e.getMessage(), "Alert", JOptionPane.ERROR_MESSAGE);
				System.exit(0);
			}
		}
		
		Runnable send = new Runnable() {
			@Override
			public void run() {
				ObjectOutputStream oos;
				
				while(connected){
					if (socket != null){
						try {
							DataPackage dp = new DataPackage();
							dp.username = username;
							
							oos = new ObjectOutputStream(socket.getOutputStream());
							oos.writeObject(state);
							
							oos = new ObjectOutputStream(socket.getOutputStream());
							oos.writeObject(dp);
							
							if(state == 1){ // Client Disconnected
								connected = false;
								socket = null;
								
								JOptionPane.showMessageDialog(null, "Client Disconnected", "Info", JOptionPane.INFORMATION_MESSAGE);
								System.exit(0);
							}
						} catch (Exception e) {}
					}else{
						break;
					}
				}
			}
		};
		
		Runnable receive = new Runnable() {
			@Override
			public void run() {
				ObjectInputStream ois;
				
				while(connected){
					try {
						ois = new ObjectInputStream(socket.getInputStream());
						int receive_state = (Integer) ois.readObject();
						
						if (receive_state == 1){ // kicked by server
							connected = false;
							socket = null;
							
							JOptionPane.showMessageDialog(null, "Kicked", "Info", JOptionPane.INFORMATION_MESSAGE);
							System.exit(0);
						}else if (receive_state == 2){ // server disconnected
							connected = false;
							socket = null;
							
							JOptionPane.showMessageDialog(null, "Server Disconnected", "Info", JOptionPane.INFORMATION_MESSAGE);
							System.exit(0);
						}
						
						ois = new ObjectInputStream(socket.getInputStream());
						String cpuInfo = (String) ois.readObject();
						
						sysInfo = cpuInfo;
						syst.setText(sysInfo);
					} catch (Exception e) {}
				}
			}
		};
	}
