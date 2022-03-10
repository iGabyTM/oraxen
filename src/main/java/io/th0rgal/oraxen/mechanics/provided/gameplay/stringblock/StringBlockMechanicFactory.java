package io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock;

import com.google.gson.JsonObject;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Attachable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Tripwire;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StringBlockMechanicFactory extends MechanicFactory {

    private static final int WEST = 1;
    private static final int SOUTH = 2;
    private static final int NORTH = 3;
    private static final int ATTACHED = 4;
    private static final int DISARMED = 5;
    private static final int POWERED = 6;

    private static final List<BlockFace> BLOCK_FACES = Arrays.asList(BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH, BlockFace.NORTH);

    public static final Map<Integer, StringBlockMechanic> BLOCK_PER_VARIATION = new HashMap<>();
    private static JsonObject variants;
    private static StringBlockMechanicFactory instance;
    public final List<String> toolTypes;

    public StringBlockMechanicFactory(ConfigurationSection section) {
        super(section);
        instance = this;
        variants = new JsonObject();
        toolTypes = section.getStringList("tool_types");
        // this modifier should be executed when all the items have been parsed, just
        // before zipping the pack
        OraxenPlugin.get().getResourcePack().addModifiers(getMechanicID(),
                packFolder -> {
                    OraxenPlugin.get().getResourcePack()
                            .writeStringToVirtual("assets/minecraft/blockstates",
                                    "tripwire.json", getBlockStateContent());
                });
        MechanicsManager.registerListeners(OraxenPlugin.get(), new StringBlockMechanicListener(this));
    }

    /**
     * Get the state of a property of {@link Tripwire}
     * @param id id if texture
     * @param index property index
     * @return whether the property is true of false
     * @see BlockFace#EAST
     * @see BlockFace#WEST
     * @see BlockFace#SOUTH
     * @see BlockFace#NORTH
     * @see Attachable#isAttached()
     * @see Tripwire#isDisarmed()
     * @see Tripwire#isPowered()
     */
    private static boolean getState(int id, int index) {
        return ((id >> index) & 1) == 1;
    }

    public static JsonObject getModelJson(String modelName) {
        JsonObject content = new JsonObject();
        content.addProperty("model", modelName);
        return content;
    }

    public static String getBlockStateVariantName(int id) {
        return "east=%b,west=%b,south=%b,north=%b,attached=%b,disarmed=%b,powered=%b".formatted(
                ((id & 1) == 1),
                getState(id, WEST),
                getState(id, NORTH),
                getState(id, SOUTH),
                getState(id, ATTACHED),
                getState(id, DISARMED),
                getState(id, POWERED)
        );
    }

    public static StringBlockMechanic getBlockMechanic(int customVariation) {
        return BLOCK_PER_VARIATION.get(customVariation);
    }

    public static StringBlockMechanicFactory getInstance() {
        return instance;
    }


    /**
     * Attempts to set the block directly to the model and texture of an Oraxen item.
     *
     * @param block  The block to update.
     * @param itemId The Oraxen item ID.
     */
    public static void setBlockModel(Block block, String itemId) {
        final MechanicFactory mechanicFactory = MechanicsManager.getMechanicFactory("noteblock");
        StringBlockMechanic noteBlockMechanic = (StringBlockMechanic) mechanicFactory.getMechanic(itemId);
        block.setBlockData(createTripwireData(noteBlockMechanic.getCustomVariation()), false);
    }

    private String getBlockStateContent() {
        JsonObject tripwire = new JsonObject();
        tripwire.add("variants", variants);
        return tripwire.toString();
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        StringBlockMechanic mechanic = new StringBlockMechanic(this, itemMechanicConfiguration);
        variants.add(getBlockStateVariantName(mechanic.getCustomVariation()),
                getModelJson(mechanic.getModel(itemMechanicConfiguration.getParent()
                        .getParent())));
        BLOCK_PER_VARIATION.put(mechanic.getCustomVariation(), mechanic);
        addToImplemented(mechanic);
        return mechanic;
    }

    public static int getCode(final Tripwire blockData) {
        int sum = 0;
        for (final BlockFace blockFace : blockData.getFaces())
            sum += (int) Math.pow(2, BLOCK_FACES.indexOf(blockFace));
        if (blockData.isAttached())
            sum += (int) Math.pow(2, ATTACHED);
        if (blockData.isDisarmed())
            sum += (int) Math.pow(2, DISARMED);
        if (blockData.isPowered())
            sum += (int) Math.pow(2, POWERED);
        return sum;
    }

    /**
     * Generate a Tripwire blockdata from its id
     *
     * @param code The block id.
     */
    public static BlockData createTripwireData(final int code) {
        Tripwire data = ((Tripwire) Bukkit.createBlockData(Material.TRIPWIRE));
        int i = 0;
        for (final BlockFace face : BLOCK_FACES)
            data.setFace(face, (code & 0x1 << i++) != 0);
        data.setAttached((code & 0x1 << i++) != 0);
        data.setDisarmed((code & 0x1 << i++) != 0);
        data.setPowered((code & 0x1 << i) != 0);
        return data;
    }

    /**
     * Generate a NoteBlock blockdata from an oraxen id
     *
     * @param itemID The id of an item implementing NoteBlockMechanic
     */
    public BlockData createTripwireData(String itemID) {
        /* We have 16 instruments with 25 notes. All of those blocks can be powered.
         * That's: 16*25*2 = 800 variations. The first 25 variations of PIANO (not powered)
         * will be reserved for the vanilla behavior. We still have 800-25 = 775 variations
         */
        return createTripwireData(((StringBlockMechanic) getInstance().getMechanic(itemID)).getCustomVariation());
    }

}
