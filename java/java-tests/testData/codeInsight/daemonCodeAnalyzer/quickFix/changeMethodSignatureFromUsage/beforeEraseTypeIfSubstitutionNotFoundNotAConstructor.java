// "Add Foo<TypeParamName> as 1 parameter to method bar" "true"
 public class Bar {
     static void bar(String args) {

     }
 }

class Foo<TypeParamName> {
    void bar() {
      Bar.bar(th<caret>is, "");
    }
}