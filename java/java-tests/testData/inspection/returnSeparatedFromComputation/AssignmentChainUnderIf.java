class T {
  int x;
  int y;

  int f(int a) {
    int n = -1;
    if (a != 0) {
      n = a;
      n = 31 * x + n;
      n = 31 * y + n;
    }
    return n;
  }
}