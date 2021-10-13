package no.cantara.jaxrsapp.test;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class NodeTraversals {

    public static void depthFirstPostOrder(Set<Node> ancestors, Node node, Consumer<Node> visitor) {
        boolean added = ancestors.add(node);
        if (!added) {
            throw new RuntimeException(String.format("Circular reference. Chain: %s->%s",
                    ancestors.stream().map(Node::getId).collect(Collectors.joining("->")),
                    node.getId()
            ));
        }
        try {
            for (Node child : node.getChildren()) {
                depthFirstPostOrder(ancestors, child, visitor);
            }
        } finally {
            ancestors.remove(node);
        }
        visitor.accept(node);
    }
}
