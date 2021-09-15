import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server{
    ServerSocket serverSoc;

    public Server(){
        try {
            serverSoc = new ServerSocket(8080);
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    void startServer() {
        while(true) {
            try {
                Socket new_socket = this.serverSoc.accept();
                DataInputStream in = new DataInputStream(new BufferedInputStream(new_socket.getInputStream()));
                System.out.println(in.readUTF());

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