package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * TODO: 实验一: 实现词法分析
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {
    private final SymbolTable symbolTable;
    private final List<Token> tokens;
    private BufferedReader reader;

    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.tokens = new LinkedList<>();
    }


    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) throws IOException{
        // 可自由实现各类缓冲区
        // 或直接采用完整读入方法
        InputStreamReader streamReader = new InputStreamReader(new FileInputStream(path));
        reader = new BufferedReader(streamReader);
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() throws IOException{
        int state=0;
        StringBuilder tokenStr= null;
        for (char c=(char)reader.read();c!='\uffff';c=(char)reader.read()){
            boolean blank = c==' ' || c=='\t' || c=='\n' || c=='\r';
            boolean letter = 'a'<=c && c<='z' || 'A'<=c && c<='Z' || c=='_';
            boolean digit = '0'<=c && c<='9';
            int nextState;
            // 实现状态转换
            switch (state){
                case 0 -> {
                    if (blank) nextState=0;
                    else if (letter) {
                        nextState=5;
                        tokenStr = new StringBuilder("" + c);
                    }else if (digit) {
                        nextState = 23;
                        tokenStr = new StringBuilder("" + c);
                    }
                    else nextState = switch (c){
                             case '=' -> 14;
                             case ',' -> 15;
                             case ';' -> 16;
                             case '+' -> 17;
                             case '-' -> 18;
                             case '*' -> 19;
                             case '/' -> 20;
                             case '(' -> 21;
                             case ')' -> 22;
                             default -> throw new RuntimeException("Unknown character!");
                        };
                }
                case 5 -> {
                    if (letter||digit) {
                        nextState = 5;
                        tokenStr.append(c);
                    }
                    else nextState=6;
                }
                case 23 -> {
                    if (digit) {
                        nextState = 23;
                        tokenStr.append(c);
                    }
                    else nextState=24;
                }
                // 这里不应遇到终止状态
                case 6,14,15,16,17,18,19,20,21,22,24 -> throw new RuntimeException("Unexpected End State"+state);
                default -> throw new RuntimeException("Unknown State"+state);
            }
            // 终止状态不再接受字符，应立即跳转至0
            state=0;
            switch (nextState){
                case 6 -> {
                    String s = tokenStr.toString();
                    switch (s){
                        case "int" -> tokens.add(Token.normal("int",""));
                        case "return" -> tokens.add(Token.normal("return",""));
                        default -> {
                            tokens.add(Token.normal("id", s));
                            // 维护符号表
                            if (!symbolTable.has(s)) symbolTable.add(s);
                        }
                    }
                    reader.reset();
                }
                case 24 -> {
                    tokens.add(Token.normal("IntConst", tokenStr.toString()));
                    reader.reset();
                }
                case 16 -> tokens.add(Token.normal("Semicolon",""));
                case 14,15,17,18,19,20,21,22 -> tokens.add(Token.normal(""+c,""));
                // 非终止状态正常跳转
                default -> state=nextState;
            }
        reader.mark(1);
        }
        tokens.add(Token.eof());
    }

    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        // 词法分析过程可以使用 Stream 或 Iterator 实现按需分析
        // 亦可以直接分析完整个文件
        // 总之实现过程能转化为一列表即可
        return tokens;
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
            path,
            StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList()
        );
    }


}
