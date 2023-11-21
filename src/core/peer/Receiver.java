package core.peer;

import core.Lib;

import java.io.IOException;
import java.net.DatagramPacket;

class Receiver implements Runnable
{
    Receiver(Peer receive_peer)
    {
        SetState(true);
        this.m_receiver_peer = receive_peer;
        this.m_receiver = receive_peer.GetSocket();
    }

    public void SetState(boolean state)
    {
        this.m_running = state;
    }

    @Override
    public void run()
    {
        while(this.m_running)
        {
            try {
                DatagramPacket packet = this.m_receiver.ReceivePacket();
                Lib.Pair<String, Integer> conn = new Lib.Pair<>(packet.getAddress().getHostAddress(), packet.getPort()); // TODO: Check host address with local to string
                this.m_receiver_peer.AddReceiveItem(conn, Lib.FormatBytes(packet.getData(), packet.getLength()));

            } catch (IOException | InterruptedException | RuntimeException e) {
                break;
            }
        }
    }

    private boolean m_running;
    private core.peer.Node m_receiver;
    private Peer m_receiver_peer;
}
