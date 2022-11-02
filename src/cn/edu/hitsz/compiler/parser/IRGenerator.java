package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.parser.table.Term;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.*;


/**
 *
 */
public class IRGenerator implements ActionObserver {
    private final List<Instruction> IRs=new LinkedList<>();
    private SymbolTable symbolTable;
    private final Stack<IRValue> stkIRValue=new Stack<>();
    // 保存所有变量名与IR变量的对应关系
    private final Map<String,IRVariable> varMap = new HashMap<>();

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        switch (currentToken.getKind().getCode()){
            // 变量入栈
            case 51 -> stkIRValue.push(varMap.get(currentToken.getText()));
            // 整数常量（立即数）入栈
            case 52 -> stkIRValue.push(
                    IRImmediate.of(Integer.parseInt(currentToken.getText())));
            // 那些不代表任何值的token，用null入栈
            default -> stkIRValue.push(null);
        }
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        switch (production.index()){
            case 6 -> { // S -> id = E;
                var E = stkIRValue.pop();
                stkIRValue.pop();
                var id = stkIRValue.pop();
                assert id instanceof IRVariable;
                IRs.add(Instruction.createMov((IRVariable) id,E));
                stkIRValue.push(null);
            }
            case 7 -> { // S -> return E;
                var E = stkIRValue.pop();
                stkIRValue.pop();
                IRs.add(Instruction.createRet(E));
                stkIRValue.push(null);
            }
            case 8 -> { // E -> E + A;
                var A = stkIRValue.pop();
                stkIRValue.pop();
                var E = stkIRValue.pop();
                var res = IRVariable.temp();
                IRs.add(Instruction.createAdd(res,E,A));
                stkIRValue.push(res);
            }
            case 9 -> { // E -> E - A;
                var A = stkIRValue.pop();
                stkIRValue.pop();
                var E = stkIRValue.pop();
                var res = IRVariable.temp();
                IRs.add(Instruction.createSub(res,E,A));
                stkIRValue.push(res);
            }
            case 11 -> { // A -> A * B;
                var B = stkIRValue.pop();
                stkIRValue.pop();
                var A = stkIRValue.pop();
                var res = IRVariable.temp();
                IRs.add(Instruction.createMul(res,A,B));
                stkIRValue.push(res);
            }
            case 10,12,14,15 -> { // E->A; A->B; B->id; B->IntConst;
                stkIRValue.push(stkIRValue.pop());
            }
            case 13 -> { // B->(E);
                stkIRValue.pop();
                var E = stkIRValue.pop();
                stkIRValue.pop();
                stkIRValue.push(E);
            }
            default -> {
                production.body().forEach((Term term) -> stkIRValue.pop());
                stkIRValue.push(null);
            }
        }
    }


    @Override
    public void whenAccept(Status currentStatus) {
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        symbolTable=table;
        symbolTable.getAllNames().forEach(
                (var name) -> varMap.put(name,IRVariable.named(name)));
    }

    public List<Instruction> getIR() {
        return IRs;
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }
}

