// "Move 'return' closer to computation of the value of 's'" "true"
class T {
    int f(String a) {
        String s = a;
        if (s == null) {
            s = ""; // end of line
        }
        else if (s.startsWith("@")) {
            s = s.substring(1);
        }
        else if (s.startsWith("#")) {
            s = "#"; /* inline */
        }
        ret<caret>urn s; // return comment
    }
}