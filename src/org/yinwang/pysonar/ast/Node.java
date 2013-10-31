package org.yinwang.pysonar.ast;

import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;

import java.util.ArrayList;
import java.util.List;

public abstract class Node implements java.io.Serializable {

    static final long serialVersionUID = 3682719481356964898L;

    public int start = -1;
    public int end = 0;

    protected Node parent = null;

    public Node() {
    }

    public Node(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public Node getParent() {
        return parent;
    }

    public Node getAstRoot() {
        if (parent == null) {
            return this;
        }
        return parent.getAstRoot();
    }

    public int length() {
        return end - start;
    }


    /**
     * Returns {@code true} if this is a name-binding node.
     * Includes functions/lambdas, function/lambda params, classes,
     * assignments, imports, and implicit assignment via for statements
     * and except clauses.
     * @see "http://www.python.org/dev/peps/pep-0227"
     */
    public boolean bindsName() {
        return false;
    }

    /**
     * @return the path to the code that generated this AST
     */
    public String getFile() {
        return parent != null ? parent.getFile() : null;
    }

    public void addChildren(Node... nodes) {
        if (nodes != null) {
            for (Node n : nodes) {
                if (n != null) {
                    n.setParent(this);
                }
            }
        }
    }

    public void addChildren(List<? extends Node> nodes) {
        if (nodes != null) {
            for (Node n : nodes) {
                if (n != null) {
                    n.setParent(this);
                }
            }
        }
    }


    public static Type resolveExpr(Node n, Scope s, int tag) throws Exception {
        Type result = n.resolve(s, tag);
        if (result == null) {
            Indexer.idx.warn(n + " resolved to a null type");
            return Indexer.idx.builtins.unknown;
        }
        return result;
    }

    /**
     * @param s the symbol table
     * @param tag thread tag of the execution path
     */
    abstract public Type resolve(Scope s, int tag) throws Exception;

    public boolean isCall() {
        return this instanceof Call;
    }

    public boolean isModule() {
        return this instanceof Module;
    }

    public boolean isClassDef() {
        return false;
    }

    public boolean isFunctionDef() {
        return false;
    }

    public boolean isLambda() {
        return false;
    }

    public boolean isName() {
        return this instanceof Name;
    }
    
    public boolean isGlobal() {
        return this instanceof Global;
    }
    
    public Call asCall() {
        return (Call)this;
    }

    public Module asModule() {
        return (Module)this;
    }

    public ClassDef asClassDef() {
        return (ClassDef)this;
    }

    public FunctionDef asFunctionDef() {
        return (FunctionDef)this;
    }

    public Lambda asLambda() {
        return (Lambda)this;
    }

    public Name asName() {
        return (Name)this;
    }
    

    public Global asGlobal() {
        return (Global)this;
    }

    protected void visitNode(Node n, NodeVisitor v) {
        if (n != null) {
            n.visit(v);
        }
    }

    protected void visitNodeList(List<? extends Node> nodes, NodeVisitor v) {
        if (nodes != null) {
            for (Node n : nodes) {
                if (n != null) {
                    n.visit(v);
                }
            }
        }
    }

    /**
     * Visits this node and optionally its children. <p>
     *
     * @param visitor the object to call with this node.
     *        If the visitor returns {@code true}, the node also
     *        passes its children to the visitor.
     */
    public abstract void visit(NodeVisitor visitor);

    protected void addWarning(String msg) {
        Indexer.idx.putProblem(this, msg);
    }

    protected void addWarning(Node loc, String msg) {
        Indexer.idx.putProblem(loc, msg);
    }

    protected void addError(String msg) {
        Indexer.idx.putProblem(this, msg);
    }

    protected void addError(Node loc, String msg) {
        Indexer.idx.putProblem(loc, msg);
    }

    /**
     * Utility method to resolve every node in {@code nodes} and
     * return the union of their types.  If {@code nodes} is empty or
     * {@code null}, returns a new {@link org.yinwang.pysonar.types.UnknownType}.
     * @throws Exception 
     */
    protected Type resolveListAsUnion(List<? extends Node> nodes, Scope s, int tag) throws Exception {
        if (nodes == null || nodes.isEmpty()) {
            return Indexer.idx.builtins.unknown;
        }

        Type result = null;
        for (Node node : nodes) {
            Type nodeType = resolveExpr(node, s, tag);
            if (result == null) {
                result = nodeType;
            } else {
                result = UnionType.union(result, nodeType);
            }
        }
        return result;
    }

    /**
     * Resolves each element of a node list in the passed scope.
     * Node list may be empty or {@code null}.
     * @throws Exception 
     */
    static protected void resolveList(List<? extends Node> nodes, Scope s, int tag) throws Exception {
        if (nodes != null) {
            for (Node n : nodes) {
                resolveExpr(n, s, tag);
            }
        }
    }
    
    static protected List<Type> resolveAndConstructList(List<? extends Node> nodes, Scope s, int tag) throws Exception {
        if (nodes == null) {
            return null;
        } else {
            List<Type> typeList = new ArrayList<Type>();
            for (Node n : nodes) {
                typeList.add(resolveExpr(n, s, tag));
            }
            return typeList;
        } 
    }

    /**
     * Assumes nodes are always traversed in increasing order of their start
     * positions.
     */
    static class DeepestOverlappingNodeFinder extends GenericNodeVisitor {
        private int offset;
        private Node deepest;

        public DeepestOverlappingNodeFinder(int offset) {
            this.offset = offset;
        }

        /**
         * Returns the deepest node overlapping the desired source offset.
         * @return the node, or {@code null} if no node overlaps the offset
         */
        public Node getNode() {
            return deepest;
        }

        @Override
        public boolean dispatch(Node node) {
            // This node ends before the offset, so don't look inside it.
            if (offset > node.end) {
                return false;  // don't traverse children, but do keep going
            }

            if (offset >= node.start && offset <= node.end) {
                deepest = node;
                return true;  // visit kids
            }

            // this node starts after the offset, so we're done
            throw new NodeVisitor.StopIterationException();
        }
    }

    /**
     * Searches the AST for the deepest node that overlaps the specified source
     * offset.  Can be called from any node in the AST, as it traverses to the
     * parent before beginning the search.
     * @param sourceOffset the spot at which to look for a node
     * @return the deepest AST node whose start is greater than or equal to the offset,
     *         and whose end is less than or equal to the offset.  Returns {@code null}
     *         if no node overlaps {@code sourceOffset}.
     */
    public Node getDeepestNodeAtOffset(int sourceOffset) {
        Node ast = getAstRoot();
        DeepestOverlappingNodeFinder finder = new DeepestOverlappingNodeFinder(sourceOffset);
        try {
            ast.visit(finder);
        } catch (NodeVisitor.StopIterationException six) {
            // expected
        }
        return finder.getNode();
    }
}
