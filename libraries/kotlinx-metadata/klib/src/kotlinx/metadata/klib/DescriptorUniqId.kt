package kotlinx.metadata.klib

import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf

class DescriptorUniqId(val index: Long)

fun DescriptorUniqId.write(): KlibMetadataProtoBuf.DescriptorUniqId.Builder =
        KlibMetadataProtoBuf.DescriptorUniqId.newBuilder().apply {
            index = this@write.index
        }