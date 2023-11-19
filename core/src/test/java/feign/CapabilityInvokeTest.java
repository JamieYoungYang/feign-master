package feign;

import java.util.Objects;

/**
 * @author yangjie
 * @date 2023/11/19 12:10
 */
public class CapabilityInvokeTest {
    static class TestCapability implements Capability {
        public Father enrich(Father father) {
            father.balance = 10;
            return father;
        }

        public Son enrich(Son son) {
            son.balance = 5;
            return son;
        }
    }

    static class Father {
        int balance;
    }

    static class Son extends Father {
    }

    public static void main(String[] args) {
        Son son = new Son();
        Father father = new Father();
        // enrich Son
        Capability.invoke(son, new TestCapability(), Son.class);
        System.out.println(son.balance);

        // enrich Father
        Capability.invoke(father, new TestCapability(), Father.class);
        System.out.println(father.balance);
    }
}
