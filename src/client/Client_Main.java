package client;


import java.io.*;
import java.net.*;
import java.util.*;
public class Client_Main {

	public static void main(String[] args){
		// TODO Auto-generated method stub
		TestClass.dotest();
	}

}

class Listener implements Runnable{
	
	DatagramSocket s;
	
	Listener(DatagramSocket s){
		this.s = s;
		this.run();
	}
	
	  public void run(){
		byte[] buf = new byte[256];
		DatagramPacket rec = new DatagramPacket(buf, buf.length);
	    try {
			s.receive(rec);
			String text = new String(rec.getData(), 0, rec.getLength());
			System.out.println(text);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	  }
	}

class TestClass {
	public static void dotest(){
		
		try {
			DatagramSocket s = new DatagramSocket();
			
			Scanner inFromUser = new Scanner(System.in);
		    System.out.println("Please type your username: ");
		    String username = inFromUser.nextLine();
		    System.out.println("Please start typing your message and hit enter when you are done.");
		    System.out.println("To exit the program, please type Quit.");
		    String message = "";
		    message = username  + inFromUser.nextLine();
		    
		    while(!message.equals("Quit")) {
	            
	            // construct datagram packet
		    	byte[] buf = message.getBytes();
	            InetAddress address = InetAddress.getByName("127.0.0.1");
	            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 194);
	            
	            
	            // send the packet -- repeat until the user type "Quit"
	            
	            s.send(packet);
	            System.out.println("Message sent, input next message or 'Quit'");
	            message = inFromUser.nextLine();
	            
	            
	            
	            
	            }
	            // close the datagram socket in the end. 
	       s.close();
	       inFromUser.close();
	       
		} catch (IOException e) {

            System.err.print(e.getMessage());

        }
       
	}
}