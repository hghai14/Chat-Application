import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;


public class Server{
    ServerSocket serverSoc;
    ArrayList<Socket> active_sockets = new ArrayList<>(); 
    static Hashtable<String, Socket> registered_users = new Hashtable<>();

    static class ServerThread extends Thread{
        Socket socket;
        String clientName;
        ServerThread(Socket soc) {
            socket = soc;
        }

        public void run(){
            
            BufferedReader in;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                while(true){
                    String message = in.readLine();
                    if(message != null) {
                        System.out.println(message);
                        if(message.length() == 0 || message.equals(" ")) continue;

                        if(message.substring(0, 15).equals("REGISTER TORECV")){
                            if(validUserName(message.substring(16)) && registered_users.get(message.substring(16)) == null){
                                registered_users.put(message.substring(16), socket);
                                out.println("REGISTERED TORECV " + message.substring(16) + "\n \n");
                                return;
                            }
                            else if(registered_users.get(message.substring(16)) != null){
                                out.println("ERROR 101");
                            }
                            else{
                                out.println("ERROR 100");
                            }
                            
                        }
                        else if(message.substring(0, 15).equals("REGISTER TOSEND")){
                            if(registered_users.get(message.substring(16)) != null){
                                clientName = message.substring(16);
                                out.println("REGISTERED TOSEND " + message.substring(16) + "\n \n");
                            }
                            else{
                                out.println("ERROR 101");
                                continue;
                            }
                            String line;
                            while(true){ 
                                line = in.readLine();  
                                if(line != null) {
                                    if(line.length()>4 && line.substring(0, 4).equals("SEND")){
                                        if(line.substring(5).equals("ALL")){
                                            Enumeration<Socket> receipents = registered_users.elements();
                                            
                                            continue;
                                        }
                                        Socket receiverClientSocket = registered_users.get(line.substring(5));
                                        line = in.readLine();

                                        int len = Integer.valueOf(line.substring(16));

                                        line = in.readLine();
                                        line = in.readLine();
                                        
                                        // content length mismatch error

                                        String forward_message = "FORWARD " + clientName + "\n" + "Content-Length: " + Integer.toString(len) + "\n" +"\n" + line;
                                        PrintWriter receiverOut = new  PrintWriter(receiverClientSocket.getOutputStream(), true);
                                        receiverOut.println(forward_message);
                                    }
                                }
                            }
                        }

                    } 
                }
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
    }

    public Server(){
        try {
            serverSoc = new ServerSocket(8080);
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public static boolean validUserName(String username) {
        if(username != null && username.matches("^[0-9a-zA-Z]*$")){
            return true;
        }
        return false;
    }

    void startServer() {
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