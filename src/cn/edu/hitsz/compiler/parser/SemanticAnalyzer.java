package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;

import java.util.Stack;

public class SemanticAnalyzer implements ActionObserver {
    private SymbolTable symbolTable;
    private final Stack<String> stkName =new Stack<>();

    @Override
    public void whenAccept(Status currentStatus) {
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        if (production.index() == 4) { // S -> D id;
            var id = stkName.pop();
            stkName.pop();
            symbolTable.get(id).setType(SourceCodeType.Int);
        }else
            production.body().forEach((var term) -> stkName.pop());
        stkName.push(null);
    }

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        stkName.push(currentToken.getText());
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        symbolTable=table;
    }
}

