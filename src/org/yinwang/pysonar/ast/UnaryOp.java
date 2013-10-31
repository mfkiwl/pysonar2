package org.yinwang.pysonar.ast;

import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;

public class UnaryOp extends Node {

    static final long serialVersionUID = 4877088513200468108L;

    public Node op;
    public Node operand;


    public UnaryOp(Node op, Node n, int start, int end) {
        super(start, end);
        this.op = op;
        this.operand = n;
        addChildren(op, n);
    }

    @Override
    public Type resolve(Scope s, int tag) throws Exception {
        return resolveExpr(operand, s, tag);
    }

    @Override
    public String toString() {
        return "<UOp:" + op + ":" + operand + ">";
    }

    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(op, v);
            visitNode(operand, v);
        }
    }
}
