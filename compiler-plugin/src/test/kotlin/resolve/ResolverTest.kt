package resolve

import jvm.testJvmCompile

internal class ResolverTest {


    fun `test compiler resolver`() = testJvmCompile(
        """
        class TestData
    """,
        """
class TestJava {

}
        """
    )

}