package feign;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yangjie
 * @date 2023/11/19 13:16
 */
public class CapabilityEnrichExample {

    public static void main(String[] args) {
        // 假如我们有四个组件分别是1,2,3,4
        List<Integer> components = Arrays.asList(1, 2, 3, 4);
        // 我们有两个增强器，分别是6,9，我们需要对每个组件增强两次,例如1要先+6再+9 = 16
        List<Integer> capabilities = Arrays.asList(6, 9);
        // 对每个组件进行增强
        List<Integer> enrichComponents = components.stream().map(component -> enrichTest(component, capabilities)).collect(Collectors.toList());
        System.out.println(enrichComponents);
        // 换一种方式进行增强
        List<Integer> enrichComponents2 = components.stream().map(component -> enrichTest2(component, capabilities)).collect(Collectors.toList());
        System.out.println(enrichComponents2);
    }

    static Integer enrichTest(Integer component, List<Integer> capabilities) {
        // 对组件进行逐个增强
        return capabilities.stream()
                .reduce(component,
                        (target, capability) -> addEnrich(target, capability),
                        (oldComponent, enrichedComponent) -> enrichedComponent);
    }

    static Integer enrichTest2(Integer component, List<Integer> capabilities) {
        for (Integer capability : capabilities) {
            component = component + capability;
        }
        return component;
    }

    static Integer addEnrich(Integer target, Integer capability) {
        return target + capability;
    }
}
