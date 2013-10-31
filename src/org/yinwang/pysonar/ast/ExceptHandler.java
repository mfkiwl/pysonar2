package org.yinwang.pysonar.ast;

import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;

public class ExceptHandler extends Node {

    static final long serialVersionUID = 6215262228266158119L;

    public Node name;
    public Node exceptionType;
    public Block body;


    public ExceptHandler(Node name, Node exceptionType, Block body, int start, int end) {
        super(start, end);
        this.name = name;
        this.exceptionType = exceptionType;
        this.body = body;
        addChildren(name, exceptionType, body);
    }

    @Override
    public boolean bindsName() {
        return true;
    }

    @Override
    public Type resolve(Scope s, int tag) throws Exception {
        Type typeval = Indexer.idx.builtins.unknown;
        if (exceptionType != null) {
            typeval = resolveExpr(exceptionType, s, tag);
        }
        if (name != null) {
            NameBinder.bind(s, name, typeval, tag);
        }
        if (body != null) {
            return resolveExpr(body, s, tag);
        } else {
            return Indexer.idx.builtins.unknown;
        }
    }

    @Override
    public String toString() {
        return "<ExceptHandler:" + start + ":" + name + ":" + exceptionType + ">";
    }

    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(name, v);
            visitNode(exceptionType, v);
            visitNode(body, v);
        }
    }
}
