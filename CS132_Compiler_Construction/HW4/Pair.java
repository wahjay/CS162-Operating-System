import java.util.HashMap;
import java.util.Map;
import java.util.*;

public class Pair {
  String code;
  String result;

  public Pair() {
    code = null;
    result = null;
  }

  public Pair(String c, String vs) {
    code = c;
    result = vs;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = hash * 31 + (result == null ? 0 : result.length());
    hash = hash * 31 + (code == null ? 0 : code.length());
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    Pair p = (Pair) obj;
    return p.code == this.code && p.result == this.result;
  }
}
