package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.parser.table.Term;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;

import java.util.Stack;

// TODO: 实验三: 实现语义分析
public class SemanticAnalyzer implements ActionObserver {
    private record TermStr(Term term,String str){

    }
    private SymbolTable symbolTable;
    private final Stack<TermStr> stkTermStr=new Stack<>();

    @Override
    public void whenAccept(Status currentStatus) {
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        if (production.index() == 4) {
            symbolTable.get(stkTermStr.get(stkTermStr.size() - 1).str).setType(SourceCodeType.Int);
        }
        production.body().forEach((Term term) -> stkTermStr.pop());
        stkTermStr.push(new TermStr(production.head(),null));
    }

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        stkTermStr.push(new TermStr(currentToken.getKind(),currentToken.getText()));
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        symbolTable=table;
    }
}

