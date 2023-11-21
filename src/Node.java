import java.io.IOException;
import java.net.*;

public class Node
{
    enum SStatus
    {
        INVALID,
        VALID
    }

    public Node(int port) throws SocketException, UnknownHostException
    {
        this.SetPort(port);
        this.m_ip_address = Inet4Address.getLocalHost().getHostAddress();
        this.m_socket = new DatagramSocket(this.m_port);
    }

    public void SendPacket(String input) throws IOException
    {
        DatagramPacket packet = DefinePacket(input);
        m_socket.send(packet);
    }

    private DatagramPacket DefinePacket(String input)
    {
        assert !(input.isEmpty());

        byte[] bytes = input.getBytes();
        return new DatagramPacket(bytes, 0, input.length() - 1);
    }

    private void SetPort(int port)
    {
        assert (port >= 0 && port <= (2^16 - 1));
        this.m_port = port;
    }

    public int m_port;
    public String m_ip_address;
    public DatagramSocket m_socket;
}
