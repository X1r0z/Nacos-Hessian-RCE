package com.example;

import com.alipay.sofa.jraft.Iterator;
import com.alipay.sofa.jraft.core.StateMachineAdapter;

public class MyStateMachine extends StateMachineAdapter {
    @Override
    public void onApply(Iterator iter) {
        System.out.println(iter);
    }
}
