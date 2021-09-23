package de.upb.docgen.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TreeNode<T>{
    private T data = null;
    private List<TreeNode> children = new ArrayList<>();
    private TreeNode parent = null;

    public TreeNode(T data) {
        this.data = data;
    }

    public void addChild(TreeNode child) {
        child.setParent(this);
        this.children.add(child);
    }

    public void addChild(T data) {
        TreeNode<T> newChild = new TreeNode<>(data);
        this.addChild(newChild);
    }

    public void addChildren(List<TreeNode> children) {
        for(TreeNode t : children) {
            t.setParent(this);
        }
        this.children.addAll(children);
    }

    public List<TreeNode> getChildren() {
        return children;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    private void setParent(TreeNode parent) {
        this.parent = parent;
    }

    public TreeNode getParent() {
        return parent;
    }


    private static int countNodes(TreeNode<String> tree) {
        int numberNodes = 1;
        for (TreeNode<String> child : tree.getChildren()) {
            numberNodes += countNodes(child);
        }
        return numberNodes;
    }

    public static void printLinkedTree(TreeNode<String> root) {
        if(root == null) return;
        int numberNodes = countNodes(root);
        boolean[] flag = new boolean[numberNodes];
        Arrays.fill(flag, true);
        printNTree(root, flag, 0, false);
    }

    public static void printLinkedTree(TreeNode<String> root, FileWriter fileWriter) throws IOException {
        if(root == null) return;
        int numberNodes = countNodes(root);
        boolean[] flag = new boolean[numberNodes];
        Arrays.fill(flag, true);
        printNTree(root, flag, 0, false, fileWriter);
    }


    private static void printNTree(TreeNode<String> x,
                                   boolean[] flag,
                                   int depth, boolean isLast )
    {
        // Condition when node is None
        if (x == null)
            return;

        // Loop to print the depths of the
        // current node
        for (int i = 1; i < depth; ++i) {

            // Condition when the depth
            // is exploring
            if (flag[i] == true) {
                System.out.print("| "
                        + " "
                        + " "
                        + " ");
            }

            // Otherwise print
            // the blank spaces
            else {
                System.out.print(" "
                        + " "
                        + " "
                        + " ");
            }
        }

        // Condition when the current
        // node is the root node
        if (depth == 0)
            System.out.println(x.getData());

            // Condition when the node is
            // the last node of
            // the exploring depth
        else if (isLast) {
            System.out.print("+--- " +  x.getData() + '\n');

            // No more childrens turn it
            // to the non-exploring depth
            flag[depth] = false;
        }
        else {
            System.out.print("+--- " +  x.getData() + '\n');
        }

        int it = 0;
        for (TreeNode<String> i : x.getChildren()) {
            ++it;

            // Recursive call for the
            // children nodes
            printNTree(i, flag, depth + 1,
                    it == (x.getChildren().size()) - 1);
        }
        flag[depth] = true;
    }


    private static void printNTree(TreeNode<String> x,
                                   boolean[] flag,
                                   int depth, boolean isLast, FileWriter fileWriter ) throws IOException {
        // Condition when node is None
        if (x == null)
            return;

        // Loop to print the depths of the
        // current node
        for (int i = 1; i < depth; ++i) {

            // Condition when the depth
            // is exploring
            if (flag[i] == true) {
                fileWriter.write("| "
                        + " "
                        + " "
                        + " ");
            }

            // Otherwise print
            // the blank spaces
            else {
                fileWriter.write(" "
                        + " "
                        + " "
                        + " ");
            }
        }

        // Condition when the current
        // node is the root node
        if (depth == 0)
            fileWriter.write(x.getData()  + '\n');

            // Condition when the node is
            // the last node of
            // the exploring depth
        else if (isLast) {
            fileWriter.write("+--- " +  x.getData() + '\n');

            // No more childrens turn it
            // to the non-exploring depth
            flag[depth] = false;
        }
        else {
            fileWriter.write("+--- " +  x.getData() + '\n');
        }

        int it = 0;
        for (TreeNode<String> i : x.getChildren()) {
            ++it;

            // Recursive call for the
            // children nodes
            printNTree(i, flag, depth + 1,
                    it == (x.getChildren().size()) - 1, fileWriter);
        }
        flag[depth] = true;
    }



}