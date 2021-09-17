import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;


public class Server{
    // Server socket listening for connections
    ServerSocket serverSoc;

    // Keeping a list of active sockets
    ArrayList<Socket> active_sockets = new ArrayList<>(); 

    // TO store registered user and sockets thorugh which they receive messages
    static Hashtable<String, Socket> registered_users = new Hashtable<>();

    // Thread for each connection
    static class ServerThread extends Thread{
        // Socket of connection at server
        Socket socket;
        // CLient registered for this socket
        String clientName;


        ServerThread(Socket soc) {
            socket = soc;
        }

        // Thread process
        public void run(){
            
            BufferedReader in;
            PrintWriter out;

            try {

                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                String message;

                while(true){
                    message = in.readLine();

                    if(message != null) {

                        if(message.length() == 0 || message.equals(" ")) continue;

                        // Register client to receive messages
                        if(message.length() >= 15 && message.substring(0, 15).equals("REGISTER TORECV")){
                            if(validUserName(message.substring(16)) && registered_users.get(message.substring(16)) == null){

                                // Valid username, adding socket ot hastable
                                registered_users.put(message.substring(16), socket);

                                // Reply to client 
                                out.println("REGISTERED TORECV " + message.substring(16) + "\n \n");

                                return;
                            }
                            else if(!validUserName(message.substring(0, 16))){
                                // Reply to client on unvalid username
                                out.println("ERROR 100 Malformed username\n \n");
                            }
                            
                        }
                        // Register client to send messages
                        else if(message.substring(0, 15).equals("REGISTER TOSEND")){
                            // If registered to receive messages then
                            if(registered_users.get(message.substring(16)) != null){

                                // Registering client to send messages
                                clientName = message.substring(16);

                                out.println("REGISTERED TOSEND " + message.substring(16) + "\n \n");
                            }
                            else{
                                out.println("ERROR 100 Malformed username");
                                continue;
                            }

                            // Thread open to receive messages from client to forward it to other users.
                            String line;
                            while(true){ 
                                line = in.readLine();  
                                if(line != null && line.length()!=0 && !line.equals(" ")) {
                                    if(line.length()>4 && line.substring(0, 4).equals("SEND")){
                                        // Breadcast Messages
                                        if(line.substring(5).equals("ALL")){

                                            line = in.readLine();
                                            // Header packet error
                                            if(!line.matches("^Content-length: [0-9]*$")){
                                                out.println("ERROR 103 Header Incomplete\n \n");
                                                // Closing both sockets of current client
                                                Socket forwardsSocket = registered_users.get(clientName);
                                                forwardsSocket.close();
                                                registered_users.remove(clientName);
                                                socket.close();
                                                return;
                                            }
                                            int len = Integer.valueOf(line.substring(16));

                                            line = in.readLine();
                                            line = in.readLine();
                                            String content = line;

                                            // Header Packet Error, closing connections with client
                                            if(content.length() != len){
                                                out.println("ERROR 103 Header Incomplete\n \n");

                                                // Closing both sockets of current client
                                                Socket forwardsSocket = registered_users.get(clientName);
                                                forwardsSocket.close();
                                                registered_users.remove(clientName);
                                                socket.close();
                                                return;
                                            }

                                            // Broadcasting message and checking if success
                                            boolean result = broadcastMessage(content);

                                            // Acknowledging the sender
                                            if(result) {
                                                out.println("SEND ALL\n");
                                            }
                                            else{
                                                out.println("ERROR 102 Unable to send\n \n");
                                            }

                                            continue;
                                        }

                                        // Unicast Messages
                                        String receiver = line.substring(5);

                                        line = in.readLine();

                                        // Header packet error
                                        if(!line.matches("Content-length: ([0-9]*)")){
                                            out.println("ERROR 103 Header Incomplete\n \n");

                                            // Closing both sockets of current client
                                            Socket forwardsSocket = registered_users.get(clientName);
                                            forwardsSocket.close();
                                            registered_users.remove(clientName);
                                            socket.close();
                                            return;
                                        }
                                        int len = Integer.valueOf(line.substring(16));

                                        line = in.readLine();
                                        line = in.readLine();
                                        String content = line;

                                        // Header Packet Error, closing connections with client
                                        if(content.length() != len){
                                            out.println("ERROR 103 Header Incomplete\n \n");

                                            // Closing both sockets of current client
                                            Socket forwardsSocket = registered_users.get(clientName);
                                            forwardsSocket.close();
                                            registered_users.remove(clientName);
                                            socket.close();
                                            return;
                                        }

                                        boolean result = unicastMessage(line, receiver);

                                        // Acknowledging the sender
                                        if(result) {
                                            out.println("SEND " + receiver + "\n");
                                        }
                                        else{
                                            out.println("ERROR 102\n");
                                        }

                                    }
                                    else if(!line.matches("SEND ([A-Za-z0-9]*)")){
                                        out.println("ERROR 103 Header Incomplete\n \n");

                                        // Closing both sockets of current client
                                        Socket forwardsSocket = registered_users.get(clientName);
                                        forwardsSocket.close();
                                        registered_users.remove(clientName);
                                        socket.close();
                                        return;
                                    }
                                }
                            }
                        }
                        else{
                            out.println("ERROR 101 No user registered\n \n");
                        }

                    } 
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        // Broadcasting message using stop and wait
        private boolean broadcastMessage(String content) {

            int len = content.length();
            String forwardMessage = "FORWARD " + clientName + "\n" + "Content-Length: " + Integer.toString(len) + "\n" +"\n" + content;
            
            PrintWriter receiverOut;
            boolean success = true;

            for (Map.Entry<String, Socket> e : registered_users.entrySet()){

                if(!e.getKey().equals(clientName)){
                    Socket currClient = e.getValue();

                    try {
                        receiverOut = new PrintWriter(currClient.getOutputStream(), true);
                        receiverOut.println(forwardMessage);

                        BufferedReader in = new BufferedReader(new InputStreamReader(currClient.getInputStream()));
                        String reply = in.readLine();

                        // Waiting for acknowledgement
                        while(true) {
                            if(reply == null || reply.length() < 4 || reply.equals(" ")){
                                continue;
                            }
                            if(reply.substring(0, 8).equals("RECEIVED")) {
                                break;
                            }
                            else if(reply.substring(0, 9).equals("ERROR 103")){
                                success = false;
                                break;
                            }

                        }
                        if(!success){
                            break;
                        }

                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }
            }
            if(success) {
                return true;
            }
            else{
                return false;
            }
        }

        // Forwarding message to single receiver
        private boolean unicastMessage(String content, String recipient){
            Socket receiverClientSocket = registered_users.get(recipient);

            if(receiverClientSocket == null) {
                return false;
            }

            int len = content.length();
            String forward_message = "FORWARD " + clientName + "\n" + "Content-Length: " + Integer.toString(len) + "\n" +"\n" + content;

            PrintWriter receiverOut;

            try {
                receiverOut = new  PrintWriter(receiverClientSocket.getOutputStream(), true);
                receiverOut.println(forward_message);
                boolean success = true;

                while (true){
                    BufferedReader in = new BufferedReader(new InputStreamReader(receiverClientSocket.getInputStream()));
                    String reply = in.readLine();

                    // Waiting for acknowledgement
                    while(true) {
                        if(reply == null || reply.length() < 4 || reply.equals(" ")){
                            continue;
                        }
                        if(reply.substring(0, 8).equals("RECEIVED")) {
                            break;
                        }
                        else if(reply.substring(0, 9).equals("ERROR 103")){
                            success = false;
                            break;
                        }
                    }

                    if(success) {
                        return true;
                    }
                    else{
                        return false;
                    }
                }

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return true;
        }
    }

    // Server constructor
    public Server(){
        try {
            serverSoc = new ServerSocket(8080);
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    // Validity of username
    public static boolean validUserName(String username) {
        if(username != null && username.matches("^[0-9a-zA-Z]*$")){
            return true;
        }
        return false;
    }

    void startServer() {
        // Listens to connections and open new thread and sockets 
        while(true) {
            try {
                Socket new_socket = this.serverSoc.accept();
                active_sockets.add(new_socket);
                ServerThread new_thread = new ServerThread(new_socket);
                new_thread.start();
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }
    public static void main(String[] args){
        Server host = new Server();
        host.startServer();
    } 
}