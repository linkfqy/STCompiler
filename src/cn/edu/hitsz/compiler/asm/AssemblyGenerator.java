package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.RegisterNotEnoughException;
import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.utils.BMap;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * TODO: 实验四: 实现汇编生成
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {
    enum Reg {
        a0,t0,t1,t2,t3,t4,t5,t6
    }
    private List<Instruction> originIRs,IRs;
    private BMap<IRVariable,Reg> bMap;
    private Map<IRVariable,Integer> lastPos;
    private List<String> asm;

    private void preProcess(){
        IRs = new LinkedList<>();
        for (var ir : originIRs){
            switch (ir.getKind()){
                case RET -> {
                    IRs.add(ir);
                    return;
                }
                case ADD -> {
                    boolean lImm = ir.getLHS().isImmediate();
                    boolean rImm = ir.getRHS().isImmediate();
                    if (lImm && rImm){
                        IRs.add(Instruction.createMov(
                                ir.getResult(),
                                IRImmediate.of(
                                        ((IRImmediate) ir.getLHS()).getValue()
                                       +((IRImmediate) ir.getRHS()).getValue()
                                )
                        ));
                    }else if (lImm && !rImm){  // a = imm + b
                        IRs.add(Instruction.createAdd(
                                ir.getResult(),
                                ir.getRHS(),
                                ir.getLHS()
                        ));
                    }else IRs.add(ir);
                }
                case SUB -> {
                    boolean lImm = ir.getLHS().isImmediate();
                    boolean rImm = ir.getRHS().isImmediate();
                    if (lImm && rImm){
                        IRs.add(Instruction.createMov(
                                ir.getResult(),
                                IRImmediate.of(
                                        ((IRImmediate) ir.getLHS()).getValue()
                                       -((IRImmediate) ir.getRHS()).getValue()
                                )
                        ));
                    }else if (lImm && !rImm){  // a = imm - b
                        var temp = IRVariable.temp();
                        IRs.add(Instruction.createMov(temp,ir.getLHS()));
                        IRs.add(Instruction.createSub(
                                ir.getResult(),
                                temp,
                                ir.getRHS()
                        ));
                    }else IRs.add(ir);
                }
                case MUL -> {
                    boolean lImm = ir.getLHS().isImmediate();
                    boolean rImm = ir.getRHS().isImmediate();
                    if (lImm && rImm){
                        IRs.add(Instruction.createMov(
                                ir.getResult(),
                                IRImmediate.of(
                                        ((IRImmediate) ir.getLHS()).getValue()
                                       *((IRImmediate) ir.getRHS()).getValue()
                                )
                        ));
                    }else if (lImm ^ rImm){  // a = imm * b; a = b * imm;
                        var temp = IRVariable.temp();
                        var imm = ir.getLHS();
                        var b = ir.getRHS();
                        if (b instanceof IRImmediate){
                            var tt=imm;
                            imm=b;
                            b=tt;
                        }
                        IRs.add(Instruction.createMov(temp,imm));
                        IRs.add(Instruction.createMul(
                                ir.getResult(),
                                temp,
                                b
                        ));
                    }else IRs.add(ir);
                }
                case MOV -> IRs.add(ir);
            }
        }
    }

    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */
    public void loadIR(List<Instruction> originInstructions) {
        originIRs=originInstructions;
        bMap = new BMap<>();
        lastPos = new HashMap<>();
        preProcess();
        // 统计每个IR变量最后出现的位置
        int index=0;
        for (var ir : IRs){
            index++;
            switch (ir.getKind()){
                case MOV -> {
                    lastPos.put(ir.getResult(),index);
                    if (ir.getFrom() instanceof IRVariable from)
                        lastPos.put(from,index);
                }
                case RET -> {
                    if (ir.getReturnValue() instanceof IRVariable returnValue)
                        lastPos.put(returnValue,index);
                }
                case ADD,SUB,MUL -> {
                    lastPos.put(ir.getResult(),index);
                    if (ir.getLHS() instanceof IRVariable lhs)
                        lastPos.put(lhs,index);
                    if (ir.getRHS() instanceof IRVariable rhs)
                        lastPos.put(rhs,index);
                }
            }
        }
    }


    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */
    public void run() {
        bMap = new BMap<>();
        asm = new LinkedList<>();
        int index=0;
        for (var ir : IRs){
            index++;
            switch (ir.getKind()){
                // RET a 等价于 MOV a0, a
                case RET -> createMOV(Reg.a0,ir.getReturnValue(),index);
                case MOV -> createMOV(getReg(ir.getResult(),index),ir.getFrom(),index);
                case ADD -> createBinaryOp(
                        "add",
                        ir.getResult(),
                        (IRVariable) ir.getLHS(),
                        ir.getRHS(),
                        index
                );
                case SUB -> createBinaryOp(
                        "sub",
                        ir.getResult(),
                        (IRVariable) ir.getLHS(),
                        ir.getRHS(),
                        index
                );
                case MUL -> createBinaryOp(
                        "mul",
                        ir.getResult(),
                        (IRVariable) ir.getLHS(),
                        ir.getRHS(),
                        index
                );
            }
        }
    }

    private void createMOV(Reg res, IRValue from, int index){
        if (from instanceof IRVariable vFrom){
            asm.add(String.format("mv %s, %s", res, getReg(vFrom,index)));
        }else{  // MOV res imm
            asm.add(String.format("li %s, %d", res, ((IRImmediate)from).getValue()));
        }
    }

    private void createBinaryOp(String op, IRVariable res, IRVariable lhs, IRValue rhs, int index){
        if (rhs.isIRVariable()){
            asm.add(String.format("%s %s, %s, %s",op,
                    getReg(res,index),
                    getReg(lhs,index),
                    getReg((IRVariable) rhs,index))
            );
        }else{
            asm.add(String.format("%s %s, %s, %d",op+"i",
                    getReg(res,index),
                    getReg(lhs,index),
                    ((IRImmediate)rhs).getValue()
            ));
        }
    }

    /**
     * 由IR变量获得寄存器
     * <br>
     * 若已分配寄存器，直接返回
     * <br>
     * 否则寻找未被分配的寄存器
     * <br>
     * 再找不到，从用不到的变量腾出寄存器
     * @param variable 待获取寄存器的IR变量
     * @param index 当前生成的汇编行号
     * @return 找到的寄存器
     */
    private Reg getReg(IRVariable variable, int index) {
        // 已经分配了寄存器
        if (bMap.containKey(variable)) return bMap.getByKey(variable);
        // 寻找未分配的寄存器
        for (var reg : Reg.values()){
            if (reg!=Reg.a0 && !bMap.containValue(reg)) {
                bMap.replace(variable,reg);
                return reg;
            }
        }
        // 寻找用不到的变量
        for (var var_ : bMap.getAllKeys()){
            if (lastPos.get(var_) < index){
                // 变量var_再也用不到了，腾出寄存器
                Reg reg = bMap.getByKey(var_);
                bMap.replace(variable, reg);
                return reg;
            }
        }
        throw new RegisterNotEnoughException();
    }

    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
        FileUtils.writeLines(path,asm);
    }
}

