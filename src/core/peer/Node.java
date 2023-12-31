package core.peer;

import java.io.IOException;
import java.net.*;

public class Node
{
    public Node(int port) throws IOException
    {
        try {
            this.m_socket = new DatagramSocket(DEFAULT_PORT);
        } catch (SocketException se) {

            if(port != DEFAULT_PORT)
                this.m_socket = new DatagramSocket(port);
            else this.m_socket = new DatagramSocket(RANDOM_PORT);
        }

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
    public final int DEFAULT_PORT = 52222;
    public final int MAX_RECEIVE_SIZE = 1<<16;
    private final int RANDOM_PORT = 0;
}
