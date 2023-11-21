package core.peer;

import java.io.IOException;
import java.net.*;

public class Node
{
    public Node(int port) throws SocketException, UnknownHostException
    {
        this.SetPort(port);
        this.m_ip_address = InetAddress.getLocalHost().getHostAddress();
        this.m_socket = new DatagramSocket(this.m_port);
        m_socket.setSoTimeout(1000);
    }

    public DatagramPacket ReceivePacket() throws IOException
    {
        DatagramPacket receive = DefinePacket(MAX_RECEIVE_SIZE);
        m_socket.receive(receive);

        return receive;
    }

    public void SendPacket(String input, String ip_address, int port) throws IOException
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

    private DatagramPacket DefinePacket(String input, String ip_address, int port) throws UnknownHostException
    {
        assert !(input.isEmpty());

        byte[] bytes = input.getBytes();
        InetAddress address = InetAddress.getByName(ip_address);
        return new DatagramPacket(bytes,input.length(), address, port);
    }

    private void SetPort(int port)
    {
        assert (port >= Math.pow(2, 10) && port <= (Math.pow(2, 16) - 1));
        this.m_port = port;
    }

    public int m_port;
    public String m_ip_address;
    public DatagramSocket m_socket;
    private final int MAX_RECEIVE_SIZE = (int) Math.pow(2, 8);
}
