package core.peer;

import core.Lib;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Peer
{
    static public class RoutingTableEntry implements Serializable
    {
        public String nick_name;
        public BigInteger id;
        public int port;
        public String ip_address;

        public RoutingTableEntry(String nick_name, BigInteger id, String ip_address, int port)
        {
            this.nick_name = nick_name;
            this.id = id;
            this.ip_address = ip_address;
            this.port = port;
        }

        public RoutingTableEntry(String ip_address, int port)
        {
            this.nick_name = "";
            this.nick_name = nick_name;
            this.id = BigInteger.valueOf(0);
            this.ip_address = ip_address;
            this.port = port;
        }

        @Override
        public boolean equals(Object in)
        {
            if (!(in instanceof RoutingTableEntry)) {
                return false;
            }

            RoutingTableEntry e = (RoutingTableEntry)in;
            return this.id.equals(e.id) &&
                   this.port == e.port &&
                   this.ip_address.equals(e.ip_address);
        }
    }

    public Peer(String nickname, int port) throws IOException, NoSuchAlgorithmException
    {
        DefineUDPSocket(port);
        Initialise(nickname);
    }

    private void Initialise(String nickname) throws NoSuchAlgorithmException
    {
        this.m_connected = false;
        this.m_nickname = nickname;
        this.m_processing = new Semaphore(0);
        this.m_received = new LinkedBlockingQueue<>();
        this.m_m_bits = 8;
        this.m_alpha = (int)Math.sqrt(m_m_bits);

        String hash_value = m_socket.m_ip_address + ":" + m_socket.m_port;
        this.m_id = Lib.SHA1(hash_value, BigInteger.valueOf(1).shiftLeft(m_m_bits));

        DefineRoutingTable();
        DefineDataTable();
        DefinePingVector();
    }

    public void DefineSenderAndReceiver()
    {
        this.m_sender = new Sender(this, this.GetSocket());
        this.m_receiver = new Receiver(this);
        this.m_heartbeat = new Heartbeat(this, m_heartbeat_interval);

        new Thread(this.m_sender).start();
        new Thread(this.m_receiver).start();
        new Thread(this.m_heartbeat).start();
    }

    public void Store(RoutingTableEntry peer_info, byte[] message) throws NoSuchAlgorithmException, InterruptedException
    {
        String[] message_string_tokens = new String(message, StandardCharsets.UTF_8).replace("\0", "").split(" ");
        assert message_string_tokens.length == 2;

        String[] key_value_tokens = message_string_tokens[1].split(",");

        if(key_value_tokens.length == 1)
        {
            String key = key_value_tokens[0];
            BigInteger command_count = new BigInteger(message_string_tokens[0].split(":")[1]);
            BigInteger key_hash = Lib.SHA1(key, BigInteger.valueOf(1).shiftLeft(m_m_bits));

            if(m_data_table.containsKey(key_hash))
                Send(peer_info.ip_address, peer_info.port, ("STORE" + ":" + command_count + " " + key + "," + m_data_table.get(key_hash).second).getBytes(), false);
        }
        else if(key_value_tokens.length == 2)
        {

            String key = key_value_tokens[0];
            BigInteger key_hash = Lib.SHA1(key, BigInteger.valueOf(1).shiftLeft(m_m_bits));
            String value = new String(message, StandardCharsets.UTF_8).replace("\0", "").split(",")[1];

            if(!m_data_table.containsKey(key_hash))
            {
                m_data_table.put(key_hash, new Lib.Pair<>(key, value));
                m_data_keys.put(key_hash, key);
            }
        }
    }

    public void Ping(RoutingTableEntry peer_info, byte[] message) throws InterruptedException
    {
        String[] message_string_tokens = new String(message, StandardCharsets.UTF_8).replace("\0", "").split(" ");
        assert message_string_tokens.length == 2;

        String[] ping_tokens = message_string_tokens[1].split(",");
        String nick_name = ping_tokens[0];
        BigInteger id = new BigInteger(ping_tokens[1]);

        InsertPeerIntoRoutingTable(nick_name, id, peer_info.ip_address, peer_info.port);
        SendPong(peer_info.ip_address, peer_info.port);
    }

    public void Pong(RoutingTableEntry peer_info, byte[] message)
    {
        String[] message_string_tokens = new String(message, StandardCharsets.UTF_8).replaceAll("\0", "").split(" ");
        assert message_string_tokens.length == 2;

        String[] ping_tokens = message_string_tokens[1].split(",");
        String nick_name = ping_tokens[0];
        BigInteger id = new BigInteger(ping_tokens[1]);

        InsertPeerIntoRoutingTable(nick_name, id, peer_info.ip_address, peer_info.port);
        SetPingStateForPeer(id, true);

        if(!m_connected)
        {
            this.m_bootstrapped_ip = peer_info.ip_address;
            this.m_bootstrapped_port = peer_info.port;
            this.m_connected = true;
        }
    }

    public void FindKeysRequest(RoutingTableEntry peer_info, byte[] message) throws IOException, InterruptedException
    {
        String[] message_string_tokens = new String(message, StandardCharsets.UTF_8).replaceAll("\0", "").split(" ");
        assert message_string_tokens.length == 1;

        BigInteger command_count = new BigInteger(message_string_tokens[0].split(":")[1]);

        StringBuilder keys = new StringBuilder();

        var data = m_data_table.firstEntry();
        for(int i = 0; i < m_data_table.size(); i++)
        {
            keys.append(data.getValue().first);
            data = m_data_table.higherEntry(data.getKey());

            if(i != m_data_table.size() - 1)
                keys.append(",");
        }

        Send(peer_info.ip_address, peer_info.port, ("FIND_KEYS_RESPONSE" + ":" + command_count + " " + keys).getBytes(), false);
    }

    public void FindKeysResponse(RoutingTableEntry peer_info, byte[] message) throws NoSuchAlgorithmException
    {
        String[] message_string_tokens = new String(message, StandardCharsets.UTF_8).replaceAll("\0", "").split(" ");
        assert message_string_tokens.length == 2;

        if(message_string_tokens.length == 2)
        {
            String[] keys = message_string_tokens[1].split(",");

            for(var key : keys)
            {
                BigInteger hash = Lib.SHA1(key, BigInteger.valueOf(1).shiftLeft(m_m_bits));

                m_data_keys.put(hash, key);
            }
        }
    }

    public void FindNodeRequest(RoutingTableEntry peer_info, byte[] message) throws IOException, InterruptedException
    {
        String[] message_string_tokens = new String(message, StandardCharsets.UTF_8).replaceAll("\0", "").split(" ");
        assert message_string_tokens.length == 2;

        BigInteger command_count = new BigInteger(message_string_tokens[0].split(":")[1]);
        BigInteger peer_id = new BigInteger(message_string_tokens[1]);

        RoutingTableEntry[] close_peers = GetClosePeers(peer_id, m_m_bits);

        byte[] initial_command = ("FIND_NODE_RESPONSE" + ":" + command_count + " ").getBytes();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(out);

        oos.writeObject(close_peers);
        oos.flush();

        byte[] to_send = new byte[initial_command.length + out.size()];
        ByteBuffer buff = ByteBuffer.wrap(to_send);

        buff.put(initial_command);
        buff.put(out.toByteArray());
        byte[] combined = buff.array();

        Send(peer_info.ip_address, peer_info.port, combined, false);
    }

    public void FindNodeResponse(RoutingTableEntry peer_info, byte[] message) throws IOException, ClassNotFoundException, InterruptedException
    {
        String[] message_string_tokens = new String(message, StandardCharsets.UTF_8).replaceAll("\0", "").split(" ");
        BigInteger command_count = new BigInteger(message_string_tokens[0].split(":")[1]);

        ByteArrayInputStream in = new ByteArrayInputStream(Arrays.copyOfRange(message, message_string_tokens[0].length() + 1, message.length));
        ObjectInputStream iis = new ObjectInputStream(in);

        SendPing(peer_info.ip_address, peer_info.port);
        RoutingTableEntry[] peers = (RoutingTableEntry[]) iis.readObject();

        for(var peer : peers)
        {
            BigInteger bucket = DetermineBucket(peer.id);

            if(!(m_routing_table.get(bucket).containsKey(peer.id)))
            {
                if(!Objects.equals(peer.id, m_id))
                {
                    SendPing(peer.ip_address, peer.port);
                    Send(peer.ip_address, peer.port, ("FIND_NODE_REQUEST" + ":" + command_count + " " + m_id).getBytes(), true);
                }
            }
        }
    }

    public void FindValueRequest(RoutingTableEntry peer_info, byte[] message) throws IOException, InterruptedException
    {
        String[] message_string_tokens = new String(message, StandardCharsets.UTF_8).replaceAll("\0", "").split(" ");
        assert message_string_tokens.length == 2;

        BigInteger command_count = new BigInteger(message_string_tokens[0].split(":")[1]);
        BigInteger key_id = new BigInteger(message_string_tokens[1]);
        RoutingTableEntry[] close_peers = GetClosePeers(key_id, m_m_bits);

        byte[] initial_command = ("FIND_VALUE_RESPONSE" + ":" + command_count + " ").getBytes();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(out);

        oos.writeObject(close_peers);
        oos.flush();

        byte[] to_send = new byte[initial_command.length + out.size()];
        ByteBuffer buff = ByteBuffer.wrap(to_send);

        buff.put(initial_command);
        buff.put(out.toByteArray());
        byte[] combined = buff.array();

        Send(peer_info.ip_address, peer_info.port, combined, false);
    }

    public void FindValueResponse(RoutingTableEntry peer_info, byte[] message) throws IOException, InterruptedException, ClassNotFoundException
    {
        String[] message_string_tokens = new String(message, StandardCharsets.UTF_8).replaceAll("\0", "").split(" ");
        BigInteger command_count = new BigInteger(message_string_tokens[0].split(":")[1]);

        ByteArrayInputStream in = new ByteArrayInputStream(Arrays.copyOfRange(message, message_string_tokens[0].length() + 1, message.length));
        ObjectInputStream iis = new ObjectInputStream(in);

        SendPing(peer_info.ip_address, peer_info.port);
        RoutingTableEntry[] peers = (RoutingTableEntry[]) iis.readObject();

        for(var peer : peers)
        {
            BigInteger bucket = DetermineBucket(peer.id);

            if(!(m_routing_table.get(bucket).containsKey(peer.id)))
            {
                if(!Objects.equals(peer.id, m_id))
                {
                    SendPing(peer.ip_address, peer.port);
                    Send(peer.ip_address, peer.port, ("FIND_VALUE_REQUEST" + ":" + command_count + " " + m_id).getBytes(), true);
                }
            }
        }
    }

    public void JoinThroughPeer(String boot_ip, int boot_port) throws InterruptedException
    {
        Send(boot_ip, boot_port, (FormatCommand("FIND_NODE_REQUEST") + " " + m_id).getBytes(), true);
    }

    public void JoinThroughBroadcast() throws IOException, InterruptedException
    {
        byte[] broadcast_join_message = (FormatCommand("FIND_NODE_REQUEST") + " " + m_id).getBytes();
        m_sender.AddSendItem(null, broadcast_join_message);
    }

    public void SendFindNode(BigInteger id) throws InterruptedException
    {
        RoutingTableEntry[] close_peers_to_key = GetClosePeers(id, m_alpha);

        for(var peer : close_peers_to_key)
            Send(peer.ip_address, peer.port, (FormatCommand("FIND_NODE_REQUEST") + " " + id).getBytes(), true);
    }

    public void SendFindValue(BigInteger id) throws InterruptedException
    {
        RoutingTableEntry[] close_peers_to_key = GetClosePeers(id, m_alpha);

        for(var peer : close_peers_to_key)
            Send(peer.ip_address, peer.port, (FormatCommand("FIND_VALUE_REQUEST") + " " + id).getBytes(), true);
    }

    public void SendPing(String ip, int port) throws InterruptedException
    {
        Send(ip, port, (FormatCommand("PING") + " " + this.m_nickname + "," + this.m_id).getBytes(), false);
    }

    public void SendPong(String ip, int port) throws InterruptedException
    {
        Send(ip, port, (FormatCommand("PONG") + " " + this.m_nickname + "," + this.m_id).getBytes(), false);
    }

    public void ContactAllBuckets() throws InterruptedException
    {
        for(int i = 0; i < m_m_bits; i++)
        {
            BigInteger bucket = BigInteger.valueOf(1).shiftLeft(i);
            SendFindNode(bucket);
        }
    }

    public void GetDataItem(String data_id) throws NoSuchAlgorithmException, InterruptedException
    {
        BigInteger key = Lib.SHA1(data_id, BigInteger.valueOf(1).shiftLeft(m_m_bits));

        Optional<String> value;

        if(!m_data_table.containsKey(key))
            value = Optional.empty();
        else value = m_data_table.get(key).second.describeConstable();

        if(value.isEmpty())
        {
            SendFindNode(key);

            RoutingTableEntry[] closest_peers_to_key = GetClosePeers(key, m_alpha);

            for(var peer : closest_peers_to_key)
                Send(peer.ip_address, peer.port, (FormatCommand("STORE") + " " + data_id).getBytes(), false);
        }
    }

    public String FormatCommand(String command)
    {
        BigInteger curr_command = m_sender.GetAndIncrementSendCount();
        return (command + ":" + curr_command);
    }

    public void Close()
    {
        this.m_sender.SetState(false);
        this.m_receiver.SetState(false);
        this.m_heartbeat.SetState(false);
        this.m_socket.m_socket.close();
    }

    public void AddDataItem(String key, String value) throws NoSuchAlgorithmException, InterruptedException
    {
        BigInteger data_key = Lib.SHA1(key, BigInteger.valueOf(1).shiftLeft(m_m_bits));
        this.m_data_table.put(data_key, new Lib.Pair<>(key, value));
        this.m_data_keys.put(data_key, key);

        RoutingTableEntry[] close_peers = GetClosePeers(data_key, m_m_bits);
        byte[] to_send = (FormatCommand("STORE") + " " + key + "," + value).getBytes();

        for(var peer : close_peers)
            Send(peer.ip_address, peer.port, to_send, false);
    }

    public Lib.Pair<RoutingTableEntry, byte[]> GetNextReceived() throws InterruptedException
    {
        return this.m_received.poll(1000, TimeUnit.MILLISECONDS);
    }

    public final core.peer.Node GetSocket()
    {
        return this.m_socket;
    }

    public Lib.Pair<Semaphore, Receiver.Packet> Send(String remote_ip, int remote_port, byte[] message, boolean lock) throws InterruptedException
    {
        this.m_sender.AddSendItem(new RoutingTableEntry(remote_ip, remote_port), message);
        var packet = this.m_sender.CreateReceivedItem(message);

        if(lock)
            packet.first.tryAcquire(10, TimeUnit.SECONDS);

        return packet;
    }

    public void PrintRoutingTable()
    {
        System.out.println("Peer (" + m_nickname + ":" + m_id + ") routing table\n---------------");
        for(var bucket : m_routing_table.entrySet())
        {
            var bucket_values = bucket.getValue();

            System.out.print("(" + bucket.getKey() + ") [" + bucket_values.size() + " " + "Peers] |");
            for(var peer : bucket_values.entrySet())
            {
                System.out.print(" " + "(" + peer.getValue().nick_name + ":" + peer.getKey() + ")");
            }
            System.out.println();
        }
        System.out.println("---------------");
    }

    public void PrintDataTable()
    {
        System.out.println("Peer (" + m_nickname + ":" + m_id + ") data table\n---------------");

        if(m_data_table.isEmpty())
        {
            System.out.println("No data entries");
            return;
        }

        for(var data : m_data_table.entrySet())
        {
            System.out.println(data.getKey() + "(" + data.getValue().first + ")" + " | " + data.getValue().second);
        }
        System.out.println("---------------");
    }

    public void PrintIndexTable()
    {
        System.out.println("Peer (" + m_nickname + ":" + m_id + ") index table\n---------------");

        if(m_data_keys.isEmpty())
        {
            System.out.println("No index entries");
            return;
        }

        for(var data : m_data_keys.entrySet())
        {
            System.out.println(data.getKey() + "(" + data.getValue() + ")");
        }
        System.out.println("---------------");
    }

    public void AddReceiveItem(RoutingTableEntry r, byte[] s) throws InterruptedException
    {
        this.m_received.put(new Lib.Pair<>(r, s));
    }

    public void SetPingStateForPeer(BigInteger peer_id, boolean state)
    {
        assert m_ping_vector.containsKey(peer_id);
        this.m_ping_vector.put(peer_id, state);
    }

    public final boolean GetPingStateForPeer(BigInteger peer_id)
    {
        assert m_ping_vector.containsKey(peer_id);
        return this.m_ping_vector.get(peer_id);
    }

    public final void RemovePeerFromRoutingTable(BigInteger peer_id)
    {
        BigInteger bucket = DetermineBucket(peer_id);
        assert this.m_routing_table.containsKey(peer_id) && this.m_ping_vector.containsKey(peer_id);

        this.m_routing_table.get(bucket).remove(peer_id);
        this.m_ping_vector.remove(peer_id);
    }

    public RoutingTableEntry[] GetClosePeers(BigInteger peer_id, int amt)
    {
        BigInteger total = GetTotalPeersInRoutingTable(peer_id);
        boolean take_all = total.compareTo(BigInteger.valueOf(amt)) <= 0;

        ArrayList<RoutingTableEntry> all_peers = new ArrayList<>();
        ArrayList<RoutingTableEntry> remaining_peers = new ArrayList<>();

        for(int i = 0; i < m_m_bits; i++)
        {
            Lib.Pair<RoutingTableEntry[], RoutingTableEntry[]> bucket_peers = GetClosePeersFromBucket(peer_id, BigInteger.valueOf(1).shiftLeft(i), take_all);
            if(bucket_peers.first != null) {
                all_peers.addAll(Arrays.asList(bucket_peers.first));
            }

            if(bucket_peers.second != null) {
                remaining_peers.addAll(Arrays.asList(bucket_peers.second));
            }
        }

        // Get remaining peers based on closest to equal total.
        if(!take_all && all_peers.size() < amt)
        {
            int diff = Math.abs(all_peers.size() - amt);

            while(diff > 0)
            {
                int curr_closest_peer_idx = GetRemainingClosestPeers(peer_id, remaining_peers);

                all_peers.add(remaining_peers.get(curr_closest_peer_idx));
                remaining_peers.remove(curr_closest_peer_idx);
                diff--;
            }
        }

        return all_peers.toArray(new RoutingTableEntry[all_peers.size()]);
    }

    private RoutingTableEntry GetClosestPeerInRoutingTable(BigInteger peer_id)
    {
        BigInteger bucket = DetermineBucket(peer_id);

        BigInteger min = Distance(m_routing_table.get(bucket).firstEntry().getValue().id, peer_id);
        BigInteger min_peer = m_routing_table.get(bucket).firstKey();

        for(var peer : m_routing_table.get(bucket).entrySet())
        {
            if(Distance(peer.getKey(), peer_id).compareTo(min) < 0)
            {
                min = Distance(peer.getKey(), peer_id);
                min_peer = peer.getKey();
            }
        }
        return m_routing_table.get(bucket).get(min_peer);
    }

    private Lib.Pair<RoutingTableEntry[], RoutingTableEntry[]> GetClosePeersFromBucket(BigInteger peer_id, BigInteger bucket_id, boolean take_all)
    {
        ArrayList<RoutingTableEntry> peers = new ArrayList<>();
        ArrayList<RoutingTableEntry> remaining_peers = new ArrayList<>();

        BigInteger closest_peer_dist = BigInteger.valueOf(Integer.MAX_VALUE);
        BigInteger closest_peer = BigInteger.valueOf(-1);

        for(var peer : this.m_routing_table.get(bucket_id).entrySet())
        {
            if(!take_all)
            {
                if(Distance(peer_id, peer.getKey()).compareTo(closest_peer_dist) < 0 && !Objects.equals(peer.getKey(),peer_id))
                {
                    closest_peer_dist = Distance(peer_id, peer.getKey());
                    closest_peer = peer.getKey();
                }
            } else peers.add(peer.getValue());
            remaining_peers.add(peer.getValue());
        }

        if(!take_all && !Objects.equals(closest_peer, (BigInteger.valueOf(-1))))
            peers.add(this.m_routing_table.get(bucket_id).get(closest_peer));

        for (RoutingTableEntry peer : peers)
            remaining_peers.remove(peer);

        return new Lib.Pair<>(peers.isEmpty() ? null : peers.toArray(new RoutingTableEntry[peers.size()]),
                              remaining_peers.isEmpty() ? null : remaining_peers.toArray(new RoutingTableEntry[remaining_peers.size()]));
    }

    private int GetRemainingClosestPeers(BigInteger peer_id, ArrayList<RoutingTableEntry> remaining_peers)
    {
        BigInteger curr_closest_peer_dist = BigInteger.valueOf(Integer.MAX_VALUE);

        int curr_closest_idx = -1;
        for (int i = 0; i < remaining_peers.size(); i++)
        {
            if (Distance(peer_id, remaining_peers.get(i).id).compareTo(curr_closest_peer_dist) < 0 && remaining_peers.get(i).id != peer_id) {
                curr_closest_peer_dist = Distance(peer_id, remaining_peers.get(i).id);
                curr_closest_idx = i;
            }
        }
        return curr_closest_idx;
    }

    private BigInteger GetTotalPeersInRoutingTable(BigInteger peer_id)
    {
        BigInteger sum = BigInteger.valueOf(0);

        for(var bucket : this.m_routing_table.entrySet())
        {
            int bias = 0;
            if(bucket.getValue().get(peer_id) != null)
                bias = -1;

            sum = sum.add(BigInteger.valueOf(bucket.getValue().size()).add(BigInteger.valueOf(bias)));
        }

        return sum;
    }

    private BigInteger Distance(BigInteger peer_id_1, BigInteger peer_id_2)
    {
        return peer_id_1.xor(peer_id_2);
    }

    private BigInteger DetermineBucket(BigInteger id)
    {
        BigInteger min = Distance(BigInteger.valueOf(1), id);
        BigInteger min_pow = BigInteger.valueOf(1);

        for(int i = 0; i < m_m_bits; i++)
        {
            if(Distance(BigInteger.valueOf(1).shiftLeft(i), id).compareTo(min) < 0)
            {
                min = Distance(BigInteger.valueOf(1).shiftLeft(i), id);
                min_pow = BigInteger.valueOf(1).shiftLeft(i);
            }
        }
        return min_pow;
    }

    private void InsertPeerIntoRoutingTable(String nick_name, BigInteger peer_id, String remote_ip, int remote_port)
    {
        if(!Objects.equals(peer_id, m_id))
        {
            BigInteger bucket = DetermineBucket(peer_id);
            this.m_routing_table.get(bucket).put(peer_id, new RoutingTableEntry(nick_name, peer_id, remote_ip, remote_port));
            m_ping_vector.put(peer_id, true);
        }
    }

    private void DefineUDPSocket(int port) throws UnknownHostException
    {
        try
        {
            this.m_socket = new core.peer.Node(port);
        }
        catch(BindException e)
        {
            System.err.println("Ensure the IP address and port is not currently opened (" + InetAddress.getLocalHost().getHostAddress() + ")");
            System.exit(-1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void DefineRoutingTable()
    {
        this.m_routing_table = new TreeMap<>();

        for(int i = 0; i < this.m_m_bits; i++)
            this.m_routing_table.put(BigInteger.valueOf(1).shiftLeft(i), new TreeMap<>());
    }

    private void DefineDataTable()
    {
        this.m_data_table = new TreeMap<>();
        this.m_data_keys = new HashMap<>();
    }

    private void DefinePingVector()
    {
        this.m_ping_vector = new TreeMap<>();
    }

    public BigInteger m_id;
    public int m_m_bits;
    public int m_alpha;
    public core.peer.Node m_socket;
    public final int m_heartbeat_interval = 10;

    public boolean m_connected;
    public String m_bootstrapped_ip;
    public int m_bootstrapped_port;
    public String m_nickname;
    public Semaphore m_processing;
    public Sender m_sender;
    public Receiver m_receiver;
    public Heartbeat m_heartbeat;
    public NavigableMap<BigInteger, Boolean> m_ping_vector;
    public NavigableMap<BigInteger, Lib.Pair<String, String>> m_data_table;
    public HashMap<BigInteger, String> m_data_keys;
    public NavigableMap<BigInteger, NavigableMap<BigInteger, RoutingTableEntry>> m_routing_table;
    private LinkedBlockingQueue<Lib.Pair<RoutingTableEntry, byte[]>> m_received;
}
