package core.peer;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;

public class Node
{
    public Node(int port) throws IOException
    {
//        String command = "curl -X GET http://xml.purplepixie.org/apps/ipaddress/?format=plain";
//        ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
//        Process process = processBuilder.start();
//        InputStream ip = process.getInputStream();
//        String ip_str = new String(ip.readAllBytes());

        this.m_socket = new DatagramSocket(port);
        this.m_ip_address = InetAddress.getLocalHost().getHostAddress();
        this.m_port = m_socket.getLocalPort();
        m_socket.setSoTimeout(1000);
    }

    public DatagramPacket ReceivePacket() throws IOException
    {
        DatagramPacket receive = DefinePacket(MAX_RECEIVE_SIZE);
        m_socket.receive(receive);

        return receive;
    }

    public void SendPacket(byte[] input, String ip_address, int port) throws IOException
    {
        DatagramPacket packet = DefinePacket(input, ip_address, port);
        m_socket.send(packet);
    }

    private DatagramPacket DefinePacket(int size)
    {
        assert size > 0;

        byte[] bytes = new byte[size];
        return new DatagramPacket(bytes, 0, size);
    }

    private DatagramPacket DefinePacket(byte[] input, String ip_address, int port) throws UnknownHostException
    {
        assert input.length != 0;

        InetAddress address = InetAddress.getByName(ip_address);
        return new DatagramPacket(input, input.length, address, port);
    }

    public int m_port;
    public String m_ip_address;
    public DatagramSocket m_socket;
    private final int MAX_RECEIVE_SIZE = 1<<16;
}
