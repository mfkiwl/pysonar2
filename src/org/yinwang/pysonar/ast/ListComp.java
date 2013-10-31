package org.yinwang.pysonar.ast;

import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.ListType;
import org.yinwang.pysonar.types.Type;

import java.util.List;

public class ListComp extends Node {

    static final long serialVersionUID = -150205687457446323L;

    public Node elt;
    public List<Comprehension> generators;


    public ListComp(Node elt, List<Comprehension> generators, int start, int end) {
        super(start, end);
        this.elt = elt;
        this.generators = generators;
        addChildren(elt);
        addChildren(generators);
    }

    /**
     * Python's list comprehension will bind the variables used in generators.
     * This will erase the original values of the variables even after the
     * comprehension.
     */
    @Override
    public Type resolve(Scope s, int tag) throws Exception {
        resolveList(generators, s, tag);
        return new ListType(resolveExpr(elt, s, tag));
    }

    @Override
    public String toString() {
        return "<NListComp:" + start + ":" + elt + ">";
    }

    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(elt, v);
            visitNodeList(generators, v);
        }
    }
}
