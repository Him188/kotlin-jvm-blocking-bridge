package compiler

import org.junit.jupiter.api.Test

internal class WithJavaTest : AbstractCompilerTest() {
    @Test
    fun `member function in class with Java`() = testJvmCompile(
        """
        //package test
        object TestData {
            @JvmBlockingBridge
            suspend fun test() = "OK"
            
            fun main() = TestDataJ.main()
        }
    """,
        """
        //package test;
        //import TestDataKt;
        public class TestDataJ {
            
            public static String main() {
                return TestData.INSTANCE.test();
            }
        }
    """
    )

    @Test
    fun `static function in class from companion with Java`() = testJvmCompile(
        """
        //package test
        class TestData {
            companion object {
                @JvmBlockingBridge
                @JvmStatic
                suspend fun test() = "OK"
            }
            
            fun main() = TestDataJ.main()
        }
    """,
        """
        //package test;
        //import TestDataKt;
        public class TestDataJ {
            
            public static String main() {
                return TestData.test(); // static
            }
        }
    """
    )

    @Test
    fun `member function in class companion with Java`() = testJvmCompile(
        """
        //package test
        class TestData {
            companion object {
                @JvmBlockingBridge
                @JvmStatic
                suspend fun test() = "OK"
            }
            
            fun main() = TestDataJ.main()
        }
    """,
        """
        //package test;
        //import TestDataKt;
        public class TestDataJ {
            
            public static String main() {
                return TestData.Companion.test(); // member
            }
        }
    """
    )

    @Test
    fun `static function in interface from companion with Java`() = testJvmCompile(
        """
        //package test
        interface TestData {
            companion object {
                @JvmBlockingBridge
                @JvmStatic
                suspend fun test() = "OK"
            
                fun main() = TestDataJ.main()
            }
        }
    """,
        """
        //package test;
        //import TestDataKt;
        public class TestDataJ {
            
            public static String main() {
                return TestData.test(); // static
            }
        }
    """
    )

    @Test
    fun `member function in interface companion with Java`() = testJvmCompile(
        """
        //package test
        interface TestData {
            companion object {
                @JvmBlockingBridge
                @JvmStatic
                suspend fun test() = "OK"
            
                fun main() = TestDataJ.main()
            }
        }
    """,
        """
        //package test;
        //import TestDataKt;
        public class TestDataJ {
            
            public static String main() {
                return TestData.Companion.test(); // member
            }
        }
    """
    )
}