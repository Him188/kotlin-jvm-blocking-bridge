package compiler.unit

internal sealed class UnitCoercionTest(ir: Boolean) : AbstractUnitCoercionTest(ir) {
    class Ir : UnitCoercionTest(true)
    class Jvm : UnitCoercionTest(false)
}

