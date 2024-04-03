/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2024 retrooper and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.retrooper.packetevents.util.adventure;

import com.github.retrooper.packetevents.protocol.nbt.*;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.*;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class AdventureNBTSerializer implements ComponentSerializer<Component, Component, NBT> {

    private final boolean downsampleColor;

    public AdventureNBTSerializer(boolean downsampleColor) {
        this.downsampleColor = downsampleColor;
    }

    @Override
    public @NotNull Component deserialize(@NotNull NBT input) {
        if (input.getType() == NBTType.STRING) { // Serialized as string
            return Component.text(((NBTString) input).getValue());
        }

        if (input.getType() == NBTType.BYTE && ((NBTByte) input).getAsByte() < 2) { // Serialized as boolean
            return Component.text(((NBTByte) input).getAsByte() == 1);
        }

        if (input instanceof NBTNumber) { // Serialized as number
            return Component.text(((NBTNumber) input).getAsInt());
        }

        // Serialized as tree
        NBTCompound compound = requireType(input, NBTType.COMPOUND);
        NBTReader reader = new NBTReader(compound);

        Function<NBT, String> textFunction = nbt -> {
            if (nbt.getType() == NBTType.STRING) {
                return ((NBTString) nbt).getValue();
            } else if (nbt.getType() == NBTType.BYTE && ((NBTByte) nbt).getAsByte() < 2) {
                return String.valueOf(((NBTByte) nbt).getAsByte() == 1);
            } else if (nbt instanceof NBTNumber) {
                return String.valueOf(((NBTNumber) nbt).getAsInt());
            } else {
                throw new IllegalStateException("Don't know how to deserialize " + nbt.getType() + " to text");
            }
        };
        String text = reader.read("text", textFunction);
        if (text == null) text = reader.read("", textFunction);

        String translate = reader.readUTF("translate", Function.identity());
        String translateFallback = reader.readUTF("fallback", Function.identity());
        List<Component> translateWith = reader.readList("with", this::deserializeComponentList);
        NBTReader score = reader.child("score");
        String selector = reader.readUTF("selector", Function.identity());
        String keybind = reader.readUTF("keybind", Function.identity());
        String nbt = reader.readUTF("nbt", Function.identity());
        Boolean nbtInterpret = reader.readBoolean("interpret", Function.identity());
        BlockNBTComponent.Pos nbtBlock = reader.readUTF("block", BlockNBTComponent.Pos::fromString);
        String nbtEntity = reader.readUTF("entity", Function.identity());
        Key nbtStorage = reader.readUTF("storage", Key::key);
        List<Component> extra = reader.readList("extra", this::deserializeComponentList);
        Component separator = reader.read("separator", this::deserialize);
        Style style = reader.readCompound("style", this::deserializeStyle);

        // build component from read values
        ComponentBuilder<?, ?> builder;
        if (text != null) {
            builder = Component.text().content(text);
        } else if (translate != null) {
            if (translateWith != null) {
                builder = Component.translatable().key(translate).fallback(translateFallback).arguments(translateWith);
            } else {
                builder = Component.translatable().key(translate).fallback(translateFallback);
            }
        } else if (score != null) {
            builder = Component.score()
                    .name(score.readUTF("name", Function.identity()))
                    .objective(score.readUTF("objective", Function.identity()));
        } else if (selector != null) {
            builder = Component.selector().pattern(selector).separator(separator);
        } else if (keybind != null) {
            builder = Component.keybind().keybind(keybind);
        } else if (nbt != null) {
            if (nbtBlock != null) {
                builder = Component.blockNBT()
                        .nbtPath(nbt).interpret(nbtInterpret).separator(separator)
                        .pos(nbtBlock);
            } else if (nbtEntity != null) {
                builder = Component.entityNBT()
                        .nbtPath(nbt).interpret(nbtInterpret).separator(separator)
                        .selector(nbtEntity);
            } else if (nbtStorage != null) {
                builder = Component.storageNBT()
                        .nbtPath(nbt).interpret(nbtInterpret).separator(separator)
                        .storage(nbtStorage);
            } else {
                throw new IllegalStateException("Illegal nbt component, block/entity/storage is missing");
            }
        } else {
            throw new IllegalStateException("Illegal nbt component, component type could not be determined");
        }

        if (style != null) {
            builder.style(style);
        }

        if (extra != null) {
            builder.append(extra);
        }
        return builder.build();
    }

    @Override
    public @NotNull NBT serialize(@NotNull Component component) {
        if (component instanceof TextComponent && !component.hasStyling() && component.children().isEmpty()) {
            return new NBTString(((TextComponent) component).content());
        }

        return serializeComponent(component);
    }

    private @NotNull NBTCompound serializeComponent(Component component) {
        NBTWriter writer = new NBTWriter(new NBTCompound());

        // component parts
        if (component instanceof TextComponent) {
            // text content
            writer.writeUTF("text", ((TextComponent) component).content());
        } else if (component instanceof TranslatableComponent) {
            // translation key
            writer.writeUTF("translate", ((TranslatableComponent) component).key());

            // translation fallback
            String fallback = ((TranslatableComponent) component).fallback();
            if (fallback != null) {
                writer.writeUTF("fallback", fallback);
            }

            // translation arguments
            List<Component> args = ((TranslatableComponent) component).args();
            if (!args.isEmpty()) {
                writer.writeList("with", NBTType.COMPOUND, serializeComponentList(args));
            }
        } else if (component instanceof ScoreComponent) {
            // nested compound
            NBTWriter score = writer.child("score");

            // score name
            String scoreName = ((ScoreComponent) component).name();
            score.writeUTF("name", scoreName);

            // score objective
            String scoreObjective = ((ScoreComponent) component).objective();
            score.writeUTF("objective", scoreObjective);
        } else if (component instanceof SelectorComponent) {
            // selector
            writer.writeUTF("selector", ((SelectorComponent) component).pattern());

            // separator
            Component separator = ((SelectorComponent) component).separator();
            if (separator != null) writer.write("separator", this.serialize(separator));
        } else if (component instanceof KeybindComponent) {
            // keybind
            writer.writeUTF("keybind", ((KeybindComponent) component).keybind());
        } else if (component instanceof NBTComponent<?, ?>) {
            // nbt path
            String nbtPath = ((NBTComponent<?, ?>) component).nbtPath();
            writer.writeUTF("nbt", nbtPath);

            // interpret
            boolean interpret = ((NBTComponent<?, ?>) component).interpret();
            if (interpret) {
                writer.writeBoolean("interpret", true);
            }

            // separator
            Component separator = ((NBTComponent<?, ?>) component).separator();
            if (separator != null) writer.write("separator", this.serialize(separator));

            if (component instanceof BlockNBTComponent) {
                // nbt block
                BlockNBTComponent.Pos pos = ((BlockNBTComponent) component).pos();
                writer.writeUTF("block", pos.asString());
            } else if (component instanceof EntityNBTComponent) {
                // nbt entity
                String selector = ((EntityNBTComponent) component).selector();
                writer.writeUTF("entity", selector);
            } else if (component instanceof StorageNBTComponent) {
                // nbt storage key
                Key storage = ((StorageNBTComponent) component).storage();
                writer.writeUTF("storage", storage.asString());
            }
        }

        if (component.hasStyling()) {
            writer.writeCompound("style", serializeStyle(component.style()));
        }

        // component children
        List<Component> children = component.children();
        if (!children.isEmpty()) {
            writer.writeList("extra", NBTType.COMPOUND, serializeComponentList(children));
        }

        return writer.compound;
    }

    // -------------------- Style --------------------
    @SuppressWarnings({"PatternValidation", "rawtypes"})
    public @NotNull Style deserializeStyle(NBTCompound input) {
        if (input.isEmpty()) return Style.empty();

        Style.Builder style = Style.style();
        NBTReader reader = new NBTReader(input);

        reader.useUTF("font", value -> style.font(Key.key(value)));
        reader.useUTF("color", value -> {
            TextColor color = deserializeColor(value);
            if (color != null) style.color(color);
        });

        for (Map.Entry<TextDecoration, String> decoration : TextDecoration.NAMES.valueToKey().entrySet()) {
            reader.useBoolean(decoration.getValue(), value -> style.decoration(decoration.getKey(), TextDecoration.State.byBoolean(value)));
        }
        reader.useUTF("insertion", style::insertion);

        NBTReader clickEvent = reader.child("clickEvent");
        if (clickEvent != null) {
            style.clickEvent(ClickEvent.clickEvent(
                    clickEvent.readUTF("action", ClickEvent.Action.NAMES::value),
                    clickEvent.readUTF("value", Function.identity())
            ));
        }

        NBTReader hoverEvent = reader.child("hoverEvent");
        if (hoverEvent != null) {
            HoverEvent.Action action = hoverEvent.readUTF("action", HoverEvent.Action.NAMES::value);
            if (action.equals(HoverEvent.Action.SHOW_TEXT)) {
                style.hoverEvent(HoverEvent.showText(hoverEvent.read("contents", this::deserialize)));
            } else if (action.equals(HoverEvent.Action.SHOW_ITEM)) {
                if (hoverEvent.type("contents") == NBTType.STRING) {
                    style.hoverEvent(HoverEvent.showItem(hoverEvent.readUTF("contents", Key::key), 1));
                } else {
                    NBTReader child = hoverEvent.child("contents");
                    style.hoverEvent(HoverEvent.showItem(
                            child.readUTF("id", Key::key),
                            child.readInt("count", Function.identity()),
                            child.readUTF("tag", BinaryTagHolder::binaryTagHolder)
                    ));
                }
            } else if (action.equals(HoverEvent.Action.SHOW_ENTITY)) {
                NBTReader child = hoverEvent.child("contents");
                style.hoverEvent(HoverEvent.showEntity(
                        child.readUTF("type", Key::key),
                        child.readIntArray("id", this::deserializeUUID),
                        child.read("name", this::deserialize)
                ));
            }
        }

        return style.build();
    }

    public @NotNull NBTCompound serializeStyle(Style style) {
        if (style.isEmpty()) return new NBTCompound();

        NBTWriter writer = new NBTWriter(new NBTCompound());

        Key font = style.font();
        if (font != null) writer.writeUTF("font", font.asString());

        TextColor color = style.color();
        if (color != null) writer.writeUTF("color", serializeColor(color));

        for (Map.Entry<TextDecoration, String> decoration : TextDecoration.NAMES.valueToKey().entrySet()) {
            TextDecoration.State state = style.decoration(decoration.getKey());
            if (state != TextDecoration.State.NOT_SET) {
                writer.writeBoolean(decoration.getValue(), state == TextDecoration.State.TRUE);
            }
        }

        String insertion = style.insertion();
        if (insertion != null) writer.writeUTF("insertion", insertion);

        ClickEvent clickEvent = style.clickEvent();
        if (clickEvent != null) {
            NBTWriter child = writer.child("clickEvent");
            child.writeUTF("action", clickEvent.action().toString());
            child.writeUTF("value", clickEvent.value());
        }

        HoverEvent<?> hoverEvent = style.hoverEvent();
        if (hoverEvent != null) {
            NBTWriter child = writer.child("hoverEvent");
            child.writeUTF("action", hoverEvent.action().toString());
            switch (hoverEvent.action().toString()) {
                case "show_text": {
                    child.write("contents", this.serialize((Component) hoverEvent.value()));
                    break;
                }
                case "show_item": {
                    HoverEvent.ShowItem item = (HoverEvent.ShowItem) hoverEvent.value();
                    Key itemId = item.item();
                    int count = item.count();
                    BinaryTagHolder nbt = item.nbt();

                    if (count == 1 && nbt == null) {
                        child.writeUTF("contents", itemId.asString());
                    } else {
                        NBTWriter itemNBT = child.child("contents");
                        itemNBT.writeUTF("id", itemId.asString());
                        itemNBT.writeInt("count", count);
                        if (nbt != null) itemNBT.writeUTF("tag", nbt.string());
                    }
                    break;
                }
                case "show_entity": {
                    HoverEvent.ShowEntity showEntity = (HoverEvent.ShowEntity) hoverEvent.value();
                    NBTWriter entity = child.child("contents");
                    entity.writeUTF("type", showEntity.type().asString());
                    entity.writeIntArray("id", this.serializeUUID(showEntity.id()));
                    if (showEntity.name() != null) entity.write("name", this.serialize(showEntity.name()));
                    break;
                }

            }
        }

        return writer.compound;
    }
    // -------------------------------------------------

    // ------------------- TextColor -------------------
    private @Nullable TextColor deserializeColor(final @NotNull String value) {
        final TextColor color;
        if (value.startsWith(TextColor.HEX_PREFIX)) {
            color = TextColor.fromHexString(value);
        } else {
            color = NamedTextColor.NAMES.value(value);
        }

        if (color == null) return null;

        return this.downsampleColor ? NamedTextColor.nearestTo(color) : color;
    }

    @SuppressWarnings("ConstantConditions")
    private @NotNull String serializeColor(final @NotNull TextColor value) {
        if (value instanceof NamedTextColor) {
            return NamedTextColor.NAMES.key((NamedTextColor) value);
        } else if (this.downsampleColor) {
            return NamedTextColor.NAMES.key(NamedTextColor.nearestTo(value));
        } else {
            return String.format(Locale.ROOT, "%c%06X", TextColor.HEX_CHARACTER, value.value());
        }
    }
    // -------------------------------------------------

    // ---------------------- UUID ----------------------
    private @NotNull UUID deserializeUUID(int[] value) {
        if (value.length != 4) {
            throw new IllegalStateException("Invalid encoded uuid length: " + value.length + " != 4");
        }
        return new UUID(
                (long) value[0] << 32 | (long) value[1] & 0xFFFFFFFFL,
                (long) value[2] << 32 | (long) value[3] & 0xFFFFFFFFL
        );
    }

    private int @NotNull [] serializeUUID(UUID value) {
        return new int[]{
                (int) (value.getMostSignificantBits() >> 32),
                (int) value.getMostSignificantBits(),
                (int) (value.getLeastSignificantBits() >> 32),
                (int) value.getLeastSignificantBits()
        };
    }
    // -------------------------------------------------

    // ---------------- Component List -----------------
    private @NotNull List<Component> deserializeComponentList(List<?> value) {
        if (value.isEmpty()) return Collections.emptyList();

        List<Component> components = new ArrayList<>(value.size());
        for (Object nbt : value) {
            components.add(deserialize((NBT) nbt));
        }

        return components;
    }

    private List<NBTCompound> serializeComponentList(List<Component> value) {
        List<NBTCompound> components = new ArrayList<>(value.size());
        for (Component component : value) {
            components.add(serializeComponent(component));
        }
        return components;
    }
    // -------------------------------------------------

    static class NBTReader {
        private final NBTCompound compound;

        public NBTReader(NBTCompound compound) {
            this.compound = compound;
        }

        public void useBoolean(String key, Consumer<Boolean> consumer) {
            useTag(key, tag -> consumer.accept(requireType(tag, NBTType.BYTE).getAsByte() != 0));
        }

        public <R> R readBoolean(String key, Function<Boolean, R> function) {
            return withTag(key, tag -> function.apply(requireType(tag, NBTType.BYTE).getAsByte() != 0));
        }

        public void useByte(String key, Consumer<Byte> consumer) {
            useTag(key, tag -> consumer.accept(requireType(tag, NBTType.BYTE).getAsByte()));
        }

        public <R> R readByte(String key, Function<Byte, R> function) {
            return withTag(key, tag -> function.apply(requireType(tag, NBTType.BYTE).getAsByte()));
        }

        public void useShort(String key, Consumer<Short> consumer) {
            useTag(key, tag -> consumer.accept(requireType(tag, NBTType.SHORT).getAsShort()));
        }

        public <R> R readShort(String key, Function<Short, R> function) {
            return withTag(key, tag -> function.apply(requireType(tag, NBTType.SHORT).getAsShort()));
        }

        public void useInt(String key, Consumer<Integer> consumer) {
            useTag(key, tag -> consumer.accept(requireType(tag, NBTType.INT).getAsInt()));
        }

        public <R> R readInt(String key, Function<Integer, R> function) {
            return withTag(key, tag -> function.apply(requireType(tag, NBTType.INT).getAsInt()));
        }

        public void useLong(String key, Consumer<Long> consumer) {
            useTag(key, tag -> consumer.accept(requireType(tag, NBTType.LONG).getAsLong()));
        }

        public <R> R readLong(String key, Function<Long, R> function) {
            return withTag(key, tag -> function.apply(requireType(tag, NBTType.LONG).getAsLong()));
        }

        public void useFloat(String key, Consumer<Float> consumer) {
            useTag(key, tag -> consumer.accept(requireType(tag, NBTType.FLOAT).getAsFloat()));
        }

        public <R> R readFloat(String key, Function<Float, R> function) {
            return withTag(key, tag -> function.apply(requireType(tag, NBTType.FLOAT).getAsFloat()));
        }

        public void useDouble(String key, Consumer<Double> consumer) {
            useTag(key, tag -> consumer.accept(requireType(tag, NBTType.DOUBLE).getAsDouble()));
        }

        public <R> R readDouble(String key, Function<Double, R> function) {
            return withTag(key, tag -> function.apply(requireType(tag, NBTType.DOUBLE).getAsDouble()));
        }

        public void useUTF(String key, Consumer<String> consumer) {
            useTag(key, tag -> consumer.accept(requireType(tag, NBTType.STRING).getValue()));
        }

        public <R> R readUTF(String key, Function<String, R> function) {
            return withTag(key, tag -> function.apply(requireType(tag, NBTType.STRING).getValue()));
        }

        public void useByteArray(String key, Consumer<byte[]> consumer) {
            useTag(key, tag -> consumer.accept(requireType(tag, NBTType.BYTE_ARRAY).getValue()));
        }

        public <R> R readByteArray(String key, Function<byte[], R> function) {
            return withTag(key, tag -> function.apply(requireType(tag, NBTType.BYTE_ARRAY).getValue()));
        }

        public void useIntArray(String key, Consumer<int[]> consumer) {
            useTag(key, tag -> consumer.accept(requireType(tag, NBTType.INT_ARRAY).getValue()));
        }

        public <R> R readIntArray(String key, Function<int[], R> function) {
            return withTag(key, tag -> function.apply(requireType(tag, NBTType.INT_ARRAY).getValue()));
        }

        public void useLongArray(String key, Consumer<long[]> consumer) {
            useTag(key, tag -> consumer.accept(requireType(tag, NBTType.LONG_ARRAY).getValue()));
        }

        public <R> R readLongArray(String key, Function<long[], R> function) {
            return withTag(key, tag -> function.apply(requireType(tag, NBTType.LONG_ARRAY).getValue()));
        }

        public void useCompound(String key, Consumer<NBTCompound> consumer) {
            useTag(key, tag -> consumer.accept(requireType(tag, NBTType.COMPOUND)));
        }

        public <R> R readCompound(String key, Function<NBTCompound, R> function) {
            return withTag(key, tag -> function.apply(requireType(tag, NBTType.COMPOUND)));
        }

        public void useList(String key, Consumer<List<?>> consumer) {
            useTag(key, tag -> consumer.accept(requireType(tag, NBTType.LIST).getTags()));
        }

        public <R> R readList(String key, Function<List<?>, R> function) {
            return withTag(key, tag -> function.apply(requireType(tag, NBTType.LIST).getTags()));
        }

        public void use(String key, Consumer<NBT> consumer) {
            useTag(key, consumer);
        }

        public <R> R read(String key, Function<NBT, R> function) {
            return withTag(key, function);
        }

        public NBTReader child(String key) {
            return withTag(key, tag -> new NBTReader(requireType(tag, NBTType.COMPOUND)));
        }

        public NBTType<?> type(String key) {
            return withTag(key, NBT::getType);
        }

        private void useTag(String key, Consumer<NBT> consumer) {
            NBT tag = compound.getTagOrNull(key);
            if (tag != null) {
                consumer.accept(tag);
            }
        }

        private <R> R withTag(String key, Function<NBT, R> function) {
            NBT tag = compound.getTagOrNull(key);
            return tag == null ? null : function.apply(tag);
        }
    }

    static class NBTWriter {
        private final NBTCompound compound;

        public NBTWriter(NBTCompound compound) {
            this.compound = compound;
        }

        public void writeBoolean(String key, boolean value) {
            compound.setTag(key, new NBTByte(value ? (byte) 1 : (byte) 0));
        }

        public void writeByte(String key, byte value) {
            compound.setTag(key, new NBTByte(value));
        }

        public void writeShort(String key, short value) {
            compound.setTag(key, new NBTShort(value));
        }

        public void writeInt(String key, int value) {
            compound.setTag(key, new NBTInt(value));
        }

        public void writeLong(String key, long value) {
            compound.setTag(key, new NBTLong(value));
        }

        public void writeFloat(String key, float value) {
            compound.setTag(key, new NBTFloat(value));
        }

        public void writeDouble(String key, double value) {
            compound.setTag(key, new NBTDouble(value));
        }

        public void writeUTF(String key, String value) {
            compound.setTag(key, new NBTString(value));
        }

        public void writeByteArray(String key, byte[] value) {
            compound.setTag(key, new NBTByteArray(value));
        }

        public void writeIntArray(String key, int[] value) {
            compound.setTag(key, new NBTIntArray(value));
        }

        public void writeLongArray(String key, long[] value) {
            compound.setTag(key, new NBTLongArray(value));
        }

        public <T extends NBT> void writeList(String key, NBTType<T> innerType, List<T> value) {
            compound.setTag(key, new NBTList<>(innerType, value));
        }

        public void writeCompound(String key, NBTCompound value) {
            compound.setTag(key, value);
        }

        public void write(String key, NBT value) {
            compound.setTag(key, value);
        }

        public NBTWriter child(String key) {
            NBTCompound child = new NBTCompound();
            compound.setTag(key, child);
            return new NBTWriter(child);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends NBT> T requireType(NBT nbt, NBTType<T> required) {
        if (nbt.getType() != required) {
            throw new IllegalArgumentException("Expected " + required + " but got " + nbt.getType());
        }
        return (T) nbt;
    }
}
