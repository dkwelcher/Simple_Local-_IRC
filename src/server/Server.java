package server;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Server {

	public static void main(String[] args) throws IOException{
		// TODO Auto-generated method stub
		
		boolean run = true;
		int PORT = 194;
		
		DatagramSocket socket = new DatagramSocket(PORT);
		DataManager serverData = new DataManager();
		
		System.out.println("Server started");
		
		
		while(run){
			try {
				System.out.println("in loop");
				byte[] buffer = new byte[256];
				DatagramPacket request = new DatagramPacket(buffer, buffer.length);
				socket.receive(request);
				System.out.println("recieved");
				System.out.println("Recieved connection at " + request.getAddress());
				System.out.println(request.getData().toString());
				new ClientHandler(request, socket, serverData);
				//
				
			}catch(Exception e){
				System.out.println(e.getMessage());
			}
		
		}
		
	}
	
	

}

class DataManager{
	
	ArrayList<String> global;
	
	DataManager(){
		global = new ArrayList<String>();
	}
	
	public void addToTextLog(String text) {
		global.add(text);
	}
	
	
}

class ClientHandler implements Runnable {
	
	DatagramSocket socket;
	DatagramPacket packet;
	boolean run;
	DataManager manager;
	
	ClientHandler(DatagramPacket pkt, DatagramSocket sock, DataManager data){
		this.packet = pkt;
		this.socket = sock;
		this.run = true;
		this.manager = data;
		new Thread(this).start();
		System.out.println("created thread");
	}
	
	public void run() {
		try {
			while(run) {
				byte[] buffer = new byte[256];
				this.packet = new DatagramPacket(buffer, buffer.length);
				this.socket.receive(this.packet);
				String text = new String(this.packet.getData(), 0, this.packet.getLength());
				System.out.println("recieved: " + text);
			}
		}catch(Exception e) {
			System.out.println(e.getMessage());
		}
		
	}
}