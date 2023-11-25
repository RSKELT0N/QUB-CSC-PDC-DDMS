package core.peer;

import core.Lib;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketTimeoutException;

class Receiver extends Runner
{
    Receiver(Peer receive_peer)
    {
        super();
        this.m_receiver_peer = receive_peer;
        this.m_receiver = receive_peer.GetSocket();
    }

    @Override
    public void run()
    {
        while(this.m_running)
        {
            try {
                DatagramPacket packet = this.m_receiver.ReceivePacket();
                Lib.Pair<String, Integer> conn = new Lib.Pair<>(packet.getAddress().getHostAddress(), packet.getPort());
                this.m_receiver_peer.AddReceiveItem(conn, packet.getData());

            } catch (SocketTimeoutException e) {
                continue;
            } catch (IOException | RuntimeException | InterruptedException e) {
                break;
            }
        }
    }

    private core.peer.Node m_receiver;
    private Peer m_receiver_peer;
}
