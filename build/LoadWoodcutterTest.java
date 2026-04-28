public class LoadWoodcutterTest {
  public static void main(String[] args) throws Exception {
    try {
      Class<?> c = Class.forName("reason.woodcutter.ReasonWoodcutterLiveBot");
      Object o = c.getDeclaredConstructor().newInstance();
      System.out.println("LOADED:" + o.getClass().getName());
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }
}
