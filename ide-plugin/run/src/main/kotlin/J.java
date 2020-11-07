public class J {
    public static void main(String[] args) {
        var x = new AClass();
        x.member();

        AClass.Companion.f();
        AClass.comp();

        AClass.P.f();
        AClass.P.comp();

        AClass.f();
    }
}