package com.czl.apt;

import com.google.auto.service.AutoService;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import java.util.*;

/**
 * 该注解表明当前注解处理器仅能处理
 * com.czl.apt.Getter注解
 */
@SupportedAnnotationTypes("com.czl.apt.Getter")
/**
 * javac调用注解处理器时是使用spi机制调用
 * 因此需要在META-INF下创建spi文件
 * 使用该@AutoService注解可以自动创建
 * Google的工具
 */
@AutoService(Processor.class)
public class GetterProcessor extends AbstractProcessor {

    private Elements elementUtils;
    private JavacTrees javacTrees;
    private TreeMaker treeMaker;
    private Names names;

    /**
     * 初始化，此处可以获取各种官方提供的工具类
     *
     * @param processingEnv
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.elementUtils = processingEnv.getElementUtils();

        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        //JCTree工具类
        this.javacTrees = JavacTrees.instance(processingEnv);
        //JCTree工具类
        this.treeMaker = TreeMaker.instance(context);
        //命名工具类
        this.names = Names.instance(context);
    }

    /**
     * 重点：Jvm会调用该方法执行注解处理
     *
     * @param annotations 被该处理器支持的注解
     * @param roundEnv    环境
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        //1. 获取要处理的注解的TypeElement(可以遍历annotations入参获取)  这里采用全限定类名获取
        TypeElement anno = elementUtils.getTypeElement("com.czl.apt.Getter");
        //2. 获取打上注解的Element(这些Element可能为方法，接口等等)
        Set<? extends Element> types = roundEnv.getElementsAnnotatedWith(anno);
        //3. 遍历这些Element
        types.forEach(t -> {
            //3. 判断类型是否为Class  在编译时需要使用getKind判断当前Element类型
            if (t.getKind().isClass()) {
                //4. 处理
                currentClassProcess((TypeElement) t);
            }
        });
        /**
         * 重要：当该方法返回True时  后续的注解处理器将不处理该注解
         *      当该方法返回False时  后续的注解处理器会处理该注解
         * 处理顺序将以SPI文件顺序处理
         */
        return false;
    }

    /**
     * 在当前类生成Getter方法
     *
     * @param typeElement
     */
    private void currentClassProcess(TypeElement typeElement) {
        try {
            //获取当前类的ast语法树
            JCTree.JCClassDecl classDecl = javacTrees.getTree(typeElement);
            //对该树进行访问   因为需要修改树结构，因此使用TreeTranslator
            classDecl.accept(new TreeTranslator() {
                @Override
                public void visitClassDef(JCTree.JCClassDecl tree) {
                    //该列表用于存放生成的Get方法
                    List<JCTree.JCMethodDecl> methods = new ArrayList<>();
                    //tree.defs为当前类中的方法和属性定义  遍历
                    for (JCTree varTree : tree.defs) {
                        //由于本次只对属性生成方法，因此只关注属性
                        if (varTree.getKind().equals(Tree.Kind.VARIABLE)) {
                            JCTree.JCVariableDecl jcVariableDecl = (JCTree.JCVariableDecl) varTree;
                            methods.add(generateGetMethods(jcVariableDecl));
                        }
                    }
                    /**
                     * prependList方法会将参数添加到列表头部并返回新列表
                     * 此处将生成的方法加入至方法定义中
                     */
                    tree.defs = tree.defs.prependList(com.sun.tools.javac.util.List.from(methods));
                    super.visitClassDef(tree);
                }
            });
        } catch (Exception e) {
            System.out.println(1);
        }

    }

    //使用TreeMaker生成方法
    private JCTree.JCMethodDecl generateGetMethods(JCTree.JCVariableDecl variableDecl) {
        //treeMaker.MethodDef  创建方法
        JCTree.JCMethodDecl methodDecl = treeMaker.MethodDef(
                //创建方法访问符 public
                treeMaker.Modifiers(Flags.PUBLIC),
                //方法名
                names.fromString("get" + variableDecl.getName().toString()),
                //返回值
                treeMaker.Ident(variableDecl.vartype.type.tsym),
                //泛型参数列表 此处写无
                com.sun.tools.javac.util.List.nil(),
                //入参列表  此处写无
                com.sun.tools.javac.util.List.nil(),
                //异常  此处写无
                com.sun.tools.javac.util.List.nil(),
                //方法体
                treeMaker.Block(
                        //我也不知道这是啥
                        0,
                        //语句链表  为了构建 return this.id;
                        new ListBuffer<JCTree.JCStatement>()
                                .append(
                                        //返回值  return
                                        treeMaker.Return(
                                                //此处是构建   this.id; 语句块
                                                treeMaker.Select(treeMaker.Ident(names.fromString("this")), variableDecl.name)
                                        )
                                ).toList()
                ),
                //不需要默认值
                null
        );
        return methodDecl;
    }


}
