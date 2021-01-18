package net.mamoe.kjbb.ide

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension

class JvmBlockingBridgeResolveExtension : SyntheticResolveExtension {
    override fun getPossibleSyntheticNestedClassNames(thisDescriptor: ClassDescriptor): List<Name>? {
        return super.getSyntheticNestedClassNames(thisDescriptor)
    }
}