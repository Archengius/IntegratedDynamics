package org.cyclops.integrateddynamics.api.block;

import com.mojang.serialization.MapCodec;

/**
 * Represents a type of block settings used to encode and decode them
 *
 * @param codec codec that can serialize and deserialize a particular block settings type
 */
public record BlockSettingsType(MapCodec<? extends IBlockSettings> codec) {
}
