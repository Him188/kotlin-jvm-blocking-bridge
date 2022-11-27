import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class CallsFromJava {
    public static void main(String[] args) {
        testDeprecated();
        testReturnValue();
        testThrows();
        testInheritance();
        testSuspendStatic();
        testJvmOverloads();
        testSynthetic();
        testVisibility();
    }

    private static void testVisibility() {
        TestEffectivePublic.INSTANCE.effectivePublic(null); // original
        TestEffectivePublic.INSTANCE.effectivePublic(); // `@PublishedApi internal` in Kotlin, should be ok.
        TestEffectivePublic.INSTANCE.privateFun(); // private in Kotlin, forbid.
    }

    private static void testReturnValue() {
        Object any = new AClass().suspendMember(); // return value must be void (error here)
        String b = new TestRet().suspendMember(); // return value must be String
    }

    private static void testDeprecated() {
        String s = new TestRet().deprecatedSuspendMember(); // should report deprecation, return value must be String
    }

    private static void testSuspendStatic() {
        AClass.Companion.ordinaryStaticInCompanion();

        AClass.ordinaryStaticInCompanion();
        AClass.suspendStaticInCompanion(); // should report deprecation
        AClass.suspendStaticInCompanion(null); // original suspend function should still be callable
    }

    private static void testThrows() {
        try {
            AClass.P.suspendThrowsInObject();
        } catch (IOException ignored) {
        }
    }

    private static void testInheritance() {
        new AClass() {
            // should be able to override original suspend function
            @Nullable
            @Override
            public Object suspendMember(@NotNull Continuation<? super Unit> $completion) {
                return super.suspendMember($completion);
            }

            // should be able to override generated overload
            @Override
            public void overloads() {
                super.overloads();
            }

            // should be able to override generated bridge
            @Override
            public void suspendMember() {
                super.suspendMember();
            }
        };
    }

    private static void testSynthetic() {
        AnnotationOnInterface i = new AnnotationOnInterface() {
        };

        i.suspendMember(); // allow calls to suspend members
        i.synthetic(); // forbid calls to synthetic functions
    }

    private static void testJvmOverloads() {
        AClass x = new AClass();

        x.overloads(1, "", null); // original suspend function
        x.overloads(1, (Continuation<? super Unit>) null); // original suspend function, by JvmOverloads
        x.overloads(null); // original suspend function, by JvmOverloads

        x.overloads(1); // source-level non-suspend overload, should clash for now.
        x.overloads(1, ""); // should be ok
        x.overloads(1); // should be ok
        x.overloads(); // should be ok

        x.overloadsClash(1, "", null); // original suspend function
        x.overloadsClash(1); // source-level non-suspend overload, should clash for now.
        x.overloadsClash(1, ""); // Should be ok
        x.overloadsClash(1, ""); // Should be ok
    }
}