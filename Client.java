import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;


public class Client{
    // Socket thorugh which client receives forwards from server i.e messages from other clients
    static Socket forwardsSocket;

    // Socket through which client send messages to server to forward it to other clients
    static Socket sendMessageSocket;

    // Keep track for both sockets is connections are opened.
    static boolean connectionClosed = false;

    // Thread for sending messages to clients
    static class ClientSendThread extends Thread{
        String userName;
        Socket socket;
    
        ClientSendThread(Socket sendMessageSocket, String clientName){
            socket = sendMessageSocket;
            userName = clientName;
        }

        // Message format for sending to server.
        String sendMessage(String input){
            String message = "";
            String username = "";
            String content = "";
            int idx = 0;
            while(input.charAt(idx) != ' '){
                if(input.charAt(idx) == '@'){
                    idx++;
                    continue;
                }
                username += input.charAt(idx);
                idx++;
            }
            content = input.substring(idx+1);
            message = "SEND " + username + "\n" + "Content-length: " + Integer.toString(content.length()) + "\n" + "\n" + content;
            return message;
        }
    
        public void run(){

            String message = "";
            try {
                PrintWriter out;
                BufferedReader stdIn ;
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String reply = "";

                while (true) {
                    // Reading message form terminal

                    System.out.print("> ");
                    out = new PrintWriter(socket.getOutputStream(), true);
                    stdIn = new BufferedReader(new InputStreamReader(System.in));
                    message = stdIn.readLine();
                    message = sendMessage(message);
                    out.println(message);

                    // Waiting for acknowledgement
                    while(true) {
                        if(connectionClosed){
                            socket.close();
                            return;
                        }
                        reply = in.readLine();
                        if(reply != null && reply.length() >= 4){
                            if(reply.substring(0, 4).equals("SEND")){
                                System.out.println(reply);
                                break;
                            }
                            else if(reply.substring(0, 9).equals("ERROR 103")){
                                System.out.println("Reconnect to send messages. Connection closed.");
                                connectionClosed = true;
                                forwardsSocket.close();
                                return;
                            }
                            else{
                                System.out.println(reply);
                                break;
                            }
                        }
                    }
                }

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
        }
    
        
    }

    // Thread to receive messages from other clients
    static class ClientReceiveThread extends Thread{
        String userName;
        Socket socket;
    
        ClientReceiveThread(Socket forwardsSocket, String clientName){
            socket =  forwardsSocket;
            userName = clientName;
        }
    
        public void run(){
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    String line;
                    
                    // Waiting for forwards from server 
                    while(true) {
                        if(connectionClosed){
                            socket.close();
                            return;
                        }
                        line = in.readLine();
                        if(line != null && line.length() != 0 && !line.equals(" ")){
                            String username = line.substring(8);
                            line = in.readLine();

                            int len = Integer.valueOf(line.substring(16));

                            line = in.readLine();
                            line = in.readLine();

                            // Sending acknowledgement to server
                            if(line.length() != len){
                                out.println("ERROR 103 Header Incomplete\n");
                            }
                            else{
                                out.println("RECEIVED " + username + "\n");
                                System.out.println("From " + username + ":" + line);
                                System.out.print("> ");
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                };
        }
    
        
    }

    public Client(InetAddress host, int port, String userName) {
        try {
            sendMessageSocket = new Socket(host, port);
        } catch (IOException e) {
            System.out.println(e);;
        }
    }

    public static void main(String[] args){

        String username = args[0];
        InetAddress server;

        // Connecting to server to receive and send messages.
        try {
            server = InetAddress.getByName(args[1]);
            try {
                // Register to reveive messages from other clients.
                forwardsSocket = new Socket(server, 8080);

                PrintWriter out = new PrintWriter(forwardsSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(forwardsSocket.getInputStream()));
                BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

                // Sending request to server
                out.println("REGISTER TORECV " + username + "\n \n");

                // Waiting for acknowledgement
                String line;
                while(true){
                    line = in.readLine();
                    if(line == null || line.length() <= 9){
                        continue;
                    }
                    else if(line.substring(0, 9).equals("ERROR 100")){
                        // Username malformed.
                        System.out.println(line);
                        System.out.println("Enter new username:");

                        stdIn = new BufferedReader(new InputStreamReader(System.in));
                        username = stdIn.readLine();
                        
                        // Again requesting to rgister with new username
                        out.println("REGISTER TORECV " + username + "\n \n");
                    }
                    else if(line.substring(0, 17).equals("REGISTERED TORECV")){
                        // Succesfully registered.
                        System.out.println("User Registered to Receive");
                        break;
                    }
                }

                // Register to send messages to other clients
                sendMessageSocket = new Socket(server, 8080);
                out = new PrintWriter(sendMessageSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(sendMessageSocket.getInputStream()));
                
                out.println("REGISTER TOSEND " + username + "\n \n");

                // waiting for acknowledgement
                while(true){
                    line = in.readLine();
                    if(line == null){
                        continue;
                    }
                    else if(line.substring(0, 9).equals("ERROR 100")){
                        // Already used username
                        System.out.println(line);
                        System.out.println("Enter new username:");

                        stdIn = new BufferedReader(new InputStreamReader(System.in));
                        username = stdIn.readLine();

                        // Requesting again with new username
                        out.println("REGISTER TOSEND " + username + "\n \n");
                    }
                    else if(line.substring(0, 17).equals("REGISTERED TOSEND")){
                        // Succesfully registered
                        System.out.println("User Registered to Send");
                        break;
                    }
                }

            } catch (IOException e) {
                System.out.println(e);;
            }
            // Openeing threads for sending and receiving messages
            ClientReceiveThread receiveThread = new Client.ClientReceiveThread(forwardsSocket, username);
            receiveThread.start();

            ClientSendThread sendThread = new Client.ClientSendThread(sendMessageSocket, username);
            sendThread.start();

        } catch (UnknownHostException e) {
            System.out.println(e);
        }

    }
}
