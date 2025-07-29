package me.unariginal.dexrewards.config;

import me.unariginal.dexrewards.datatypes.DexType;

import java.util.ArrayList;
import java.util.List;

public class DexTypesConfig {
    public List<DexType> dexTypes = new ArrayList<>();

    public DexType getDexType(String name) {
        for (DexType dexType : dexTypes) {
            if (dexType.name.equals(name)) {
                return dexType;
            }
        }
        return null;
    }
}
