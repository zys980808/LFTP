import java.net.*;
import java.io.*;
import java.util.*;

public class Client {
    final static int COMMAND_GET = 0;
    final static int COMMAND_SEND = 1;
    

    static final int SERVER_PORT = 6060;
    static final int START_PORT = 8000; // 寻找可用端口的起始端口
    public static void main(String[] args){
        String commandStr = null, serverName = null, fileName = null;
        int command = -1;
        DatagramSocket socket;  // the udp socket
        DatagramPacket packet;  // the packet of udp socket
        int port; // the port of socket
    
        InetAddress serverAddr = null;
        try{
            commandStr = args[0];
            serverName = args[1];
            fileName = args[2];
        } catch(Exception e){
            Utils.sendUsage();
            Utils.sendError("Invalid arguments");
        }

        /* 读取指令 */
        if(commandStr.equals("lsend")){
            command = COMMAND_SEND;
        } else if(commandStr.equals("lget")){
            command = COMMAND_GET;
        } else{
            Utils.sendUsage();
            Utils.sendError("No such command");
        }

        /* 地址解析 */
        try{
            serverAddr = InetAddress.getByName(serverName);
        } catch(Exception e){
            Utils.sendError("Invalid server name or address");
        }

        /* 获取可用端口 */
        port = START_PORT;
        while(true){
            try{
                socket = new DatagramSocket(port);
            }
            catch(Exception e){  
                port++;
                continue;
            }
            break;
        }

        /* (上传文件)检查文件是否可读 */
        if(command == COMMAND_SEND){
            File tempFile = new File(fileName);
            if(!tempFile.exists()){
                Utils.sendError("File doesn't exist");
            }
        }

        
        /* 向服务器发送请求 - fisrt handshake (需要设置定时器) */
        
        byte[] requestData = new byte[Utils.HEADER_DATASIZE + Utils.HEADER_DATA];
        Utils.SetCommand(requestData, (command == COMMAND_GET)); //分别用b11和b111代表GET和SEND
        Utils.SetSeq(requestData, port*10); //初始Seq#.设置为client_port*10
        Utils.SetDataSize(requestData, fileName.length());
        Utils.SetData(requestData, fileName.getBytes());
        packet = new DatagramPacket(requestData, requestData.length, serverAddr, SERVER_PORT);

        try{socket.send(packet);}
        catch(Exception e){
            Utils.sendError("Unknown socket error.");
        }

        /* 接收服务器回传的请求 - second handshake */
        packet.setLength(128);
        try{socket.receive(packet);}
        catch(Exception e){
            Utils.sendError("Unknown socket error.");
        }
        int transPort = packet.getPort();
        

        /* 客户端再次确认 - third handshake */
        packet = new DatagramPacket("200".getBytes(), 128, serverAddr, transPort);
        try{socket.send(packet);}
        catch(Exception e){
            Utils.sendError("Unknown socket error.");
        }

        /* 连接确认，根据指令设置handler并进行新开线程进行处理 （需要设置定时器） */
        Thread thread;
        if(command == COMMAND_GET){
            thread = new Thread(new ReceiveHandler(socket, fileName));
        }
        else{
            thread = new Thread(new SendHandler(socket, fileName));
        }
        thread.start();
    }
}