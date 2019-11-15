import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.HashMap;
import java.io.IOException;
import java.util.*;

public class VTableVisitor extends GJDepthFirst {
 @Override
   public Object visit(NodeList n, Object argu) {
       return super.visit(n, argu);
   }

   @Override
   public Object visit(NodeListOptional n, Object argu) {
       return super.visit(n, argu);
   }

   @Override
   public Object visit(NodeOptional n, Object argu) {
       return super.visit(n, argu);
   }

   @Override
   public Object visit(NodeSequence n, Object argu) {
       return super.visit(n, argu);
   }

   @Override
   public Object visit(NodeToken n, Object argu) {
       return n.tokenImage;
   }

   @Override
   public Object visit(Goal n, Object argu) {
       return super.visit(n, argu);
   }

   @Override
   public Object visit(MainClass n, Object argu) {
       return super.visit(n, argu);
   }

   @Override
   public Object visit(TypeDeclaration n, Object argu) {
       return super.visit(n, argu);
   }

   @Override
   public Object visit(ClassDeclaration n, Object argu) {
       MethodTable instance = MethodTable.getInstance();
       String class_name = (String) n.f1.accept(this,argu);
       List<String> methods_name = new ArrayList<String>();
       instance.cur_class = class_name;  //set cur class

       //create the list of fields name
       List<String> fields_name = new ArrayList<String>();
       Vector<Node> vec = n.f3.nodes;
       Iterator it = vec.iterator();
       while(it.hasNext()){
         VarDeclaration field = (VarDeclaration)it.next();
         String fn = (String)field.f1.accept(this,argu);
         String tn = (String)field.f0.accept(this,argu);
         fn = class_name + "." + fn;
         //check if current class has fields from other classes
         if(field.f0.f0.choice instanceof Identifier && !tn.equals(class_name))
            instance.types.put(fn, tn);

         fields_name.add(fn);
       }

       //fields_name.add(0, fields_name.size() + "");
       instance.insertFields(class_name, fields_name);

       //create the list of methods name
       vec = n.f4.nodes;
       it = vec.iterator();
       while(it.hasNext()) {
         MethodDeclaration md = (MethodDeclaration)it.next();
         String method_name = (String) md.f2.accept(this,argu);
         method_name = class_name + "." + method_name;
         methods_name.add(method_name);
       }

       //finally insert the class name and its methods name to hash table
       instance.insertMethods(class_name, methods_name);
       return super.visit(n, argu);
   }

   @Override
   public Object visit(ClassExtendsDeclaration n, Object argu) {
       MethodTable instance = MethodTable.getInstance();
       String class_name = (String) n.f1.accept(this,argu);
       List<String> methods_name = new ArrayList<String>();
       instance.cur_class = class_name;  //set cur class

       //inset parent class fileds name
       String parent_cn = (String) n.f3.accept(this,argu);
       List<String> parent_fields = instance.getFields(parent_cn);


       //insert child class fields name
       List<String> child_fields = new ArrayList<String>();
       Vector<Node> vec = n.f5.nodes;
       Iterator it = vec.iterator();
       while(it.hasNext()){
         VarDeclaration field = (VarDeclaration)it.next();
         String fn = (String)field.f1.accept(this,argu);
         String tn = (String)field.f0.accept(this,argu);
         fn = class_name + "." + fn;

         if(field.f0.f0.choice instanceof Identifier && !tn.equals(class_name))
            instance.types.put(fn, tn);

         child_fields.add(fn);
       }

       parent_fields.addAll(child_fields);

       //fields_name.add(0, fields_name.size() + "");
       instance.insertFields(class_name, parent_fields);

       //create the list of methods name
       vec = n.f6.nodes;
       it = vec.iterator();
       while(it.hasNext()) {
         MethodDeclaration md = (MethodDeclaration)it.next();
         String method_name = (String) md.f2.accept(this,argu);
         method_name = class_name + "." + method_name;
         methods_name.add(method_name);
       }

       //finally insert the class name and its methods name to hash table
       instance.insertMethods(class_name, methods_name);
       return super.visit(n, argu);
   }

   @Override
   public Object visit(VarDeclaration n, Object argu) {
       return super.visit(n, argu);
   }

   @Override
   public Object visit(MethodDeclaration n, Object argu) {
     MethodTable instance = MethodTable.getInstance();
     String cur_class = instance.cur_class;
     String cur_method = (String)n.f2.accept(this,argu);
     instance.cur_method = cur_method;  //set cur method

     //insert parameters list
     if(n.f4.node != null) {
       HashMap<String, String> paras_list = (HashMap<String,String>)n.f4.node.accept(this,argu);
       if(paras_list != null)
          instance.types.putAll(paras_list);
     }

     //insert locals
     Vector<Node> vec = n.f7.nodes;
     Iterator it = vec.iterator();
     while(it.hasNext()) {
       VarDeclaration local = (VarDeclaration)it.next();
       String ln = (String)local.f1.accept(this,argu);
       String tn = (String)local.f0.accept(this,argu);

       if(local.f0.f0.choice instanceof Identifier && !tn.equals(cur_class)) {
         String name = cur_class + "." + cur_method + "." + ln;
         instance.types.put(name, tn);
       }
     }

     return super.visit(n, argu);
   }

   @Override
   public Object visit(FormalParameterList n, Object argu) {
     HashMap<String, String> paras = new HashMap<>();
     HashMap<String, String> first_para = (HashMap<String, String>)n.f0.accept(this,argu);
     if(first_para != null)
        paras.putAll(first_para);

     Vector<Node> vec = n.f1.nodes;
     Iterator it = vec.iterator();
     while(it.hasNext()) {
       FormalParameterRest para_rest = (FormalParameterRest)it.next();
       HashMap<String, String> parameter = (HashMap<String,String>)para_rest.f1.accept(this,argu);
       if(parameter!=null)
          paras.putAll(parameter);
     }

     return paras;
     //return super.visit(n, argu);
   }

   @Override
   public Object visit(FormalParameter n, Object argu) {
       MethodTable instance = MethodTable.getInstance();
       String cur_class = instance.cur_class;
       String cur_method = instance.cur_method;
       HashMap<String, String> para = new HashMap<>();
       String pn = (String) n.f1.accept(this,argu);
       String tn = (String) n.f0.accept(this,argu);
       if(n.f0.f0.choice instanceof Identifier && !tn.equals(cur_class)) {
         String name = cur_class + "." + cur_method + "." + pn;
         para.put(name, tn);
         return para;
       }

       return super.visit(n, argu);
   }

   @Override
   public Object visit(FormalParameterRest n, Object argu) {
       return n.f1.accept(this, argu);
   }

   /**
    * f0 -> ArrayType()
    *       | BooleanType()
    *       | IntegerType()
    *       | Identifier()
    */
   @Override
   public Object visit(Type n, Object argu) {
       return n.f0.choice.accept(this, argu);
   }

   @Override
   public Object visit(ArrayType n, Object argu) {
       return super.visit(n, argu);
   }

   @Override
   public Object visit(BooleanType n, Object argu) {
       return n.f0.accept(this, argu);
   }

   @Override
   public Object visit(IntegerType n, Object argu) {
       return n.f0.accept(this, argu);
       //return super.visit(n, argu);
   }

   @Override
   public Object visit(Statement n, Object argu) {
       //System.out.println(n.f0.choice);
       return n.f0.choice.accept(this, argu);
       //return super.visit(n, argu);
   }

   @Override
   public Object visit(Block n, Object argu) {
       return super.visit(n, argu);
   }

   /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
   @Override
   public Object visit(AssignmentStatement n, Object argu) {
       return super.visit(n, argu);
   }

   @Override
   public Object visit(ArrayAssignmentStatement n, Object argu) {
       return super.visit(n, argu);
   }

   @Override
   public Object visit(IfStatement n, Object argu) {
       return super.visit(n, argu);
   }

   @Override
   public Object visit(WhileStatement n, Object argu) {
       return super.visit(n, argu);
   }

   @Override
   public Object visit(PrintStatement n, Object argu) {
       return n.f2.f0.choice.accept(this, argu);


       //return super.visit(n, argu);
   }

   @Override
   public Object visit(Expression n, Object argu) {
       //System.out.println(n.f0.choice.accept(this,argu));
       return n.f0.accept(this,argu);
       //return n.f0.choice.accept(this, argu);
   }

   @Override
   public Object visit(AndExpression n, Object argu) {
       return super.visit(n, argu);
   }

   @Override
   public Object visit(CompareExpression n, Object argu) {
       return super.visit(n, argu);
   }

   @Override
   public Object visit(PlusExpression n, Object argu) {
       return super.visit(n, argu);
   }

   @Override
   public Object visit(MinusExpression n, Object argu) {
       return super.visit(n, argu);
   }

   @Override
   public Object visit(TimesExpression n, Object argu) {
       return super.visit(n, argu);
   }

   @Override
   public Object visit(ArrayLookup n, Object argu) {
       return super.visit(n, argu);
   }

   @Override
   public Object visit(ArrayLength n, Object argu) {
       return super.visit(n, argu);
   }

   @Override
   public Object visit(MessageSend n, Object argu) {
       return super.visit(n, argu);
   }

   @Override
   public Object visit(ExpressionList n, Object argu) {
       return n.f0.accept(this,argu);
       //return super.visit(n, argu);
   }

   @Override
   public Object visit(ExpressionRest n, Object argu) {
       return super.visit(n, argu);
   }

   @Override
   public Object visit(PrimaryExpression n, Object argu) {
       return n.f0.choice.accept(this, argu);
   }

   @Override
   public Object visit(IntegerLiteral n, Object argu) {
       return new Pair(n.f0.tokenImage, null);
       //return new IntegerType();
       //return super.visit(n, argu);
   }

   @Override
   public Object visit(TrueLiteral n, Object argu) {
       return new Pair(n.f0.tokenImage, null);
       //return new BooleanType();
       //return super.visit(n, argu);
   }

   @Override
   public Object visit(FalseLiteral n, Object argu) {
       return new Pair(n.f0.tokenImage, null);
       //return new BooleanType();
       //return super.visit(n, argu);
   }

   @Override
   public Object visit(Identifier n, Object argu) {
       //return super.visit(n, argu);
       return n.f0.tokenImage;
   }

   @Override
   public Object visit(ThisExpression n, Object argu) {
       //return new ThisExpression();
       return super.visit(n, argu);
   }

   @Override
   public Object visit(ArrayAllocationExpression n, Object argu) {
       //return n.f1.accept(this, argu);
       return super.visit(n, argu);
   }

   @Override
   public Object visit(AllocationExpression n, Object argu) {
       //System.out.println(n.f1.f0.accept(this, argu));
       return n.f1.f0.accept(this, argu);
   }

   @Override
   public Object visit(NotExpression n, Object argu) {
       return super.visit(n, argu);
   }

   @Override
   public Object visit(BracketExpression  n, Object argu) {
       //System.out.println(n.f1.f0.choice.getClass());
       return n.f1.f0.choice.accept(this, argu);
   }
}
