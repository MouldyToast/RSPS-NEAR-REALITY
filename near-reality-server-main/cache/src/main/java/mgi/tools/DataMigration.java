package mgi.tools;

import com.zenyte.game.item.ItemId;
import com.zenyte.game.world.region.XTEALoaderPorted;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mgi.tools.jagcached.ArchiveType;
import mgi.tools.jagcached.GroupType;
import mgi.tools.jagcached.cache.Archive;
import mgi.tools.jagcached.cache.Cache;
import mgi.tools.jagcached.cache.File;
import mgi.tools.jagcached.cache.Group;
import mgi.tools.parser.MapChanges;
import mgi.tools.parser.TypeParser;
import mgi.types.Definitions;
import mgi.types.component.SpriteDefaults;
import mgi.types.config.AnimationDefinitions;
import mgi.types.config.HitbarDefinitions;
import mgi.types.config.ObjectDefinitions;
import mgi.types.config.SpotAnimationDefinition;
import mgi.types.config.items.ItemDefinitions;
import mgi.types.config.npcs.NPCDefinitions;
import mgi.utilities.ByteBuffer;
import net.runelite.api.NpcID;
import org.runestar.cs2.type.Value;

import java.util.ArrayList;
import java.util.List;

import static com.zenyte.game.world.entity._Location.getRegionIDByRegion;

public class DataMigration {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DataMigration.class);
    private Cache target;
    private Cache source;
    private boolean portSequencesDirect;
    /**
     * True when the source cache is rev-239+ format. Rev-239 renumbered/restructured opcodes in
     * sequences, NPCs, items, objects, and spotanims (model ids widened to 32 bits under new
     * opcodes), so those types must be decoded with the new format and re-encoded in the old
     * client-compatible format instead of raw-copied.
     */
    private boolean source239;

    public DataMigration(Cache old_cache, Cache new_cache, boolean directSequences) {
        this.target = old_cache;
        this.source = new_cache;
        this.portSequencesDirect = directSequences;
    }

    public DataMigration(Cache old_cache, Cache new_cache, boolean directSequences, boolean source239) {
        this(old_cache, new_cache, directSequences);
        this.source239 = source239;
    }

    private boolean hasArchive(Cache cache, ArchiveType type) {
        return cache.getArchive(type) != null;
    }

    public void run() {
        if (!hasArchive(source, ArchiveType.MODELS)) {
            logger.error("Source cache is missing MODELS archive — skipping migration");
            return;
        }
        portHitbars();
        portModels();
        portUnderlays();
        portOverlays();
        portGraphics();
        portSkins();
        portSkeletons();
        portSequences();
        portTextures();
        generatePrePortSpriteMapping();
        portSprites();
        portNPCs();
        portItems();
        portMaps();
        if (!source239) {
            portWorldMap();
        }
        if(!portSequencesDirect)
            portObjects();
    }

    private static IntArrayList manualModelList = new IntArrayList();
    static {
        manualModelList.addAll(List.of(32362, 32586, 44968, 32585, 32358, 32073, 29622, 44936));
    }

    private void portHitbars() {
        Archive oldConfigs = target.getArchive(ArchiveType.CONFIGS);
        Group oldHitbars = oldConfigs.findGroupByID(GroupType.HITBAR);

        Archive newConfigs = source.getArchive(ArchiveType.CONFIGS);
        Group newHitbars = newConfigs.findGroupByID(GroupType.HITBAR);

        int live_count = newHitbars.fileCount();
        int old_count = oldHitbars.fileCount();

        logger.info("Porting hitbars index with new group ct " + live_count + " into old group ct " + old_count);

        int port_index = old_count;
        int successful_ports = 0;

        while(port_index < live_count) {
            File file = newHitbars.findFileByID(port_index);
            if(file == null) {
                port_index++;
                continue;
            }
            HitbarDefinitions definitions = new HitbarDefinitions(file.getID(), file.getData());
            MANUAL_SPRITE_GROUPS.put(definitions.getPrimarySprite(), definitions.getPrimarySprite());
            MANUAL_SPRITE_GROUPS.put(definitions.getSecondarySprite(), definitions.getSecondarySprite());
            oldHitbars.addFile(file);
            successful_ports++;
            port_index++;
        }

        logger.info("Successfully ported " + successful_ports + " hitbars from latest OSRS cache");
    }


    private void portObjects() {
        Archive oldConfigs = target.getArchive(ArchiveType.CONFIGS);
        Group oldObjects = oldConfigs.findGroupByID(GroupType.OBJECT);

        Archive newConfigs = source.getArchive(ArchiveType.CONFIGS);
        Group newObjects = newConfigs.findGroupByID(GroupType.OBJECT);

        int live_count = newObjects.fileCount();
        int old_count = oldObjects.fileCount();

        logger.info("Porting objects index with new group ct " + live_count + " into old group ct " + old_count);

        int port_index = old_count;
        int successful_ports = 0;

        while(port_index < live_count) {
            File file = newObjects.findFileByID(port_index);
            if(file == null) {
                port_index++;
                continue;
            }
            if (source239) {
                // Rev-239 object format has new opcodes (6/7 = 32-bit models, etc.) the rev-211
                // client cannot read; decode with the new format and re-encode in the old format.
                ObjectDefinitions definitions = new ObjectDefinitions(file.getID(), file.getData(), true);
                oldObjects.addFile(new File(file.getID(), definitions.encode()));
            } else {
                oldObjects.addFile(file);
            }
            successful_ports++;
            port_index++;
        }

        logger.info("Successfully ported " + successful_ports + " objects from latest OSRS cache");
    }

    private void portUnderlays() {
        Archive oldConfigs = target.getArchive(ArchiveType.CONFIGS);
        Group oldUnderlays = oldConfigs.findGroupByID(GroupType.UNDERLAY);

        Archive newConfigs = source.getArchive(ArchiveType.CONFIGS);
        Group newUnderlays = newConfigs.findGroupByID(GroupType.UNDERLAY);

        int live_count = newUnderlays.fileCount();
        int old_count = oldUnderlays.fileCount();

        logger.info("Porting underlays index with new group ct " + live_count + " into old group ct " + old_count);

        int port_index = old_count;
        int successful_ports = 0;

        while(port_index < live_count) {
            File file = newUnderlays.findFileByID(port_index);
            if(file == null) {
                port_index++;
                continue;
            }
            oldUnderlays.addFile(file);
            successful_ports++;
            port_index++;
        }

        logger.info("Successfully ported " + successful_ports + " underlays from latest OSRS cache");
    }

    private void portOverlays() {
        Archive oldConfigs = target.getArchive(ArchiveType.CONFIGS);
        Group oldOverlays = oldConfigs.findGroupByID(GroupType.OVERLAY);

        Archive newConfigs = source.getArchive(ArchiveType.CONFIGS);
        Group newOverlays = newConfigs.findGroupByID(GroupType.OVERLAY);

        int live_count = newOverlays.fileCount();
        int old_count = oldOverlays.fileCount();

        logger.info("Porting overlays index with new group ct " + live_count + " into old group ct " + old_count);

        int port_index = old_count;
        int successful_ports = 0;

        while(port_index < live_count) {
            File file = newOverlays.findFileByID(port_index);
            if(file == null) {
                port_index++;
                continue;
            }
            oldOverlays.addFile(file);
            successful_ports++;
            port_index++;
        }

        logger.info("Successfully ported " + successful_ports + " overlays from latest OSRS cache");
    }
    public static List<Integer> forcedGFXOverwrites = List.of(2510);
    private void portGraphics() {
        Archive oldConfigs = target.getArchive(ArchiveType.CONFIGS);
        Group oldGraphics = oldConfigs.findGroupByID(GroupType.SPOTANIM);

        Archive newConfigs = source.getArchive(ArchiveType.CONFIGS);
        Group newGraphics = newConfigs.findGroupByID(GroupType.SPOTANIM);

        int live_count = newGraphics.fileCount();
        int old_count = oldGraphics.fileCount();

        logger.info("Porting graphics index with new group ct " + live_count + " into old group ct " + old_count);

        int port_index = old_count;
        int successful_ports = 0;

        while(port_index < live_count) {
            File file = newGraphics.findFileByID(port_index);
            if(file == null) {
                port_index++;
                continue;
            }
            oldGraphics.addFile(transcodeGraphic(file));
            successful_ports++;
            port_index++;
        }

        for(Integer id: forcedGFXOverwrites) {
            File file = newGraphics.findFileByID(id);
            if(file == null) {
                continue;
            }
            oldGraphics.addFile(transcodeGraphic(file));
        }

        logger.info("Successfully ported " + successful_ports + " graphics from latest OSRS cache");
    }

    /**
     * Rev-239 spotanims use opcode 3 (32-bit model id) which the rev-211 client cannot read;
     * decode with the new format and re-encode in the old format. Older sources pass through.
     */
    private File transcodeGraphic(File file) {
        if (!source239) {
            return file;
        }
        SpotAnimationDefinition definition = new SpotAnimationDefinition(file.getID(), file.getData());
        return new File(file.getID(), definition.encode());
    }

    private void portMaps() {
        Archive newMaps = this.source.getArchive(ArchiveType.MAPS);
        Archive oldMaps  = this.target.getArchive(ArchiveType.MAPS);

        int newCount = newMaps.getHighestGroupId();
        int old_count = oldMaps.getHighestGroupId();

        logger.info("Porting map index with new group ct " + newCount + " into old group ct " + old_count);

        int regionX = 0, regionY = 0;
        int successful_ports = 0;
        int failed_ports = 0;

        while(regionX <= 99) {
            regionY = 0;
            while(regionY < 256) {
                if(skipRegion(getRegionIDByRegion(regionX, regionY))) {
                    regionY++;
                    continue;
                }
                try {
                    final int regionID = getRegionIDByRegion(regionX, regionY);
                    final ByteBuffer mapBuffer;
                    final ByteBuffer landBuffer;
                    Group landGroup = null;
                    if (source239) {
                        // Rev-239 map rework: one UNENCRYPTED group per region (group id = region id)
                        // containing file 0 = terrain (old format + a single trailing 0x00 byte),
                        // file 1 = locs (old format, XTEA removed entirely), files 2-4 = new metadata
                        // with no old-format representation. Verified against all 2,934 regions.
                        Group regionGroup = newMaps.findGroupByID(regionID);
                        if (regionGroup == null || regionGroup.fileCount() != 5) {
                            regionY++;
                            continue;
                        }
                        File terrainFile = regionGroup.findFileByID(0);
                        File locsFile = regionGroup.findFileByID(1);
                        mapBuffer = terrainFile == null ? null : terrainFile.getData();
                        landBuffer = locsFile == null ? null : locsFile.getData();
                    } else {
                        int[] keys = XTEALoaderPorted.getXTEAKeys(regionID);
                        landGroup = newMaps.findGroupByName("l" + regionX + "_" + regionY, keys);
                        Group mapGroup = newMaps.findGroupByName("m" + regionX + "_" + regionY);
                        mapBuffer = mapGroup == null ? null : mapGroup.findFileByID(0).getData();
                        landBuffer = landGroup == null ? null : landGroup.findFileByID(0).getData();
                        if (mapBuffer != null && landBuffer != null) {
                            byte[] l_data = MapChanges.modifyRegionData(regionID, landBuffer.getBuffer());
                            landGroup.findFileByID(0).setData(new ByteBuffer(l_data));
                            landGroup.setXTEA(null);
                            oldMaps.addGroup(landGroup);
                            oldMaps.addGroup(mapGroup);
                            successful_ports++;
                        }
                        regionY++;
                        continue;
                    }

                    if (mapBuffer == null || landBuffer == null) {
                        regionY++;
                        continue;
                    }
                    // strip the new single trailing byte from terrain (always 0x00)
                    byte[] m_raw = mapBuffer.getBuffer();
                    byte[] m_data = (m_raw.length > 0 && m_raw[m_raw.length - 1] == 0)
                            ? java.util.Arrays.copyOf(m_raw, m_raw.length - 1) : m_raw;
                    byte[] l_data = MapChanges.modifyRegionData(regionID, landBuffer.getBuffer());
                    putMapGroup(oldMaps, "m" + regionX + "_" + regionY, m_data);
                    putMapGroup(oldMaps, "l" + regionX + "_" + regionY, l_data);
                    successful_ports++;
                    regionY++;
                }   catch (RuntimeException ex) {
                    regionY++;
                    failed_ports++;
                }
            }
            regionX++;
        }

        logger.info("Successfully ported " + successful_ports + " maps from latest OSRS cache, "+ failed_ports + " skipped");
    }

    /**
     * Adds or replaces a named single-file map group ("mX_Y" / "lX_Y") in the target archive.
     * Reuses the existing group's id when the name already exists so addGroup replaces it
     * instead of creating a duplicate name under a fresh id.
     */
    private void putMapGroup(Archive maps, String name, byte[] data) {
        Group existing = null;
        try {
            existing = maps.findGroupByName(name);
        } catch (RuntimeException ignored) {
        }
        File file = new File(0, new ByteBuffer(data));
        Group replacement = existing != null
                ? new Group(existing.getID(), name, 1, file)
                : new Group(name, 1, file);
        maps.addGroup(replacement);
    }

    private boolean skipRegion(int regionIDByRegion) {
        IntArrayList skips = IntArrayList.of(12889,
                13136, 13137, 13138, 13139, 13140, 13141, 13142, 13143, 13144, 13145,
                13392, 13393, 13394, 13395, 13396, 13397, 13398, 13399, 13400, 13401,

                12610, 12611, 12612, 12613,
                12866, 12867, 12868, 12869,
                13122, 13123, 13124, 13125,

                // NR custom map regions — protect from rev-239 overwrite
                13420,  // flower poker / gamble area
                12145,  // middleman area
                13909,  // custom barrows
                6729    // PVM arena
                );
        return skips.contains(regionIDByRegion);
    }

    public Int2IntOpenHashMap MANUAL_SPRITE_GROUPS = new Int2IntOpenHashMap();

    private void generatePrePortSpriteMapping() {
        Archive liveIdx = this.source.getArchive(ArchiveType.DEFAULTS);
        byte[] defaults = liveIdx.findGroupByID(3).findFileByID(0).getData().getBuffer();
        SpriteDefaults defaultData = SpriteDefaults.decode(new ByteBuffer(defaults));
        MANUAL_SPRITE_GROUPS.put(defaultData.mapScenes, 317);
    }


    private void portSprites() {
        Archive liveIdx = this.source.getArchive(ArchiveType.SPRITES);
        Archive oldIdx  = this.target.getArchive(ArchiveType.SPRITES);

        int live_count = liveIdx.getHighestGroupId();
        int old_count = oldIdx.getHighestGroupId();

        logger.info("Porting sprite index with new group ct " + live_count + " into old group ct " + old_count);

        int port_index = old_count;
        int successful_ports = 0;

        while(port_index < live_count) {
            Group group = liveIdx.findGroupByID(port_index);
            if(group == null) {
                port_index++;
                continue;
            }
            oldIdx.addGroup(group);
            successful_ports++;
            port_index++;
        }

        for(Int2IntMap.Entry i: MANUAL_SPRITE_GROUPS.int2IntEntrySet()) {
            Group group = liveIdx.findGroupByID(i.getIntKey());
            group.setID(i.getIntValue());
            oldIdx.addGroup(group);
            logger.warn("Migrated default sprite group {} to {}", i.getIntKey(), i.getIntValue());
        }

        logger.info("Successfully ported " + successful_ports + " sprites from latest OSRS cache");
    }

    private void portTextures() {
        Archive oldConfigs = target.getArchive(ArchiveType.TEXTURES);
        Group oldTextures = oldConfigs.findGroupByID(0);

        Archive newConfigs = source.getArchive(ArchiveType.TEXTURES);
        Group newTextures = newConfigs.findGroupByID(0);

        int live_count = newTextures.getHighestFileId();
        int old_count = oldTextures.getHighestFileId();

        logger.info("Porting textures index with new group ct " + live_count + " into old group ct " + old_count);

        int port_index = old_count;
        int successful_ports = 0;

        while(port_index < live_count) {
            File file = newTextures.findFileByID(port_index);
            if(file == null) {
                port_index++;
                continue;
            }
            if (source239) {
                // Rev-239 flattened texture defs to 7 bytes: {fileId u16, averageRGB u16,
                // opaque u8, animationDirection u8, animationSpeed u8} (single sprite only).
                // The rev-211 client expects the old 12-byte layout, so transcode.
                ByteBuffer in = file.getData();
                int spriteId = in.readUnsignedShort();
                int averageRGB = in.readUnsignedShort();
                int opaque = in.readUnsignedByte();
                int animationDirection = in.readUnsignedByte();
                int animationSpeed = in.readUnsignedByte();
                ByteBuffer out = new ByteBuffer(12);
                out.writeShort(averageRGB);
                out.writeByte(opaque);
                out.writeByte(1); // sprite count
                out.writeShort(spriteId);
                out.writeInt(0);  // per-sprite transform, always 0 for single-sprite textures
                out.writeByte(animationDirection);
                out.writeByte(animationSpeed);
                oldTextures.addFile(new File(file.getID(), out));
            } else {
                oldTextures.addFile(file);
            }
            successful_ports++;
            port_index++;
        }

        logger.info("Successfully ported " + successful_ports + " textures from latest OSRS cache");
    }

    private void portSequences() {
        Archive oldConfigs = target.getArchive(ArchiveType.CONFIGS);
        Group oldSequences = oldConfigs.findGroupByID(GroupType.SEQUENCE);

        Archive newConfigs = source.getArchive(ArchiveType.CONFIGS);
        Group newSequences = newConfigs.findGroupByID(GroupType.SEQUENCE);

        int live_count = newSequences.fileCount();
        int old_count = oldSequences.fileCount();

        logger.info("Porting sequences index with new group ct {} into old group ct {}", live_count, old_count);

        int port_index = old_count;
        int successful_ports = 0;

        while(port_index < live_count) {
            File file = newSequences.findFileByID(port_index);
            if(file == null) {
                port_index++;
                continue;
            }
            if(portSequencesDirect) {
                oldSequences.addFile(file);
            } else {
                AnimationDefinitions def = source239
                        ? AnimationDefinitions.decodeNew239(file.getID(), file.getData())
                        : AnimationDefinitions.decodeNew(file.getID(), file.getData());
                File newFile = new File(file.getID(), def.encode());
                oldSequences.addFile(newFile);
            }
            successful_ports++;
            port_index++;
        }

        logger.info("Successfully ported " + successful_ports + " sequences from latest OSRS cache");
    }

    private void portNPCs() {
        Archive oldConfigs = target.getArchive(ArchiveType.CONFIGS);
        Group oldNpcs = oldConfigs.findGroupByID(GroupType.NPC);

        Archive newConfigs = source.getArchive(ArchiveType.CONFIGS);
        Group newNpcs = newConfigs.findGroupByID(GroupType.NPC);

        int live_count = newNpcs.getHighestFileId();
        int old_count = oldNpcs.getHighestFileId();

        logger.info("Porting npcs index with new group ct " + live_count + " into old group ct " + old_count);

        int port_index = old_count;
        int successful_ports = 0;

        while(port_index < live_count) {
            File file = newNpcs.findFileByID(port_index);
            if(file == null) {
                port_index++;
                continue;
            }
            NPCDefinitions definitions = new NPCDefinitions(file.getID(), file.getData());
            File newFile = new File(file.getID(), definitions.encode());

            oldNpcs.addFile(newFile);
            successful_ports++;
            port_index++;
        }

        logger.info("Successfully ported " + successful_ports + " npcs from latest OSRS cache");
    }

    private void portModels() {
        Archive liveIdx = this.source.getArchive(ArchiveType.MODELS);
        Archive oldIdx  = this.target.getArchive(ArchiveType.MODELS);

        int live_count = liveIdx.getHighestGroupId();
        int old_count = oldIdx.getHighestGroupId();

        logger.info("Porting model index with new group ct " + live_count + " into old group ct " + old_count);

        int port_index = old_count;
        int successful_ports = 0;

        while(port_index < live_count) {
            Group group = liveIdx.findGroupByID(port_index);
            if(group == null) {
                port_index++;
                continue;
            }
            oldIdx.addGroup(group);
            successful_ports++;
            port_index++;
        }


        for(int i: manualModelList) {
            Group group = liveIdx.findGroupByID(i);
            oldIdx.addGroup(group);
        }

        logger.info("Successfully ported " + successful_ports + " models from latest OSRS cache");
    }

    private void portSkeletons() {
        Archive liveIdx = this.source.getArchive(ArchiveType.FRAMES);
        Archive oldIdx  = this.target.getArchive(ArchiveType.FRAMES);

        int live_count = liveIdx.getHighestGroupId();
        int old_count = oldIdx.getHighestGroupId();

        logger.info("Porting skeleton index with new group ct " + live_count + " into old group ct " + old_count);

        int port_index = old_count;
        int successful_ports = 0;

        while(port_index < live_count) {
            Group group = liveIdx.findGroupByID(port_index);
            if(group == null) {
                port_index++;
                continue;
            }
            oldIdx.addGroup(group);
            successful_ports++;
            port_index++;
        }

        logger.info("Successfully ported " + successful_ports + " skeletons from latest OSRS cache");
    }

    private void portSkins() {
        Archive liveIdx = this.source.getArchive(ArchiveType.BASES);
        Archive oldIdx  = this.target.getArchive(ArchiveType.BASES);

        int live_count = liveIdx.getFreeGroupID();
        int old_count = oldIdx.getHighestGroupId();

        logger.info("Porting skins (bases) index with new group ct " + live_count + " into old group ct " + old_count);

        int port_index = old_count;
        int successful_ports = 0;

        while(port_index < live_count) {
            Group group = liveIdx.findGroupByID(port_index);
            if(group == null) {
                port_index++;
                continue;
            }
            oldIdx.addGroup(group);
            successful_ports++;
            port_index++;
        }

        logger.info("Successfully ported " + successful_ports + " skins from latest OSRS cache");
    }

    private void portItems() {
        Archive oldConfigs = target.getArchive(ArchiveType.CONFIGS);
        Group oldItems = oldConfigs.findGroupByID(GroupType.ITEM);

        Archive newConfigs = source.getArchive(ArchiveType.CONFIGS);
        Group newItems = newConfigs.findGroupByID(GroupType.ITEM);

        int live_count = newItems.getHighestFileId();
        int old_count = oldItems.getHighestFileId();

        logger.info("Porting items index with new group ct " + live_count + " into old group ct " + old_count);

        IntArrayList manualOverwrites = IntArrayList.of(ItemId.VIRTUS_MASK, ItemId.VIRTUS_ROBE_TOP, ItemId.VIRTUS_ROBE_LEGS, 26242, 26244, 26246);
        // We overwrite item definitions because some older items have new model ids now
        int port_index = old_count;
        int successful_ports = 0;
        for(int id: manualOverwrites) {
            File file = newItems.findFileByID(id);
            if(file == null) {
                continue;
            }
            oldItems.addFile(transcodeItem(file));
        }

        while(port_index < live_count) {
            File file = newItems.findFileByID(port_index);
            if(file == null) {
                port_index++;
                continue;
            }
            oldItems.addFile(transcodeItem(file));
            successful_ports++;
            port_index++;
        }

        logger.info("Successfully ported " + successful_ports + " items from latest OSRS cache");
    }

    /**
     * Rev-239 items use opcodes 44-54 (32-bit model ids) and new flags the rev-211 client
     * cannot read; decode with the new format and re-encode in the old format.
     */
    private File transcodeItem(File file) {
        if (!source239) {
            return file;
        }
        ItemDefinitions definitions = new ItemDefinitions(file.getID(), file.getData());
        return new File(file.getID(), definitions.encode());
    }

    /**
     * Ports world map data from the 225 source cache into the 211-based target.
     * The in-game world map uses its own archives (idx18/19/20) that are separate
     * from the playable region maps (idx5). Without this, the map UI shows the
     * rev-211 world regardless of what regions are actually playable.
     *
     * idx19 WORLDMAPDATA:
     *   - "details" group: one file per map area defining bounds, display origin,
     *     zoom, and section lists. Rev-225 inserts 4 bytes after bgColor that the
     *     rev-211 client doesn't expect; we strip them.
     *   - All other groups (compositemap, per-area render data): raw-copied.
     *
     * idx18 WORLDMAPGEOGRAPHY + idx20 WORLDMAPGROUND:
     *   - Per-square pre-baked render data. Same format between 211 and 225;
     *     all 225 groups are copied into the target (overwrites existing shared
     *     groups to keep render data consistent with the ported terrain).
     */
    private void portWorldMap() {
        if (!hasArchive(source, ArchiveType.WORLDMAPDATA)
                || !hasArchive(source, ArchiveType.WORLDMAPGEOGRAPHY)
                || !hasArchive(source, ArchiveType.WORLDMAPGROUND)) {
            logger.warn("Source cache missing worldmap archives — skipping portWorldMap");
            return;
        }

        portWorldMapData();
        portWorldMapSquares(ArchiveType.WORLDMAPGEOGRAPHY, "geography (idx18)");
        portWorldMapSquares(ArchiveType.WORLDMAPGROUND, "ground (idx20)");
        portMapLabels();
    }

    /** Port idx19 WORLDMAPDATA: transcode "details", raw-copy everything else. */
    private void portWorldMapData() {
        Archive srcWM = source.getArchive(ArchiveType.WORLDMAPDATA);
        Archive tgtWM = target.getArchive(ArchiveType.WORLDMAPDATA);

        if (srcWM == null || tgtWM == null) return;

        int detailsNameHash = tgtWM.findGroupIdByName("details");
        int ported = 0, transcoded = 0;

        for (Group srcGroup : srcWM.getGroups()) {
            try {
                // Load the group's data from the source cache
                srcWM.load(srcGroup);
            } catch (RuntimeException e) {
                logger.warn("Failed to load worldmap group id={}: {}", srcGroup.getID(), e.getMessage());
                continue;
            }

            if (srcGroup.getName() == detailsNameHash) {
                // --- Transcode "details" files: strip 4 inserted bytes ---
                File[] srcFiles = srcGroup.getFiles();
                File[] tgtFiles = new File[srcFiles.length];
                for (int i = 0; i < srcFiles.length; i++) {
                    if (srcFiles[i] == null) continue;
                    byte[] raw = srcFiles[i].getData().getBuffer();
                    byte[] out = transcodeWorldMapDetails(raw);
                    tgtFiles[i] = new File(srcFiles[i].getID(), srcFiles[i].getName(), new ByteBuffer(out));
                    transcoded++;
                }
                Group tgtGroup = new Group(srcGroup.getID(), srcGroup.getName(), 0, tgtFiles);
                tgtWM.addGroup(tgtGroup);
            } else {
                // --- Raw-copy all other groups ---
                tgtWM.addGroup(srcGroup);
                ported++;
            }
        }

        logger.info("portWorldMap: idx19 details transcoded {} files, raw-copied {} other groups", transcoded, ported);
    }

    /**
     * Transcode a rev-225 world map "details" area file into rev-211 format.
     * 225 inserts 4 bytes (observed: 0xFF 0x00 0x00 0x00) after the bgColor int32
     * and before the discarded-unknown byte. Stripping them produces the exact
     * byte sequence the rev-211 client's WorldMapArea.read() expects.
     *
     * Layout: string(internalName) + string(externalName) + int(origin) + int(bgColor)
     *         + [4 INSERTED BYTES] + u8(unknown) + u8(isMain) + u8(zoom) + u8(sectionCount) + sections...
     */
    private byte[] transcodeWorldMapDetails(byte[] b225) {
        // Find the position after the two null-terminated strings + 8 bytes (origin + bgColor)
        int pos = 0;
        // Skip first string
        while (pos < b225.length && b225[pos] != 0) pos++;
        pos++; // skip the null terminator
        // Skip second string
        while (pos < b225.length && b225[pos] != 0) pos++;
        pos++; // skip the null terminator
        // Skip origin (4 bytes) + bgColor (4 bytes)
        pos += 8;

        if (pos + 4 > b225.length) {
            // File too short for the insertion to exist — return as-is (shouldn't happen)
            return b225;
        }

        // Strip the 4 inserted bytes at 'pos'
        byte[] result = new byte[b225.length - 4];
        System.arraycopy(b225, 0, result, 0, pos);
        System.arraycopy(b225, pos + 4, result, pos, b225.length - pos - 4);
        return result;
    }

    /** Port idx18 or idx20: copy all source groups into the target. */
    private void portWorldMapSquares(ArchiveType type, String label) {
        Archive srcArchive = source.getArchive(type);
        Archive tgtArchive = target.getArchive(type);

        if (srcArchive == null || tgtArchive == null) return;

        int ported = 0;
        for (Group srcGroup : srcArchive.getGroups()) {
            try {
                srcArchive.load(srcGroup);
            } catch (RuntimeException e) {
                continue;
            }
            tgtArchive.addGroup(srcGroup);
            ported++;
        }

        logger.info("portWorldMap: {} ported {} groups", label, ported);
    }

    /**
     * Port MAP_LABELS (WorldMapElement definitions, config group 35) from the source cache.
     * These define world map icons, labels, and tooltips. Without them, new map areas
     * render without any icons or place names.
     */
    private void portMapLabels() {
        Archive oldConfigs = target.getArchive(ArchiveType.CONFIGS);
        Group oldLabels = oldConfigs.findGroupByID(GroupType.MAP_LABELS);

        Archive newConfigs = source.getArchive(ArchiveType.CONFIGS);
        Group newLabels = newConfigs.findGroupByID(GroupType.MAP_LABELS);

        if (oldLabels == null || newLabels == null) return;

        int old_count = oldLabels.getHighestFileId();
        int live_count = newLabels.getHighestFileId();

        int port_index = old_count;
        int successful_ports = 0;

        while (port_index < live_count) {
            File file = newLabels.findFileByID(port_index);
            if (file == null) {
                port_index++;
                continue;
            }
            oldLabels.addFile(file);
            successful_ports++;
            port_index++;
        }

        logger.info("portWorldMap: ported {} MAP_LABELS (ids {}-{})", successful_ports, old_count, live_count - 1);
    }

    public void preload() {

    }
}
