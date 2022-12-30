import java.io.*;
import java.net.*;
import  java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Scanner;

class ChatRoom{
    String roomName;
    InetAddress multicastAddress;
    int port;
    MulticastSocket socket;
    public ChatRoom(String roomName, int port) throws IOException, NoSuchAlgorithmException {
        this.roomName = roomName;
        this.multicastAddress = InetAddress.getByName(getMulticastAddress(roomName));
        System.out.println("Room address: "+ multicastAddress.toString());
        this.port = port;
        socket = new MulticastSocket(port);
        socket.joinGroup(multicastAddress);
    }
    public static String getMulticastAddress(String roomName) throws NoSuchAlgorithmException {
        MessageDigest sh= MessageDigest.getInstance("SHA-256");
        sh.update(roomName.getBytes());
        return "225."+ (128+sh.digest()[0]) + "."+(128+sh.digest()[1]) + "."+(128+sh.digest()[2]);
    }
}

class Sender extends Thread{
    ChatRoom chatRoom;
    BufferedReader inFromUser;
    MulticastSocket senderSocket;
    String userName;
    public Sender(ChatRoom chatRoom, BufferedReader inFromUser,String userName) throws IOException {
        this.chatRoom = chatRoom;
        this.inFromUser = inFromUser;
        senderSocket = chatRoom.socket;
        this.userName = userName;
    }

    public void run() {
        byte[] sendData;
        String sentence;

        while(true){
            try {
                sentence = inFromUser.readLine();
                if(sentence.equals("#EXIT")){
                    break;
                }
                sentence = userName + ": "+sentence;
            } catch (IOException e) {
                System.out.println("input error");
                continue;
            }

            // 청크 단위로 나누고 보내야함
            sendData = sentence.getBytes();
            for(int i=0; i <= (sendData.length-1)/512; i++ ){
                int from = i*512;
                int to = Math.min((i+1)*512,sendData.length);
                byte[] temp = Arrays.copyOfRange(sendData,from, to);
                DatagramPacket sendPacket = new DatagramPacket(
                        temp,
                        temp.length, chatRoom.multicastAddress, chatRoom.port);

                try {
                    senderSocket.send(sendPacket);
                } catch (IOException e) {
                    System.out.println("send error");
                    continue;
                }
            }

        }
    }
}
class Receiver extends Thread {
    ChatRoom chatRoom;
    MulticastSocket receiverSocket;

    public Receiver(ChatRoom chatRoom) throws IOException {
        this.chatRoom = chatRoom;
        receiverSocket = chatRoom.socket;
    }
    public void run(){
        while(true){
            byte[] buffer = new byte[512];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            try {
                receiverSocket.receive(receivePacket);
            } catch (IOException e) {
                System.out.println("Receiver error");
                continue;
            }
            byte[] receiveData = new byte[receivePacket.getLength()];
            System.arraycopy(receivePacket.getData(), receivePacket.getOffset(), receiveData, 0, receivePacket.getLength());
            System.out.println(new String(receiveData));
        }
    }
}

public class Peer {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InterruptedException {
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        int port = 3030;
        if(args.length>0){
            port = Integer.parseInt(args[0]);
        }


        System.out.println("port number: "+port);

        System.out.println("Welcome to Chat App!");
        System.out.println("command: #JOIN (room name) (user name)");
        System.out.print("Enter command: ");
        String command = inFromUser.readLine();
        String[] commandList = command.split(" ");
        String roomName = commandList[1];

        InetAddress	localAddress = InetAddress.getLocalHost();
        ChatRoom chatRoom = new ChatRoom(roomName, port);

        String userName = commandList[2];

        Thread receiver = new Receiver(chatRoom);
        Thread sender = new Sender(chatRoom, inFromUser, userName);
        receiver.start();
        sender.start();
        sender.join();
        System.out.println("end");

    }
}
