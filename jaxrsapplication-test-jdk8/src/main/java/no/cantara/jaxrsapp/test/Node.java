package no.cantara.jaxrsapp.test;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Node {

    final List<Node> parents = new LinkedList<>();
    final String id;
    final List<Node> children = new LinkedList<>();
    final Map<String, String> attributes = new LinkedHashMap<>();
    final Map<String, Object> objects = new LinkedHashMap<>();

    public Node(Node parent, String id) {
        Objects.requireNonNull(id);
        this.parents.add(parent);
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public List<Node> getParents() {
        return parents;
    }

    public List<Node> getChildren() {
        return children;
    }

    public void addChild(Node child) {
        this.children.add(child);
    }

    public String getAttribute(String key) {
        return attributes.get(key);
    }

    public String putAttribute(String key, String value) {
        return attributes.put(key, value);
    }

    public <T> T get(String key) {
        return (T) objects.get(key);
    }

    public <T> Node put(String key, T instance) {
        objects.put(key, instance);
        return this;
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Node node = (Node) o;

        return id.equals(node.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
