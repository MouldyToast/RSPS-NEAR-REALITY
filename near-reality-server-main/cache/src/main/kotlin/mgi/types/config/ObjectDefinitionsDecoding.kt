package mgi.types.config


import mgi.utilities.ByteBuffer

object ObjectDefinitionsDecoding {
    @JvmStatic
    fun ObjectDefinitions.decodeOpcode(buffer: ByteBuffer, opcode: Int, oldVersion: Boolean) {
        when (opcode) {
            1 -> {
                val size = buffer.readUnsignedByte()
                if(size > 0) {
                    types = IntArray(size)
                    models = IntArray(size)
                    for (i in 0 until size) {
                        models[i] = buffer.readUnsignedShort()
                        types[i] = buffer.readUnsignedByte()
                    }
                }
            }
            2 -> name = buffer.readString()
            3 -> buffer.readString() // desc/examine text (added rev-239)
            // --- Rev-239 opcodes (verified empirically against OpenRS2 cache 2615: all 62,193 object files parse) ---
            6 -> {
                // models + types with 32-bit model ids (replaces opcode 1)
                val size = buffer.readUnsignedByte()
                if(size > 0) {
                    types = IntArray(size)
                    models = IntArray(size)
                    for (i in 0 until size) {
                        models[i] = buffer.readInt()
                        types[i] = buffer.readUnsignedByte()
                    }
                }
            }
            7 -> {
                // models with 32-bit ids, no types (replaces opcode 5)
                val size = buffer.readUnsignedByte()
                if(size > 0) {
                    types = null
                    models = IntArray(size)
                    for (i in 0 until size) {
                        models[i] = buffer.readInt()
                    }
                }
            }
            93 -> { buffer.readUnsignedShort(); buffer.readUnsignedShort(); buffer.readUnsignedShort() } // rev-239, unknown 6-byte field
            95, 96 -> buffer.readUnsignedByte() // rev-239, unknown 1-byte fields
            100 -> {
                // rev-239 travel-menu group header (fairy rings / spirit trees): {u8, u8, string}
                buffer.readUnsignedByte(); buffer.readUnsignedByte(); buffer.readString()
            }
            101 -> {
                // rev-239 travel-menu entry: {u8, u16, u16, i32, i32, string}
                buffer.readUnsignedByte(); buffer.readUnsignedShort(); buffer.readUnsignedShort()
                buffer.readInt(); buffer.readInt(); buffer.readString()
            }
            102 -> {
                // rev-239 travel-menu sub-entry: {u8, u16, u16, u16, i32, i32, string}
                buffer.readUnsignedByte(); buffer.readUnsignedShort(); buffer.readUnsignedShort()
                buffer.readUnsignedShort(); buffer.readInt(); buffer.readInt(); buffer.readString()
            }
            5 -> {
                val size = buffer.readUnsignedByte()
                if(size > 0) {
                    types = null
                    models = IntArray(size)
                    for (i in 0 until size) {
                        models[i] = buffer.readUnsignedShort()
                    }
                }
            }
            14 -> sizeX = buffer.readUnsignedByte()
            15 -> sizeY = buffer.readUnsignedByte()
            17 -> { clipType = 0; isProjectileClip = false }
            18 -> isProjectileClip = false
            19 -> optionsInvisible = buffer.readUnsignedByte()
            21 -> contouredGround = 0
            22 -> nonFlatShading = true
            23 -> modelClipped = true
            24 -> animationId = buffer.readUnsignedShortNo65535()
            27 -> clipType = 1
            28 -> decorDisplacement = buffer.readUnsignedByte()
            29 -> ambient = buffer.readByte().toInt()
            in 30..34 -> {
                options[opcode - 30] = buffer.readString()
                if (options[opcode - 30].equals("Hidden", ignoreCase = true)) {
                    options[opcode - 30] = null
                }
            }
            39 -> contrast = buffer.readByte() * 25
            40 -> {
                val size = buffer.readUnsignedByte()
                modelColours = IntArray(size)
                replacementColours = IntArray(size)
                for (count in 0 until size) {
                    modelColours[count] = buffer.readUnsignedShort()
                    replacementColours[count] = buffer.readUnsignedShort()
                }
            }
            41 -> {
                val size = buffer.readUnsignedByte()
                modelTexture = ShortArray(size)
                replacementTexture = ShortArray(size)
                for (count in 0 until size) {
                    modelTexture[count] = buffer.readUnsignedShort().toShort()
                    replacementTexture[count] = buffer.readUnsignedShort().toShort()
                }
            }
            61 -> buffer.readUnsignedShort()
            62 -> isRotated = true
            64 -> clipped = false
            65 -> modelSizeX = buffer.readUnsignedShort()
            66 -> modelSizeHeight = buffer.readUnsignedShort()
            67 -> modelSizeY = buffer.readUnsignedShort()
            68 -> mapSceneId = buffer.readUnsignedShort()
            69 -> accessBlockFlag = buffer.readUnsignedByte()
            70 -> offsetX = buffer.readUnsignedShort()
            71 -> offsetHeight = buffer.readUnsignedShort()
            72 -> offsetY = buffer.readUnsignedShort()
            73 -> obstructsGround = true
            74 -> hollow = true
            75 -> supportItems = buffer.readUnsignedByte()
            77, 92 -> {
                varbit = buffer.readUnsignedShortNo65535()
                varp = buffer.readUnsignedShortNo65535()
                finalTransformation = -1
                if (opcode == 92) {
                    finalTransformation = buffer.readUnsignedShortNo65535()
                }
                val size = buffer.readUnsignedByte()
                transformedIds = IntArray(size + 2)
                for (index in 0..size) {
                    transformedIds[index] = buffer.readUnsignedShortNo65535()
                }
                transformedIds[size + 1] = finalTransformation
                return
            }
            78 -> {
                ambientSoundId = buffer.readUnsignedShort()
                ambientSoundDistance = buffer.readUnsignedByte()
                if(!oldVersion)
                    ambientSoundRetain = buffer.readUnsignedByte()
            }
            79 -> {
                anInt456 = buffer.readUnsignedShort()
                anInt457 = buffer.readUnsignedShort()
                ambientSoundDistance = buffer.readUnsignedByte()
                if(!oldVersion)
                    ambientSoundRetain = buffer.readUnsignedByte()
                val size = buffer.readUnsignedByte()
                anIntArray100 = IntArray(size)
                for (count in 0 until size) {
                    anIntArray100[count] = buffer.readUnsignedShort()
                }
            }
            81 -> contouredGround = buffer.readUnsignedByte() * 256
            82 -> mapIconId = buffer.readUnsignedShort()
            89 -> return
            // --- Rev-239 opcodes (rsmod reference) ---
            90 -> {} // fixLocAnimAfterLocChange = true (flag only)
            200 -> buffer.readUnsignedShort() // contentGroup
            249 -> buffer.readParameters()
        }
    }
}
