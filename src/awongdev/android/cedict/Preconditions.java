package awongdev.android.cedict;

public class Preconditions {
  public static void NotNull(Object o, String msg) {
	  if (o == null) {
		  throw new RuntimeException("Expected non-null object: " + msg);
	  }
  }
  
  public static void IsNull(Object o, String msg) {
	  if (o != null) {
		  throw new RuntimeException("Expected null object: " + msg);
	  }
  }
  
  public static void IsEquals(Object o1, Object o2) {
	  if (!o1.equals(o2)) {
		  throw new RuntimeException("Expected " + o1 + " equals " + o2);
	  }
  }
}
