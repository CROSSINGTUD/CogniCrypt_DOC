package de.upb.docgen.utils;

import java.util.ArrayList;
import java.util.List;

public class TreeNode<T>{
    // Payload stored in the node.
    private T data = null;
    // Children of this node (unordered list).
    private final List<TreeNode> children = new ArrayList<>();
    // Parent pointer for upward navigation.
    private TreeNode parent = null;

    /**
     * Create a node with the given payload.
     */
    public TreeNode(T data) {
        this.data = data;
    }

    /**
     * Add a child node and set its parent to this node.
     */
    public void addChild(TreeNode child) {
        child.setParent(this);
        this.children.add(child);
    }

    /**
     * Create and add a child node with the given payload.
     */
    public void addChild(T data) {
        TreeNode<T> newChild = new TreeNode<>(data);
        this.addChild(newChild);
    }

    /**
     * Add multiple child nodes and set their parent pointers.
     */
    public void addChildren(List<TreeNode> children) {
        for(TreeNode t : children) {
            t.setParent(this);
        }
        this.children.addAll(children);
    }

    /**
     * Return the direct children of this node.
     */
    public List<TreeNode> getChildren() {
        return children;
    }

    /**
     * Return the payload stored in this node.
     */
    public T getData() {
        return data;
    }

    /**
     * Update the payload stored in this node.
     */
    public void setData(T data) {
        this.data = data;
    }

    /**
     * Assign the parent pointer (used internally when linking nodes).
     */
    private void setParent(TreeNode parent) {
        this.parent = parent;
    }

    /**
     * Return the parent node or null if this is a root.
     */
    public TreeNode getParent() {
        return parent;
    }


}