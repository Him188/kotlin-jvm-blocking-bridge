import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class J {
    public static void main(String[] args) {
        String s = new TestRet().test(); // should report deprecation


        var x = new AClass();
        x.member();

        AClass.Companion.f();

        AClass.comp(); // should report deprecation
        AClass.comp(null);

        AClass.P.f();
        try {
            AClass.P.compThrows();
        } catch (IOException ignored) {

        }

        AClass.f();

        x.overloads();
        x.overloads(1);
        x.member();
        x.overloads(1, "");

        AnnotationOnInterface i = new AnnotationOnInterface() {

            @Nullable
            @Override
            public Object suspend(@NotNull Continuation<? super Unit> $completion) {
                return null;
            }

            @Override
            public void nonSuspend() {
            }
        };
        i.nonSuspend();
        i.suspend();

        new AClass() {
            @Nullable
            @Override
            public Object member(@NotNull Continuation<? super Unit> $completion) {
                return super.member($completion);
            }

            @Nullable
            @Override
            public void overloads() {
                super.overloads();
            }

            @Nullable
            @Override
            public void member() {
                super.member();
            }
        };
    }
}