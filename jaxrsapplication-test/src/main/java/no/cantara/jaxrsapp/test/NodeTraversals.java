package no.cantara.jaxrsapp.test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class NodeTraversals {

    public static void depthFirstPostOrderWithConcurrentSiblingExecutions(ExecutorService executor, Set<Node> ancestors, Node node, Consumer<Node> visitor) {
        boolean added = ancestors.add(node);
        if (!added) {
            throw new RuntimeException(String.format("Circular reference. Chain: %s->%s",
                    ancestors.stream().map(Node::getId).collect(Collectors.joining("->")),
                    node.getId()
            ));
        }
        try {
            List<Node> children = node.getChildren();
            CompletableFuture[] childTasks = new CompletableFuture[children.size()];
            for (int i = 0; i < childTasks.length; i++) {
                Node child = children.get(i);
                childTasks[i] = CompletableFuture.runAsync(() -> depthFirstPostOrderWithConcurrentSiblingExecutions(executor, ancestors, child, visitor), executor);
            }
            CompletableFuture.allOf(childTasks).join();
        } finally {
            ancestors.remove(node);
        }
        visitor.accept(node);
    }
}
