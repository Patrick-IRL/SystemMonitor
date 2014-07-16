package com.patmoorehouse.sysmon;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.hyperic.sigar.Sigar;

public class SysMon_Server {
	
	// Variables
	public static int port = 56565;
	public static String ip = "";
	
	public static ServerSocket server;
	
	public static ArrayList<Socket> list_sockets = new ArrayList<Socket>();
	public static ArrayList<Integer> list_client_states = new ArrayList<Integer>();
	public static ArrayList<DataPackage> list_data = new ArrayList<DataPackage>();
	public static String sysInfo = "";
	
	public static Runnable accept = new Runnable() {
		@Override
		public void run() {
			new Thread(send).start();
			new Thread(receive).start();
			
			while(true){
				try {
					Socket socket = server.accept();
					ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
					
					String username = (String) ois.readObject();
					
					boolean accepted = true;
					
					for (int i = 0; i < list_data.size(); i++){
						if (list_data.get(i).username.toLowerCase().equals(username.toLowerCase())){
							accepted = false;
							break;
						}
					}
					
					ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
					
					if(accepted){
						oos.writeObject("Welcome!");
						
						list_clients_model.addElement(username + " - " + socket.getInetAddress().getHostAddress() + " - " + socket.getInetAddress().getHostName());
						list_client_states.add(0);
						
						list_data.add(new DataPackage());
						list_sockets.add(socket);
					}else{
						oos.writeObject("Name Already Taken!");
					}
					
				} catch (Exception e) {}
				
				try
				{
					Thread.sleep(1);
				}
				catch (Exception ex) {}
			}
		}
	};
	
	public static Runnable send = new Runnable() {
		@Override
		public void run() {
			ObjectOutputStream oos;
			
			while(true){
				for(int i = 0; i < list_sockets.size(); i++){
					try {
						oos = new ObjectOutputStream(list_sockets.get(i).getOutputStream());
						int client_state = list_client_states.get(i);
						oos.writeObject(client_state);
						
						Sigar sigar = new Sigar();
						double byteToGigabyte = 1073741824;
						DecimalFormat df = new DecimalFormat("#.00");

						sysInfo = "CPU: " + df.format(sigar.getCpuPerc().getCombined());
						
						oos = new ObjectOutputStream(list_sockets.get(i).getOutputStream());
						oos.writeObject(sysInfo);
						
						
						if(client_state == 1){ // Kicked by Server
							disconnectClient(i);
							i--;
						}else if(client_state == 2){ // Server Closed
							disconnectClient(i);
							i--;
						}
					} catch (Exception e) {}
				}
				
				try
				{
					Thread.sleep(1);
				}
				catch (Exception ex) {}
			}
		}
	};
	
	public static Runnable receive = new Runnable() {
		@Override
		public void run() {
			ObjectInputStream ois;
			
			while(true){
				for(int i = 0; i < list_sockets.size(); i++){
					try {
						ois = new ObjectInputStream(list_sockets.get(i).getInputStream());
						int received_state = (Integer) ois.readObject();
						
						ois = new ObjectInputStream(list_sockets.get(i).getInputStream());
						DataPackage dp = (DataPackage) ois.readObject();
						
						list_data.set(i, dp);
						if(received_state == 1){ // Client Disconnect by User
							disconnectClient(i);
							i--;
						}
					} catch (Exception e) { // Client Disconnect - Failed to Respond
						disconnectClient(i);
						i--;
					}
				}
				
				try
				{
					Thread.sleep(1);
				}
				catch (Exception ex) {}
			}
		}
	};
	
	public static void disconnectClient(int index){
		try {
			list_clients_model.removeElementAt(index);
			list_client_states.remove(index);
			list_data.remove(index);
			list_sockets.remove(index);
		} catch (Exception e) {}
	}
	
	// UI
	public static JFrame frame;
	
	public static JPanel content;
	public static JPanel panel1;
	public static JPanel panel2;
	public static JPanel panel3;
	
	public static JButton btn_disconnect;
	
	public static JList list_clients;
	public static DefaultListModel list_clients_model;
	
	public static void main(String[] args) {
		
		try {
			ip = InetAddress.getLocalHost().getHostAddress() + ":" + port;
			
			server = new ServerSocket(port, 0, InetAddress.getLocalHost());
			new Thread(accept).start();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Error: " + e.getMessage(), "Alert", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}
		
		btn_disconnect = new JButton();
		btn_disconnect.setText("Disconnect");
		//btn_disconnect.setEnabled(false);
		btn_disconnect.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent ae) {
				int selected = list_clients.getSelectedIndex();
				
				if(selected != -1){
					try {
						list_client_states.set(selected, 1);
					} catch (Exception e) {
						JOptionPane.showMessageDialog(null, e.getMessage(), "Alert!", JOptionPane.ERROR_MESSAGE);
					}
				}
				
			}
			
		});
		
		list_clients_model = new DefaultListModel();
		list_clients = new JList(list_clients_model);
		list_clients.addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent lse) {
				
				if(lse.getValueIsAdjusting()){
					System.out.println(list_clients.getSelectedIndex());
				}
			}
		});
		
		frame = new JFrame();
		frame.setTitle("Server: " + ip);
		
		frame.addWindowListener(new WindowListener() {
			
			public void windowOpened(WindowEvent e) {}
			public void windowIconified(WindowEvent e) {}
			public void windowDeiconified(WindowEvent e) {}
			public void windowDeactivated(WindowEvent e) {}
			
			@Override
			public void windowClosing(WindowEvent e) {
				
				while(list_sockets.size() != 0){
					try {
						for(int i = 0; i < list_client_states.size(); i++){
							list_client_states.set(i, 2);
						}
					} catch (Exception e2) {}
				}
				
				System.exit(0);
			}
			
			public void windowClosed(WindowEvent e) {}
			public void windowActivated(WindowEvent e) {}
		});
		
		panel1 = new JPanel();
		panel1.setLayout(new GridLayout(1, 1, 1, 1));
		panel1.add(btn_disconnect);
		
		panel2 = new JPanel();
		panel2.add(new JLabel(ip));
		
		panel3 = new JPanel();
		panel3.setLayout(new BorderLayout(1, 1));
		panel3.add(panel1, BorderLayout.NORTH);
		panel3.add(new JScrollPane(list_clients), BorderLayout.CENTER);
		panel3.add(panel2, BorderLayout.SOUTH);
		
		content = new JPanel();
		content.setLayout(new GridLayout(1, 1, 1, 1));
		content.add(panel3);
		
		content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		frame.setContentPane(content);
		frame.pack();
		frame.setSize(350, 400);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

}
