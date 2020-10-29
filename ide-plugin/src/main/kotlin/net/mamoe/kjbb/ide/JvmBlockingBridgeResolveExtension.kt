package net.mamoe.kjbb.ide

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension

@AutoService(SyntheticResolveExtension::class)
class JvmBlockingBridgeResolveExtension : SyntheticResolveExtension {
    override fun getPossibleSyntheticNestedClassNames(thisDescriptor: ClassDescriptor): List<Name>? {
        return super.getSyntheticNestedClassNames(thisDescriptor)
    }
}