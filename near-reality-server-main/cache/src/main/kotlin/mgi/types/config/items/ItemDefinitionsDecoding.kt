package mgi.types.config.items

import mgi.utilities.ByteBuffer

object ItemDefinitionsDecoding {

    @JvmStatic
    fun ItemDefinitions.decodeOpcode(buffer: ByteBuffer, opcode: Int) {
        when (opcode) {
            1 -> inventoryModelId = buffer.readUnsignedShort()
            2 -> {
                name = buffer.readString()
                lowercaseName = name.lowercase()
            }
            3 -> examine = buffer.readString()
            4 -> zoom = buffer.readUnsignedShort()
            5 -> modelPitch = buffer.readUnsignedShort()
            6 -> modelRoll = buffer.readUnsignedShort()
            7 -> offsetX = buffer.readShort()
            8 -> offsetY = buffer.readShort()
            9 -> buffer.readString()
            11 -> isStackable = 1
            12 -> price = buffer.readInt()
            13 -> wearPos1 = buffer.readByte().toInt()
            14 -> wearPos2 = buffer.readByte().toInt()
            16 -> isMembers = true
            23 -> {
                primaryMaleModel = buffer.readUnsignedShort()
                maleOffset = buffer.readUnsignedByte()
            }
            24 -> secondaryMaleModel = buffer.readUnsignedShort()
            25 -> {
                primaryFemaleModel = buffer.readUnsignedShort()
                femaleOffset = buffer.readUnsignedByte()
            }
            26 -> secondaryFemaleModel = buffer.readUnsignedShort()
            27 -> wearPos3 = buffer.readByte().toInt()
            in 30..34 -> groundOptions[opcode - 30] = buffer.readGroundOption()
            in 35..39 -> inventoryOptions[opcode - 35] = buffer.readString()
            40 -> {
                val amount = buffer.readUnsignedByte()
                originalColours = ShortArray(amount)
                replacementColours = ShortArray(amount)
                for (index in 0 until amount) {
                    originalColours[index] = buffer.readUnsignedShort().toShort()
                    replacementColours[index] = buffer.readUnsignedShort().toShort()
                }
            }
            41 -> {
                val amount = buffer.readUnsignedByte()
                originalTextureIds = ShortArray(amount)
                replacementTextureIds = ShortArray(amount)
                for (index in 0 until amount) {
                    originalTextureIds[index] = buffer.readUnsignedShort().toShort()
                    replacementTextureIds[index] = buffer.readUnsignedShort().toShort()
                }
            }
            42 -> shiftClickIndex = buffer.readByte().toInt()
            43 -> readSubOps(buffer)
            // --- Rev-239 opcodes (verified empirically against OpenRS2 cache 2615: all 33,736 item files parse) ---
            15 -> {}  // rev-239 flag, no payload
            44 -> inventoryModelId = buffer.readInt()          // replaces opcode 1 (32-bit model ids)
            45 -> {                                            // replaces opcode 23
                primaryMaleModel = buffer.readInt()
                maleOffset = buffer.readUnsignedByte()
            }
            46 -> secondaryMaleModel = buffer.readInt()        // replaces opcode 24
            47 -> tertiaryMaleModel = buffer.readInt()         // replaces opcode 78
            48 -> {                                            // replaces opcode 25
                primaryFemaleModel = buffer.readInt()
                femaleOffset = buffer.readUnsignedByte()
            }
            49 -> secondaryFemaleModel = buffer.readInt()      // replaces opcode 26
            50 -> tertiaryFemaleModel = buffer.readInt()       // replaces opcode 79
            51 -> primaryMaleHeadModelId = buffer.readInt()    // replaces opcode 90
            52 -> secondaryMaleHeadModelId = buffer.readInt()  // replaces opcode 92
            53 -> primaryFemaleHeadModelId = buffer.readInt()  // replaces opcode 91
            54 -> secondaryFemaleHeadModelId = buffer.readInt()// replaces opcode 93
            160 -> {} // rev-239 flag, no payload
            65 -> grandExchange = true
            75 -> cacheWeight = buffer.readShort()
            78 -> tertiaryMaleModel = buffer.readUnsignedShort()
            79 -> tertiaryFemaleModel = buffer.readUnsignedShort()
            90 -> primaryMaleHeadModelId = buffer.readUnsignedShort()
            91 -> primaryFemaleHeadModelId = buffer.readUnsignedShort()
            92 -> secondaryMaleHeadModelId = buffer.readUnsignedShort()
            93 -> secondaryFemaleHeadModelId = buffer.readUnsignedShort()
            94 -> category = buffer.readUnsignedShort() // category
            95 -> modelYaw = buffer.readUnsignedShort()
            97 -> notedId = buffer.readUnsignedShort()
            98 -> notedTemplate = buffer.readUnsignedShort()
            in 100..109 -> {
                if (stackIds == null) {
                    stackIds = IntArray(10)
                    stackAmounts = IntArray(10)
                }
                stackIds[opcode - 100] = buffer.readUnsignedShort()
                stackAmounts[opcode - 100] = buffer.readUnsignedShort()
            }
            110 -> resizeX = buffer.readUnsignedShort()
            111 -> resizeY = buffer.readUnsignedShort()
            112 -> resizeZ = buffer.readUnsignedShort()
            113 -> ambient = buffer.readByte().toInt()
            114 -> contrast = buffer.readByte().toInt() * 5
            115 -> teamId = buffer.readUnsignedByte()
            139 -> bindId = buffer.readUnsignedShort()
            140 -> bindTemplateId = buffer.readUnsignedShort()
            148 -> placeholderId = buffer.readUnsignedShort()
            149 -> placeholderTemplate = buffer.readUnsignedShort()
            // --- Rev-239 opcodes (rsmod reference) ---
            200 -> {
                // objvar array
                val count = buffer.readUnsignedByte()
                for (i in 0 until count) {
                    buffer.readUnsignedShort()
                }
            }
            201 -> buffer.readInt()    // playerCost
            202 -> buffer.readInt()    // playerCostDerived
            203 -> buffer.readInt()    // playerCostDerivedConst
            204 -> buffer.readUnsignedShort() // stockMarketBuyLimit
            205 -> buffer.readUnsignedShort() // stockMarketRecalcUsers
            206 -> {} // tradeable = false (flag only)
            207 -> buffer.readUnsignedShort() // respawnRate
            208 -> buffer.readByte()   // dummyitem
            209 -> buffer.readUnsignedShort() // contentGroup
            210 -> buffer.readUnsignedShort() // transformlink
            211 -> buffer.readUnsignedShort() // transformtemplate
            212 -> buffer.readUnsignedByte()  // weaponCategory
            249 -> parameters = buffer.readParameters()
        }
    }

    private fun readSubOps(buffer: ByteBuffer) {
        val opId: Int = buffer.readUnsignedByte()
        while (true) {
            val subopId: Int = buffer.readUnsignedByte() - 1
            if (subopId == -1) {
                break
            }
            val op: String = buffer.readString()
        }

    }

    private fun ByteBuffer.readGroundOption() = readString()
        .takeIf { !it.equals("Hidde", ignoreCase = true) }

}
