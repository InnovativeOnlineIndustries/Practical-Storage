package com.buuz135.functionalstorage.client.loader;

import com.buuz135.functionalstorage.block.FramedDrawerBlock;
import com.buuz135.functionalstorage.client.model.FramedDrawerModelData;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Vector3f;
import io.github.fabricators_of_create.porting_lib.model.CompositeModel;
import io.github.fabricators_of_create.porting_lib.model.IQuadTransformer;
import io.github.fabricators_of_create.porting_lib.model.SimpleModelState;
import io.github.fabricators_of_create.porting_lib.model.data.ModelData;
import io.github.fabricators_of_create.porting_lib.model.data.ModelProperty;
import io.github.fabricators_of_create.porting_lib.model.geometry.IGeometryBakingContext;
import io.github.fabricators_of_create.porting_lib.model.geometry.IGeometryLoader;
import io.github.fabricators_of_create.porting_lib.model.geometry.IUnbakedGeometry;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.buuz135.functionalstorage.client.loader.FramedModel.Baked.getQuadsUsingShape;

/**
 * A Custom Model for Framed Drawers. <br>
 * Based on {@link CompositeModel} from Forge. <br>
 * Using parts of <a href="https://github.com/SleepyTrousers/EnderIO-Rewrite/blob/dev/1.19.x/src/decor/java/com/enderio/decoration/client/model/painted/PaintedBlockModel.java"> Painted Block Model</a> from Ender IO.
 */
public class FramedModel implements IUnbakedGeometry<FramedModel> {
    private static final Logger LOGGER = LogManager.getLogger();

    private final ImmutableMap<String, BlockModel> children;
    private final ImmutableList<String> itemPasses;
    private final boolean logWarning;

    public FramedModel(ImmutableMap<String, BlockModel> children, ImmutableList<String> itemPasses)
    {
        this(children, itemPasses, false);
    }

    private FramedModel(ImmutableMap<String, BlockModel> children, ImmutableList<String> itemPasses, boolean logWarning)
    {
        this.children = children;
        this.itemPasses = itemPasses;
        this.logWarning = logWarning;
    }

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides, ResourceLocation modelLocation)
    {
        if (logWarning)
            LOGGER.warn("Model \"" + modelLocation + "\" is using the deprecated \"parts\" field in its composite model instead of \"children\". This field will be removed in 1.20.");

        Material particleLocation = context.getMaterial("particle");
        TextureAtlasSprite particle = spriteGetter.apply(particleLocation);

        var rootTransform = context.getRootTransform();
        if (!rootTransform.isIdentity())
            modelState = new SimpleModelState(modelState.getRotation().compose(rootTransform), modelState.isUvLocked());

        var bakedPartsBuilder = ImmutableMap.<String, BakedModel>builder();
        for (var entry : children.entrySet())
        {
            var name = entry.getKey();
            if (!context.isComponentVisible(name, true))
                continue;
            var model = entry.getValue();
            bakedPartsBuilder.put(name, model.bake(bakery, model, spriteGetter, modelState, modelLocation, true));
        }
        var bakedParts = bakedPartsBuilder.build();

        var itemPassesBuilder = ImmutableList.<BakedModel>builder();
        for (String name : this.itemPasses)
        {
            var model = bakedParts.get(name);
            if (model == null)
                throw new IllegalStateException("Specified \"" + name + "\" in \"item_render_order\", but that is not a child of this model.");
            itemPassesBuilder.add(model);
        }

        return new FramedModel.Baked(context.isGui3d(), context.useBlockLight(), context.useAmbientOcclusion(), particle, context.getTransforms(), overrides, bakedParts, itemPassesBuilder.build());
    }

    @Override
    public Collection<Material> getMaterials(IGeometryBakingContext context, Function<ResourceLocation, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors)
    {
        Set<Material> textures = new HashSet<>();
        if (context.hasMaterial("particle"))
            textures.add(context.getMaterial("particle"));
        for (BlockModel part : children.values())
            textures.addAll(part.getMaterials(modelGetter, missingTextureErrors));
        return textures;
    }

    @Override
    public Set<String> getConfigurableComponentNames()
    {
        return children.keySet();
    }

    public class Baked implements BakedModel, FabricBakedModel
    {
        private final boolean isAmbientOcclusion;
        private final boolean isGui3d;
        private final boolean isSideLit;
        private final TextureAtlasSprite particle;
        private final ItemOverrides overrides;
        private final ItemTransforms transforms;
        private final ImmutableMap<String, BakedModel> children;
        private final ImmutableList<BakedModel> itemPasses;

        public Baked(boolean isGui3d, boolean isSideLit, boolean isAmbientOcclusion, TextureAtlasSprite particle, ItemTransforms transforms, ItemOverrides overrides, ImmutableMap<String, BakedModel> children, ImmutableList<BakedModel> itemPasses)
        {
            this.children = children;
            this.isAmbientOcclusion = isAmbientOcclusion;
            this.isGui3d = isGui3d;
            this.isSideLit = isSideLit;
            this.particle = particle;
            this.overrides = overrides;
            this.transforms = transforms;
            this.itemPasses = itemPasses;
        }

//        @NotNull
//        @Override
//        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData data, @Nullable RenderType renderType)
//        {
//            List<List<BakedQuad>> quadLists = new ArrayList<>();
//            for (Map.Entry<String, BakedModel> entry : children.entrySet())
//            {
//                if (renderType == null || (state != null && entry.getValue().getRenderTypes(state, rand, data).contains(renderType)))
//                {
//                    FramedDrawerModelData framedDrawerModelData = data.get(FramedDrawerModelData.FRAMED_PROPERTY);
//                    List<BakedQuad> quads = entry.getValue().getQuads(state, side, rand, Data.resolve(data, entry.getKey()), renderType);
//                    if (framedDrawerModelData != null && framedDrawerModelData.getDesign().containsKey(entry.getKey())) {
//                        Item item = framedDrawerModelData.getDesign().get(entry.getKey());
//                        quadLists.add(getQuadsUsingShape(item, quads, side, rand, renderType));
//                    } else {
//                        quadLists.add(quads);
//                    }
//                }
//            }
//            return ConcatenatedListView.of(quadLists);
//        }


        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState blockState, @Nullable Direction direction, RandomSource randomSource) {
            return List.of();
        }

        @Override
        public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
            for (Map.Entry<String, BakedModel> entry : children.entrySet())
            {
                context.fallbackConsumer().accept(entry.getValue());
            }
        }

        @Override
        public void emitItemQuads(ItemStack itemStack, Supplier<RandomSource> randomSupplier, RenderContext context) {
            for (Map.Entry<String, BakedModel> entry : children.entrySet()) {
                ((FabricBakedModel)entry.getValue()).emitItemQuads(itemStack, randomSupplier, context);
                FramedDrawerModelData framedDrawerModelData = FramedDrawerBlock.getDrawerModelData(itemStack);
//                if (framedDrawerModelData != null && framedDrawerModelData.getDesign().containsKey(entry.getKey())) {
//                    Item item = framedDrawerModelData.getDesign().get(entry.getKey());
//                    QuadEmitter emitter = context.getEmitter();
//                    context.pushTransform(quad -> {
//                        quad.
//                    });
//                    quadLists.add(getQuadsUsingShape(item, quads, side, rand, renderType));
//                } else {
//                    quadLists.add(quads);
//                }
            }
        }

        protected static List<BakedQuad> getQuadsUsingShape(@Nullable Item frameItem, List<BakedQuad> shape, @Nullable Direction side, RandomSource rand, @Nullable RenderType renderType) {
            if (frameItem instanceof BlockItem blockItem) {
                BlockState state1 = blockItem.getBlock().defaultBlockState();
                BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state1);
                Optional<List<Triple<TextureAtlasSprite, Integer, int[]>>> spriteOptional = getSpriteData(model, state1, side, rand, null, renderType);
                List<BakedQuad> returnQuads = new ArrayList<>();
                for (BakedQuad shapeQuad : shape) {
                    List<Triple<TextureAtlasSprite, Integer, int[]>> spriteData = spriteOptional.orElse(getSpriteFromModel(shapeQuad, model, state1,null));
                    returnQuads.addAll(framedQuad(shapeQuad, spriteData, state1.getLightEmission()));
                }
                return returnQuads;
            }
            return List.of();
        }

        private static Optional<List<Triple<TextureAtlasSprite, Integer, int[]>>> getSpriteData(BakedModel model, BlockState state, @Nullable Direction side, RandomSource rand, @Nullable Direction rotation, @Nullable RenderType renderType) {
            List<BakedQuad> quads = model.getQuads(state, side, rand);
            List<Float> positions = new ArrayList<>();
            List<Triple<TextureAtlasSprite, Integer, int[]>> modelData = new ArrayList<>();
            if (!quads.isEmpty()) {
                for (BakedQuad bakedQuad: quads) {
                    float[] position = unpackVertices(bakedQuad.getVertices(), 0, IQuadTransformer.POSITION, 3);
                    positions.add(getPositionFromDirection(position, side));
                }
                List<Integer> index = getMinMaxPosition(positions, side);
                for (int i = 0; i < index.size(); i++) {
                    int[] lights = new int[4];
                    for (int j=0; j<4 ; j++) {
                        lights[j] = quads.get(i).getVertices()[IQuadTransformer.UV2 + j * IQuadTransformer.STRIDE];
                    }
                    int tint = quads.get(i).isTinted() ? Minecraft.getInstance().getBlockColors().getColor(state, Minecraft.getInstance().level, null, quads.get(i).getTintIndex()) : -1;
                    Triple<TextureAtlasSprite, Integer, int[]> triple = new ImmutableTriple<>(quads.get(i).getSprite(), tint, lights);
                    modelData.add(triple);
                }
            }
            return quads.isEmpty() ? Optional.empty() : Optional.of(modelData);
        }

        private static float getPositionFromDirection(float[] position, Direction side) {
            Vec3i normal = new Vec3i(0,0,0);
            if (side != null) {
                normal = side.getNormal();
            }
            Vector3f vector3f = new Vector3f(position[0] * normal.getX(), position[1] * normal.getY(), position[2] * normal.getZ()); // making a vector with only 1 element at the normal
            return (float) Math.sqrt(vector3f.dot(vector3f));
        }

        private static List<Integer> getMinMaxPosition(List<Float> positions, Direction side) {
            List<Integer> index = new ArrayList<>();
            float minMax = side != null && side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? Collections.max(positions) : Collections.min(positions);
            for (int i = 0; i < positions.size(); i++) {
                if (Math.abs(positions.get(i) - minMax) < 0.1) {
                    index.add(i);
                }
            }
            return index;
        }

        protected static List<Triple<TextureAtlasSprite, Integer, int[]>> getSpriteFromModel(BakedQuad shape, BakedModel model, BlockState state, Direction rotation) {
            List<BakedQuad> quads = model.getQuads(state, shape.getDirection(), RandomSource.create());
            List<Float> positions = new ArrayList<>();
            List<Triple<TextureAtlasSprite, Integer, int[]>> modelData = new ArrayList<>();
            if (!quads.isEmpty()) {
                for (BakedQuad bakedQuad: quads) {
                    float[] position = unpackVertices(bakedQuad.getVertices(), 0, IQuadTransformer.POSITION, 3);
                    positions.add(getPositionFromDirection(position, shape.getDirection()));

                }
                List<Integer> index = getMinMaxPosition(positions, shape.getDirection());
                for (int i = 0; i < index.size(); i++) {
                    int[] lights = new int[4];
                    for (int j=0; j<4; j++) {
                        lights[j] = quads.get(i).getVertices()[IQuadTransformer.UV2 + j * IQuadTransformer.STRIDE];
                    }
                    int tint = quads.get(i).isTinted() ? Minecraft.getInstance().getBlockColors().getColor(state, Minecraft.getInstance().level, null, quads.get(i).getTintIndex()) : -1;
                    Triple<TextureAtlasSprite, Integer, int[]> triple = new ImmutableTriple<>(quads.get(i).getSprite(), tint, lights);
                    modelData.add(triple);
                }
            }
            return quads.isEmpty() ? List.of(Triple.of(Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(MissingTextureAtlasSprite.getLocation()), -1, new int[] {0,0,0,0})) : modelData;
        }

        protected static List<BakedQuad> framedQuad(BakedQuad toCopy, List<Triple<TextureAtlasSprite, Integer, int[]>> modelData, int lightEmission) {
            lightEmission = LightTexture.pack(lightEmission, lightEmission);
            List<BakedQuad> quads = new ArrayList<>();
            for (int j = 0; j < modelData.size(); j++) {
                BakedQuad copied = new BakedQuad(Arrays.copyOf(toCopy.getVertices(), 32), -1, toCopy.getDirection(), modelData.get(j).getLeft(), toCopy.isShade());

                for (int i = 0; i < 4; i++) {
                    float[] uv0 = unpackVertices(copied.getVertices(), i, IQuadTransformer.UV0, 2);
                    uv0[0] = (uv0[0] - toCopy.getSprite().getU0()) * modelData.get(j).getLeft().getWidth() / toCopy.getSprite().getWidth() + modelData.get(j).getLeft().getU0();
                    uv0[1] = (uv0[1] - toCopy.getSprite().getV0()) * modelData.get(j).getLeft().getHeight() / toCopy.getSprite().getHeight() + modelData.get(j).getLeft().getV0();
                    int[] packedTextureData = packUV(uv0[0], uv0[1]);
                    copied.getVertices()[IQuadTransformer.UV0 + i * IQuadTransformer.STRIDE] = packedTextureData[0];
                    copied.getVertices()[IQuadTransformer.UV0 + 1 + i * IQuadTransformer.STRIDE] = packedTextureData[1];

                    if (modelData.get(j).getMiddle() != -1) {
                        int[] colors = getColorARGB(copied.getVertices(), i);
                        int[] color1 = getColorARGB(modelData.get(j).getMiddle());
                        colors[0] = (colors[0] * color1[0]) / 255;
                        colors[1] = (colors[1] * color1[1]) / 255;
                        colors[2] = (colors[2] * color1[2]) / 255;
                        colors[3] = (colors[3] * color1[3]) / 255;
                        int packedColor = packColor( colors[3], colors[2], colors[1], colors[0]);
                        copied.getVertices()[IQuadTransformer.COLOR + i * IQuadTransformer.STRIDE] = packedColor;
                    }

                    copied.getVertices()[IQuadTransformer.UV2 + i * IQuadTransformer.STRIDE] = Math.max(modelData.get(j).getRight()[i], lightEmission);

                }
                quads.add(copied);
            }
            return quads;
        }

        private static float[] unpackVertices(int[] vertices, int vertexIndex, int position, int count) {
            float[] floats = new float[count];
            int startIndex = vertexIndex * IQuadTransformer.STRIDE + position;
            for (int i = 0; i < count; i++) {
                floats[i] = Float.intBitsToFloat(vertices[startIndex + i]);
            }
            return floats;
        }

        private static int[] getColorARGB(int[] vertices, int vertexIndex) {
            int color = vertices[IQuadTransformer.STRIDE * vertexIndex + IQuadTransformer.COLOR];
            return getColorARGB(color);
        }

        private static int[] getColorARGB(int color) {
            int[] argb = new int[4];
            argb[0] = color >> 24 & 0xFF;
            argb[1] = color >> 16 & 0xFF;
            argb[2] = color >> 8 & 0xFF;
            argb[3] = color & 0xFF;
            return argb;
        }

        private static int[] getUV2(int[] vertices, int vertexIndex) {
            int[] light = new int[2];
            int uv2 = vertices[IQuadTransformer.STRIDE * vertexIndex + IQuadTransformer.UV2];
            light[0] = (uv2 & 0xFFFF) >> 4;
            light[1] = uv2 >> 20 & '\uffff';
            return light;
        }


        public static int[] packUV(float u, float v) {
            int[] quadData = new int[2];
            quadData[0] = Float.floatToRawIntBits(u);
            quadData[1] = Float.floatToRawIntBits(v);
            return quadData;
        }

        public static int packColor(int r, int g, int b, int a) {
            return ((a & 0xFF) << 24) |
                    ((r & 0xFF) << 16) |
                    ((g & 0xFF) << 8)  |
                    ((b & 0xFF));
        }

        public static int packUV2(int u, int v) {
            return u << 4 | v << 20;
        }

        @Override
        public boolean useAmbientOcclusion()
        {
            return isAmbientOcclusion;
        }

        @Override
        public boolean isGui3d()
        {
            return isGui3d;
        }

        @Override
        public boolean usesBlockLight()
        {
            return isSideLit;
        }

        @Override
        public boolean isCustomRenderer()
        {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleIcon()
        {
            return particle;
        }

//        @Override
        public TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
            FramedDrawerModelData framedDrawerModelData = data.get(FramedDrawerModelData.FRAMED_PROPERTY);
            if (framedDrawerModelData != null && framedDrawerModelData.getDesign().containsKey("particle")) {
                if (framedDrawerModelData.getDesign().get("particle") instanceof BlockItem blockItem) {
                    return Minecraft.getInstance().getBlockRenderer().getBlockModel(blockItem.getBlock().defaultBlockState()).getParticleIcon(/*data*/);
                }
            }
            return particle;
        }

        @Override
        public ItemOverrides getOverrides()
        {
            return overrides;
        }

        @Override
        public ItemTransforms getTransforms()
        {
            return transforms;
        }

//        @Override
//        public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data)
//        {
//            var sets = new ArrayList<ChunkRenderTypeSet>();
//            for (Map.Entry<String, BakedModel> entry : children.entrySet())
//                sets.add(entry.getValue().getRenderTypes(state, rand, FramedModel.Data.resolve(data, entry.getKey())));
//            return ChunkRenderTypeSet.union(sets);
//        }
//
//        @Override
//        public List<BakedModel> getRenderPasses(ItemStack itemStack, boolean fabulous)
//        {
//            return List.of(new ItemModel(this, itemStack));
//        }

        @Nullable
        public BakedModel getPart(String name)
        {
            return children.get(name);
        }

        @Override
        public boolean isVanillaAdapter() {
            return false;
        }
    }

    /**
     * A model data container which stores data for child components.
     */
    public static class Data
    {
        public static final ModelProperty<Data> PROPERTY = new ModelProperty<>();

        private final Map<String, ModelData> partData;

        private Data(Map<String, ModelData> partData)
        {
            this.partData = partData;
        }

        @Nullable
        public ModelData get(String name)
        {
            return partData.get(name);
        }

        /**
         * Helper to get the data from a {@link ModelData} instance.
         *
         * @param modelData The object to get data from
         * @param name      The name of the part to get data for
         * @return The data for the part, or the one passed in if not found
         */
        public static ModelData resolve(ModelData modelData, String name)
        {
            var compositeData = modelData.get(PROPERTY);
            if (compositeData == null)
                return modelData;
            var partData = compositeData.get(name);
            return partData != null ? partData : modelData;
        }

        public static FramedModel.Data.Builder builder()
        {
            return new FramedModel.Data.Builder();
        }

        public static final class Builder
        {
            private final Map<String, ModelData> partData = new IdentityHashMap<>();

            public FramedModel.Data.Builder with(String name, ModelData data)
            {
                partData.put(name, data);
                return this;
            }

            public FramedModel.Data build()
            {
                return new FramedModel.Data(partData);
            }
        }
    }

    public static final class Loader implements IGeometryLoader<FramedModel>
    {
        public static final FramedModel.Loader INSTANCE = new FramedModel.Loader();

        private Loader()
        {
        }

        @Override
        public FramedModel read(JsonObject jsonObject, JsonDeserializationContext deserializationContext)
        {
            List<String> itemPasses = new ArrayList<>();
            ImmutableMap.Builder<String, BlockModel> childrenBuilder = ImmutableMap.builder();
            readChildren(jsonObject, "children", deserializationContext, childrenBuilder, itemPasses, false);
            boolean logWarning = readChildren(jsonObject, "parts", deserializationContext, childrenBuilder, itemPasses, true);

            var children = childrenBuilder.build();
            if (children.isEmpty())
                throw new JsonParseException("Composite model requires a \"children\" element with at least one element.");

            if (jsonObject.has("item_render_order"))
            {
                itemPasses.clear();
                for (var element : jsonObject.getAsJsonArray("item_render_order"))
                {
                    var name = element.getAsString();
                    if (!children.containsKey(name))
                        throw new JsonParseException("Specified \"" + name + "\" in \"item_render_order\", but that is not a child of this model.");
                    itemPasses.add(name);
                }
            }

            return new FramedModel(children, ImmutableList.copyOf(itemPasses), logWarning);
        }

        private boolean readChildren(JsonObject jsonObject, String name, JsonDeserializationContext deserializationContext, ImmutableMap.Builder<String, BlockModel> children, List<String> itemPasses, boolean logWarning)
        {
            if (!jsonObject.has(name))
                return false;
            var childrenJsonObject = jsonObject.getAsJsonObject(name);
            for (Map.Entry<String, JsonElement> entry : childrenJsonObject.entrySet())
            {
                children.put(entry.getKey(), deserializationContext.deserialize(entry.getValue(), BlockModel.class));
                itemPasses.add(entry.getKey()); // We can do this because GSON preserves ordering during deserialization
            }
            return logWarning;
        }
    }

    private class ItemModel implements BakedModel {

        private final Baked baked;
        private final ItemStack itemStack;

        public ItemModel(Baked baked, ItemStack itemStack) {
            this.baked = baked;
            this.itemStack = itemStack;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
//            List<List<BakedQuad>> quadLists = new ArrayList<>();
//            for (Map.Entry<String, BakedModel> entry : baked.children.entrySet())
//            {
//                if (renderType == null || (state != null && entry.getValue().getRenderTypes(state, rand, extraData).contains(renderType)))
//                {
//
//                    List<BakedQuad> quads = entry.getValue().getQuads(state, side, rand, Data.resolve(extraData, entry.getKey()), renderType);
//                    FramedDrawerModelData framedDrawerModelData = FramedDrawerBlock.getDrawerModelData(itemStack);
//                    if (framedDrawerModelData != null && framedDrawerModelData.getDesign().containsKey(entry.getKey())) {
//                        Item item = framedDrawerModelData.getDesign().get(entry.getKey());
//                        quadLists.add(getQuadsUsingShape(item, quads, side, rand, renderType));
//                    } else {
//                        quadLists.add(quads);
//                    }
//                }
//            }
//            return ConcatenatedListView.of(quadLists);
            return List.of();
        }

        @Override
        public boolean useAmbientOcclusion() {
            return false;
        }

        @Override
        public boolean isGui3d() {
            return true;
        }

        @Override
        public boolean usesBlockLight() {
            return true;
        }

        @Override
        public boolean isCustomRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return baked.getParticleIcon();
        }

        @Override
        public ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }

        @Override
        public ItemTransforms getTransforms() {
            return baked.getTransforms();
        }
    }
}
