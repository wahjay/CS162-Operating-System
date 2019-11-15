import syntaxtree.*;
import java.util.HashMap;
import java.util.Map;
import java.util.*;


class MethodTable {
  HashMap<String, List<String>> methodTable;  //store object methods
  HashMap<String, List<String>> fields;       //store object fields

  //store local object name and its type
  //tells me which method to call
  HashMap<String, String> types;

  private static MethodTable instance;
  public static String cur_class;  //store current class scope
  public static String new_class;  //used for object allocation
  public static String cur_method; //store current method scope
  public static String pre_method; //store previous method scope

  private MethodTable() {
    methodTable = new HashMap<>();
    fields = new HashMap<>();
    types = new HashMap<>();
  }

  public static MethodTable getInstance() {
    if(instance == null)
      instance = new MethodTable();

    return instance;
  }

  public String getType(String n) {
    return types.get(n);
  }

  public void insertTypes(String n, String t) {
    types.put(n, t);
  }

  //get the object size
  public int getOSize(String cn) {
    List<String> fields_ = fields.get(cn);
    return (fields_.size() + 1) * 4;
  }


  public int getFPosition(String cn, String fn) {
    List<String> fields_ = fields.get(cn);

    //if fields_ is null, we are probably in the main class
    //or the field name is simply just a integer literal
    if(fields_ == null)
      return -1;

    String field = cn + "." + fn;
    int pos = fields_.indexOf(field);
    if(pos == -1)
      return -1;
    return (pos + 1) * 4;
  }

  public int getMPosition(String cn, String mn) {
    List<String> methods = methodTable.get(cn);
    String method = cn + "." + mn;
    int pos = methods.indexOf(method);
    if(pos == -1)
      return -1;
    return pos * 4;
  }

  public void insertFields(String cn, List<String> fn) {
    fields.put(cn, fn);
  }

  public List<String> getFields(String cn) {
    return fields.get(cn);
  }

  public void insertMethods(String cn, List<String> mn) {
    methodTable.put(cn, mn);
  }

  public List<String> getMethods(String cn) {
    return methodTable.get(cn);
  }

  public void print() {
    for(Map.Entry<String, List<String>> entry : methodTable.entrySet()) {
      List<String> methods = entry.getValue();
      List<String> fields_ = fields.get(entry.getKey());
      System.out.println("Class name: " + entry.getKey());
      for(String field : fields_)
        System.out.println("Field name: " + field);

      for(String method : methods)
        System.out.println("Method name: " + method);

      System.out.println("_______________________________");
    }
  }

  public void printTypes() {
    for(Map.Entry<String, String> entry : types.entrySet()) {
      System.out.println("Var Name: " + entry.getKey());
      System.out.println("Type Name: " + entry.getValue());
    }
  }

}
